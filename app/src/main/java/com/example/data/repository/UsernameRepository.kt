package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.entity.PlayerEntity
import com.example.data.model.UsernameStatus
import com.example.util.UsernameNormalizer
import com.example.util.UsernameSuggestionGenerator
import com.example.util.UsernameValidator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

sealed class UsernameReservationResult {
    object Success : UsernameReservationResult()
    data class Taken(val availableSuggestions: List<String>) : UsernameReservationResult()
    object PendingOffline : UsernameReservationResult()
    data class Error(val message: String) : UsernameReservationResult()
}

class UsernameRepository(
    private val memoryQuestDao: MemoryQuestDao,
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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
     * Reserves a username online via Firestore transaction or saves provisionally offline.
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

        val normalizedName = UsernameNormalizer.normalizeUsername(displayName)
        val player = memoryQuestDao.getPlayer() ?: PlayerEntity()

        Log.d(LOG_TAG, "Iniciando reserva de username. Nome digitado: '$displayName', Normalizado: '$normalizedName', Conexão online: $isOnline")

        if (!isOnline) {
            // OFFLINE FLOW: Save provisional name and set PENDING_VALIDATION
            val updatedPlayer = player.copy(
                name = displayName,
                pendingDisplayName = displayName,
                pendingNormalizedName = normalizedName,
                usernameStatus = UsernameStatus.PENDING_VALIDATION.name
            )
            memoryQuestDao.insertOrUpdatePlayer(updatedPlayer)
            Log.d(LOG_TAG, "OFFLINE: Salvo nome provisório '$displayName' ($normalizedName). Mudança de status para PENDING_VALIDATION")
            return UsernameReservationResult.PendingOffline
        }

        // ONLINE FLOW
        // 1. Autenticar
        val uid = leaderboardRepository.ensureAuthenticated()
        Log.d(LOG_TAG, "UID autenticado: $uid")
        if (uid.isNullOrEmpty()) {
            Log.e(LOG_TAG, "Erro de autenticação: UID retornado é nulo")
            val pendingPlayer = player.copy(
                name = displayName,
                pendingDisplayName = displayName,
                pendingNormalizedName = normalizedName,
                usernameStatus = UsernameStatus.PENDING_VALIDATION.name
            )
            memoryQuestDao.insertOrUpdatePlayer(pendingPlayer)
            return UsernameReservationResult.Error("Erro na autenticação com o servidor")
        }

        // 2. Transação no Firestore
        Log.d(LOG_TAG, "Início da transação no Firestore para o documento: usernames/$normalizedName")

        return try {
            val usernameRef = firestore.collection("usernames").document(normalizedName)
            var isConflict = false

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameRef)
                if (snapshot.exists()) {
                    val existingUid = snapshot.getString("uid")
                    if (existingUid != uid) {
                        Log.d(LOG_TAG, "Conflito na transação: usernames/$normalizedName já pertence ao UID $existingUid (atual: $uid)")
                        isConflict = true
                        return@runTransaction
                    }
                }

                val usernameData = hashMapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "normalizedName" to normalizedName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(usernameRef, usernameData, SetOptions.merge())
            }.await()

            if (isConflict) {
                Log.d(LOG_TAG, "Conflito confirmado na reserva do nome '$displayName'")
                val suggestions = getAvailableSuggestionsOnline(displayName, uid)
                UsernameReservationResult.Taken(suggestions)
            } else {
                Log.d(LOG_TAG, "Sucesso na transação da coleção usernames/$normalizedName")

                // Deletar reserva antiga se o nome confirmado mudou
                val oldNormalized = player.confirmedNormalizedName
                if (oldNormalized.isNotEmpty() && oldNormalized != normalizedName) {
                    try {
                        firestore.collection("usernames").document(oldNormalized).delete().await()
                        Log.d(LOG_TAG, "Antiga reserva 'usernames/$oldNormalized' excluída com sucesso")
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Erro ao excluir reserva antiga 'usernames/$oldNormalized': ${e.message}")
                    }
                }

                // 3. Marcar CONFIRMED e salvar
                val confirmedPlayer = player.copy(
                    name = displayName,
                    confirmedDisplayName = displayName,
                    confirmedNormalizedName = normalizedName,
                    pendingDisplayName = "",
                    pendingNormalizedName = "",
                    usernameStatus = UsernameStatus.CONFIRMED.name
                )
                memoryQuestDao.insertOrUpdatePlayer(confirmedPlayer)
                Log.d(LOG_TAG, "Mudança de status para CONFIRMED. Player local atualizado com sucesso")

                // 4. Criar ou atualizar leaderboard/{uid}
                Log.d(LOG_TAG, "Criação do documento leaderboard/$uid")
                val stats = memoryQuestDao.getStatistics()
                leaderboardRepository.syncLeaderboard(confirmedPlayer, stats)

                UsernameReservationResult.Success
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(LOG_TAG, "Código e mensagem de exceção Firestore: [${e.code}] ${e.message}", e)
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    Log.e(LOG_TAG, "PERMISSÃO NEGADA: As regras do Firestore bloquearam a gravação em usernames/$normalizedName.")
                    val pendingPlayer = player.copy(
                        name = displayName,
                        pendingDisplayName = displayName,
                        pendingNormalizedName = normalizedName,
                        usernameStatus = UsernameStatus.PENDING_VALIDATION.name
                    )
                    memoryQuestDao.insertOrUpdatePlayer(pendingPlayer)
                    UsernameReservationResult.PendingOffline
                }
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> {
                    Log.d(LOG_TAG, "Tratando ALREADY_EXISTS como conflito de nome")
                    val suggestions = getAvailableSuggestionsOnline(displayName, uid)
                    UsernameReservationResult.Taken(suggestions)
                }
                FirebaseFirestoreException.Code.UNAVAILABLE -> {
                    Log.w(LOG_TAG, "Servidor Firestore indisponível. Mantendo em PENDING_VALIDATION")
                    val pendingPlayer = player.copy(
                        name = displayName,
                        pendingDisplayName = displayName,
                        pendingNormalizedName = normalizedName,
                        usernameStatus = UsernameStatus.PENDING_VALIDATION.name
                    )
                    memoryQuestDao.insertOrUpdatePlayer(pendingPlayer)
                    UsernameReservationResult.PendingOffline
                }
                else -> {
                    Log.e(LOG_TAG, "Exceção Firestore não tratada: ${e.message}", e)
                    val pendingPlayer = player.copy(
                        name = displayName,
                        pendingDisplayName = displayName,
                        pendingNormalizedName = normalizedName,
                        usernameStatus = UsernameStatus.PENDING_VALIDATION.name
                    )
                    memoryQuestDao.insertOrUpdatePlayer(pendingPlayer)
                    UsernameReservationResult.PendingOffline
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exceção geral ao reservar username: ${e.message}", e)
            val pendingPlayer = player.copy(
                name = displayName,
                pendingDisplayName = displayName,
                pendingNormalizedName = normalizedName,
                usernameStatus = UsernameStatus.PENDING_VALIDATION.name
            )
            memoryQuestDao.insertOrUpdatePlayer(pendingPlayer)
            UsernameReservationResult.PendingOffline
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

        val pendingName = player.pendingDisplayName.ifEmpty { player.name }
        val pendingNormalized = player.pendingNormalizedName.ifEmpty { UsernameNormalizer.normalizeUsername(pendingName) }

        if (pendingNormalized.isEmpty()) return true

        Log.d(LOG_TAG, "Iniciando validação pendente online para '$pendingName' ($pendingNormalized)")

        val uid = leaderboardRepository.ensureAuthenticated()
        Log.d(LOG_TAG, "UID autenticado para validação pendente: $uid")
        if (uid.isNullOrEmpty()) {
            Log.e(LOG_TAG, "UID nulo na validação pendente")
            return false
        }

        Log.d(LOG_TAG, "Início da transação no Firestore para o documento: usernames/$pendingNormalized")

        return try {
            val usernameRef = firestore.collection("usernames").document(pendingNormalized)
            var isConflict = false

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameRef)
                if (snapshot.exists()) {
                    val existingUid = snapshot.getString("uid")
                    if (existingUid != uid) {
                        Log.d(LOG_TAG, "Conflito na transação pendente: usernames/$pendingNormalized pertence a $existingUid (atual: $uid)")
                        isConflict = true
                        return@runTransaction
                    }
                }

                val usernameData = hashMapOf(
                    "uid" to uid,
                    "displayName" to pendingName,
                    "normalizedName" to pendingNormalized,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(usernameRef, usernameData, SetOptions.merge())
            }.await()

            if (isConflict) {
                Log.d(LOG_TAG, "Conflito na validação pendente do nome '$pendingName'. Mudança de status para CONFLICT")
                val conflictPlayer = player.copy(
                    usernameStatus = UsernameStatus.CONFLICT.name
                )
                memoryQuestDao.insertOrUpdatePlayer(conflictPlayer)
                false
            } else {
                Log.d(LOG_TAG, "Sucesso na transação da coleção usernames/$pendingNormalized")
                val confirmedPlayer = player.copy(
                    name = pendingName,
                    confirmedDisplayName = pendingName,
                    confirmedNormalizedName = pendingNormalized,
                    pendingDisplayName = "",
                    pendingNormalizedName = "",
                    usernameStatus = UsernameStatus.CONFIRMED.name
                )
                memoryQuestDao.insertOrUpdatePlayer(confirmedPlayer)
                Log.d(LOG_TAG, "Mudança de status para CONFIRMED. Player local atualizado")

                Log.d(LOG_TAG, "Criação do documento leaderboard/$uid")
                val stats = memoryQuestDao.getStatistics()
                leaderboardRepository.syncLeaderboard(confirmedPlayer, stats)
                true
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(LOG_TAG, "Código e mensagem de exceção Firestore na validação pendente: [${e.code}] ${e.message}", e)
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    Log.e(LOG_TAG, "PERMISSÃO NEGADA: As regras do Firestore bloquearam a gravação em usernames/$pendingNormalized")
                    false
                }
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> {
                    Log.d(LOG_TAG, "ALREADY_EXISTS na validação pendente, marcando como CONFLICT")
                    val conflictPlayer = player.copy(usernameStatus = UsernameStatus.CONFLICT.name)
                    memoryQuestDao.insertOrUpdatePlayer(conflictPlayer)
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exceção ao validar username pendente online: ${e.message}", e)
            false
        }
    }
}

