package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.entity.PlayerEntity
import com.example.data.model.UsernameStatus
import com.example.config.FirebaseBootstrap
import com.example.util.UsernameNormalizer
import com.example.util.UsernameSuggestionGenerator
import com.example.util.UsernameValidator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

sealed class UsernameReservationResult {
    object Success : UsernameReservationResult()
    data class Taken(val availableSuggestions: List<String>) : UsernameReservationResult()
    object PendingOffline : UsernameReservationResult()
    data class Error(val message: String) : UsernameReservationResult()
    data class Cooldown(
        val remainingDays: Int,
        val remainingHours: Int,
        val remainingMinutes: Int
    ) : UsernameReservationResult()
}

class NameTakenException : Exception("NAME_TAKEN")
class NameCooldownException(val remainingMs: Long) : Exception("NAME_COOLDOWN")

class UsernameRepository(
    private val memoryQuestDao: MemoryQuestDao,
    private val leaderboardRepository: LeaderboardRepository,
    firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseBootstrap.requireReady()
        firestoreProvider()
    }

    companion object {
        private const val LOG_TAG = "MemoryQuestUsername"
    }

    /**
     * Checks if normalized username is available in Firestore (without reserving).
     */
    suspend fun checkAvailabilityOnline(normalizedName: String, currentUid: String?): Boolean {
        return try {
            val uid = currentUid ?: leaderboardRepository.ensureAuthenticated()
            
            // 1. Check usernames collection
            val usernameDoc = firestore.collection("usernames").document(normalizedName).get().await()
            if (usernameDoc.exists()) {
                val ownerUid = usernameDoc.getString("uid")
                if (ownerUid != null && ownerUid != uid) {
                    return false
                }
            }

            // 2. Check leaderboard collection for existing matching name
            val leaderboardSnapshot = firestore.collection("leaderboard").get().await()
            for (doc in leaderboardSnapshot.documents) {
                val existingUid = doc.getString("uid") ?: doc.id
                val existingName = doc.getString("name") ?: ""
                val existingNormalized = doc.getString("normalizedName") 
                    ?: UsernameNormalizer.normalizeUsername(existingName)
                if (existingNormalized == normalizedName && existingUid != uid) {
                    return false
                }
            }

            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Falha ao verificar disponibilidade online para $normalizedName: ${e.message}")
            true
        }
    }

    /**
     * Generates and verifies available username suggestions online.
     */
    suspend fun getAvailableSuggestionsOnline(displayName: String, currentUid: String?): List<String> {
        val rawSuggestions = UsernameSuggestionGenerator.generateSuggestions(displayName)
        val verified = mutableListOf<String>()
        for (suggestion in rawSuggestions) {
            val normalized = UsernameNormalizer.normalizeUsername(suggestion)
            if (checkAvailabilityOnline(normalized, currentUid)) {
                verified.add(suggestion)
            }
        }
        return verified
    }

    /**
     * Reserves or changes a username online via Firestore transaction or saves provisionally offline.
     */
    suspend fun reserveUsername(
        displayName: String,
        isOnline: Boolean
    ): UsernameReservationResult {
        val validation = UsernameValidator.validate(displayName)
        if (validation is UsernameValidator.ValidationResult.Invalid) {
            Log.w(LOG_TAG, "Formato de nome inválido digitado: '$displayName'")
            return UsernameReservationResult.Error("Formato de nome inválido")
        }

        val newDisplayName = displayName.trim()
        val newNormalizedName = UsernameNormalizer.normalizeUsername(newDisplayName)
        val player = memoryQuestDao.getPlayer() ?: PlayerEntity()

        val oldNormalizedName = player.confirmedNormalizedName.ifEmpty {
            player.pendingNormalizedName.ifEmpty {
                UsernameNormalizer.normalizeUsername(player.name)
            }
        }

        Log.d(LOG_TAG, "Iniciando alteração de username. Novo: '$newDisplayName' ($newNormalizedName), Antigo: '$oldNormalizedName', Online: $isOnline")

        if (!isOnline) {
            val updatedPlayer = player.copy(
                name = newDisplayName,
                pendingDisplayName = newDisplayName,
                pendingNormalizedName = newNormalizedName,
                usernameStatus = UsernameStatus.PENDING_VALIDATION.name
            )
            memoryQuestDao.insertOrUpdatePlayer(updatedPlayer)
            Log.d(LOG_TAG, "OFFLINE: Salvo nome provisório '$newDisplayName' ($newNormalizedName)")
            return UsernameReservationResult.PendingOffline
        }

        // ONLINE FLOW
        // Rule 10: Não executar signOut() ou signInAnonymously() se já autenticado
        Log.d(LOG_TAG, "[RESERVATION] Etapa 1: Verificação de internet -> isOnline=$isOnline")
        Log.d(LOG_TAG, "[RESERVATION] Etapa 2: Verificação do FirebaseAuth")
        val uid = leaderboardRepository.ensureAuthenticated()
        Log.d(LOG_TAG, "[RESERVATION] UID autenticado: $uid")
        if (uid.isNullOrEmpty()) {
            Log.e(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: Usuário não autenticado no FirebaseAuth (UID nulo)")
            return UsernameReservationResult.Error("Sua sessão expirou. Faça login novamente.")
        }

        // Document references for transaction
        val oldUsernameRef = if (oldNormalizedName.isNotEmpty()) {
            firestore.collection("usernames").document(oldNormalizedName)
        } else null

        val newUsernameRef = firestore.collection("usernames").document(newNormalizedName)
        val leaderboardRef = firestore.collection("leaderboard").document(uid)
        val usernameSettingsRef = firestore.collection("username_settings").document(uid)

        val isNameChange = oldNormalizedName.isNotEmpty() && oldNormalizedName != newNormalizedName

        return try {
            // Rule 1: Executar uma única transação online
            firestore.runTransaction { transaction ->
                // Rule 2: Fazer TODAS as leituras antes de TODAS as gravações
                Log.d(LOG_TAG, "[RESERVATION] Etapa 3: Leitura de username_settings/$uid")
                val usernameSettingsDoc = transaction.get(usernameSettingsRef)

                Log.d(LOG_TAG, "[RESERVATION] Etapa 4: Leitura de usernames/$newNormalizedName")
                val oldUsernameDoc = oldUsernameRef?.let { transaction.get(it) }
                val newUsernameDoc = transaction.get(newUsernameRef)

                Log.d(LOG_TAG, "[RESERVATION] Etapa 5: Leitura de leaderboard/$uid")
                val leaderboardDoc = transaction.get(leaderboardRef)

                // Etapa 6: Validação do limite de 7 dias se for uma alteração de nome
                Log.d(LOG_TAG, "[RESERVATION] Etapa 6: Verificação do prazo de 7 dias")
                if (isNameChange && usernameSettingsDoc.exists()) {
                    val lastChangeAt = usernameSettingsDoc.getTimestamp("lastUsernameChangeAt")
                    if (lastChangeAt != null) {
                        val lastMs = lastChangeAt.toDate().time
                        val nextAvailableMs = lastMs + (7 * 24 * 60 * 60 * 1000L)
                        val nowMs = System.currentTimeMillis()
                        if (nowMs < nextAvailableMs) {
                            Log.w(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: Limite de 7 dias não atingido")
                            throw NameCooldownException(nextAvailableMs - nowMs)
                        }
                    }
                }

                // Rule 3, 4, 5: Verificar e criar/manter/cancelar o novo username
                if (!newUsernameDoc.exists()) {
                    // Rule 3: Se o novo username não existir: criar exatamente com:
                    // uid, displayName, normalizedName, createdAt = FieldValue.serverTimestamp()
                    val newUsernameData = hashMapOf<String, Any>(
                        "uid" to uid,
                        "displayName" to newDisplayName,
                        "normalizedName" to newNormalizedName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(newUsernameRef, newUsernameData)
                } else {
                    val ownerUid = newUsernameDoc.getString("uid")
                    if (ownerUid == uid) {
                        // Rule 4: Se o novo username já existir e pertencer ao UID atual: não executar set() nele.
                    } else {
                        // Rule 5: Se pertencer a outro UID: cancelar como NAME_TAKEN.
                        Log.w(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: Nome '$newDisplayName' ($newNormalizedName) já pertence a outro UID '$ownerUid'")
                        throw NameTakenException()
                    }
                }

                // Rule 6: Atualizar o leaderboard usando transaction.update()
                if (leaderboardDoc.exists()) {
                    transaction.update(
                        leaderboardRef,
                        mapOf(
                            "name" to newDisplayName,
                            "normalizedName" to newNormalizedName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Rule 7: Excluir usernames/{oldNormalizedName}
                if (oldNormalizedName.isNotEmpty() && oldNormalizedName != newNormalizedName) {
                    if (oldUsernameDoc != null && oldUsernameDoc.exists()) {
                        val oldUid = oldUsernameDoc.getString("uid")
                        if (oldUid == uid) {
                            transaction.delete(oldUsernameRef!!)
                        }
                    }
                }

                // Atualizar / Criar controle em username_settings/{uid}
                if (!usernameSettingsDoc.exists()) {
                    val settingsData = hashMapOf<String, Any?>(
                        "uid" to uid,
                        "lastUsernameChangeAt" to if (isNameChange) FieldValue.serverTimestamp() else null,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(usernameSettingsRef, settingsData)
                } else if (isNameChange) {
                    val settingsUpdate = hashMapOf<String, Any>(
                        "lastUsernameChangeAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.update(usernameSettingsRef, settingsUpdate)
                }
            }.await()

            // Rule 9: Atualizar DataStore/Room local e interface somente depois do commit bem-sucedido.
            val confirmedPlayer = player.copy(
                name = newDisplayName,
                confirmedDisplayName = newDisplayName,
                confirmedNormalizedName = newNormalizedName,
                pendingDisplayName = "",
                pendingNormalizedName = "",
                usernameStatus = UsernameStatus.CONFIRMED.name
            )
            memoryQuestDao.insertOrUpdatePlayer(confirmedPlayer)
            Log.d(LOG_TAG, "Alteração concluída com sucesso. Player atualizado para CONFIRMED com nome '$newDisplayName'")

            val stats = memoryQuestDao.getStatistics()
            leaderboardRepository.ensureLeaderboardExists(confirmedPlayer, stats)

            UsernameReservationResult.Success
        } catch (e: NameCooldownException) {
            Log.w(LOG_TAG, "Alteração de nome bloqueada pelo limite de 7 dias (restam ${e.remainingMs} ms)")
            val days = (e.remainingMs / (24 * 60 * 60 * 1000L)).toInt()
            val hours = (e.remainingMs / (60 * 60 * 1000L)).toInt()
            val minutes = (e.remainingMs / (60 * 1000L)).toInt()
            UsernameReservationResult.Cooldown(days, hours, minutes)
        } catch (e: NameTakenException) {
            Log.w(LOG_TAG, "Nome '$newDisplayName' ($newNormalizedName) já pertence a outro UID")
            val suggestions = getAvailableSuggestionsOnline(newDisplayName, uid)
            UsernameReservationResult.Taken(suggestions)
        } catch (e: FirebaseFirestoreException) {
            Log.e(LOG_TAG, "[RESERVATION] Erro Firestore na alteração de nome: [${e.code}] ${e.message}", e)
            if (e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                val suggestions = getAvailableSuggestionsOnline(newDisplayName, uid)
                UsernameReservationResult.Taken(suggestions)
            } else if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val docPath = "usernames/$newNormalizedName"
                Log.e(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: PERMISSION_DENIED no caminho $docPath. Mensagem: ${e.message}")
                UsernameReservationResult.Error("Erro de permissão no servidor ($docPath).")
            } else {
                Log.e(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: Firestore indisponível (${e.code}).")
                UsernameReservationResult.Error("Não foi possível conectar ao servidor.")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "[RESERVATION] MOTIVO DO BLOQUEIO: Exceção ao alterar username: ${e.message}", e)
            UsernameReservationResult.Error("Não foi possível conectar ao servidor.")
        }
    }

    /**
     * Validates pending provisional username when network becomes available.
     */
    suspend fun validatePendingUsernameOnline(): Boolean {
        val player = memoryQuestDao.getPlayer() ?: return true

        if (player.usernameStatus != UsernameStatus.PENDING_VALIDATION.name && player.pendingNormalizedName.isEmpty()) {
            return true
        }

        val pendingName = player.pendingDisplayName.ifEmpty { player.name }.trim()
        val pendingNormalized = player.pendingNormalizedName.ifEmpty { UsernameNormalizer.normalizeUsername(pendingName) }

        if (pendingNormalized.isEmpty()) return true

        Log.d(LOG_TAG, "Iniciando validação pendente online para '$pendingName' ($pendingNormalized)")

        val uid = leaderboardRepository.ensureAuthenticated()
        if (uid.isNullOrEmpty()) return false

        val oldNormalizedName = player.confirmedNormalizedName
        val oldUsernameRef = if (oldNormalizedName.isNotEmpty()) firestore.collection("usernames").document(oldNormalizedName) else null
        val newUsernameRef = firestore.collection("usernames").document(pendingNormalized)
        val leaderboardRef = firestore.collection("leaderboard").document(uid)

        return try {
            firestore.runTransaction { transaction ->
                val oldUsernameDoc = oldUsernameRef?.let { transaction.get(it) }
                val newUsernameDoc = transaction.get(newUsernameRef)
                val leaderboardDoc = transaction.get(leaderboardRef)

                if (!newUsernameDoc.exists()) {
                    val newUsernameData = hashMapOf<String, Any>(
                        "uid" to uid,
                        "displayName" to pendingName,
                        "normalizedName" to pendingNormalized,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(newUsernameRef, newUsernameData)
                } else {
                    val ownerUid = newUsernameDoc.getString("uid")
                    if (ownerUid != uid) {
                        throw NameTakenException()
                    }
                }

                if (leaderboardDoc.exists()) {
                    transaction.update(
                        leaderboardRef,
                        mapOf(
                            "name" to pendingName,
                            "normalizedName" to pendingNormalized,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (oldNormalizedName.isNotEmpty() && oldNormalizedName != pendingNormalized) {
                    if (oldUsernameDoc != null && oldUsernameDoc.exists()) {
                        val oldUid = oldUsernameDoc.getString("uid")
                        if (oldUid == uid) {
                            transaction.delete(oldUsernameRef!!)
                        }
                    }
                }
            }.await()

            val confirmedPlayer = player.copy(
                name = pendingName,
                confirmedDisplayName = pendingName,
                confirmedNormalizedName = pendingNormalized,
                pendingDisplayName = "",
                pendingNormalizedName = "",
                usernameStatus = UsernameStatus.CONFIRMED.name
            )
            memoryQuestDao.insertOrUpdatePlayer(confirmedPlayer)

            val stats = memoryQuestDao.getStatistics()
            leaderboardRepository.ensureLeaderboardExists(confirmedPlayer, stats)
            true
        } catch (e: Exception) {
            if (e is NameTakenException) {
                val conflictPlayer = player.copy(usernameStatus = UsernameStatus.CONFLICT.name)
                memoryQuestDao.insertOrUpdatePlayer(conflictPlayer)
            }
            false
        }
    }
}

