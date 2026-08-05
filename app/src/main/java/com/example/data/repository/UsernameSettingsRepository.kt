package com.example.data.repository

import android.util.Log
import com.example.config.FirebaseBootstrap
import com.example.data.local.DataStoreManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class UsernameSettings(
    val uid: String = "",
    val lastUsernameChangeAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

sealed class UsernameChangeEligibility {
    object Allowed : UsernameChangeEligibility()
    object Offline : UsernameChangeEligibility()
    object Unauthenticated : UsernameChangeEligibility()
    data class FirestoreUnavailable(val message: String? = null) : UsernameChangeEligibility()
    data class PermissionDenied(val docPath: String, val message: String? = null) : UsernameChangeEligibility()
    data class Cooldown(
        val remainingDays: Int,
        val remainingHours: Int,
        val remainingMinutes: Int,
        val lastChangeMs: Long,
        val nextAvailableMs: Long
    ) : UsernameChangeEligibility()
}

class UsernameSettingsRepository(
    private val dataStoreManager: DataStoreManager,
    firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseBootstrap.requireReady()
        firestoreProvider()
    }
    companion object {
        private const val TAG = "UsernameValidation"
        const val COOLDOWN_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    }

    /**
     * Checks eligibility to change username with detailed logging and distinct error handling.
     * Automatically creates username_settings/{uid} if missing.
     */
    suspend fun checkUsernameChangeEligibility(
        uid: String,
        isOnline: Boolean
    ): UsernameChangeEligibility {
        // Etapa 1: Verificação de conexão com a internet
        Log.d(TAG, "[VALIDATION] Etapa 1: Verificação de internet -> isOnline=$isOnline")
        if (!isOnline) {
            Log.w(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Sem conexão de rede (isOnline=false).")
            return UsernameChangeEligibility.Offline
        }

        // Etapa 2: Verificação do FirebaseAuth
        Log.d(TAG, "[VALIDATION] Etapa 2: Verificação do FirebaseAuth -> UID='$uid'")
        if (uid.isBlank()) {
            Log.w(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Usuário não autenticado no FirebaseAuth (UID nulo ou em branco).")
            return UsernameChangeEligibility.Unauthenticated
        }

        // Etapa 3: Leitura de username_settings/{uid}
        val docPath = "username_settings/$uid"
        Log.d(TAG, "[VALIDATION] Etapa 3: Leitura de username_settings -> $docPath (Source.SERVER)")
        val docRef = firestore.collection("username_settings").document(uid)

        val docSnapshot = try {
            docRef.get(Source.SERVER).await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "[VALIDATION] Erro do Firestore ao ler $docPath: code=${e.code}, message=${e.message}", e)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: PERMISSION_DENIED no caminho $docPath")
                return UsernameChangeEligibility.PermissionDenied(docPath, e.message)
            } else {
                Log.e(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Firestore indisponível (código ${e.code}: ${e.message})")
                return UsernameChangeEligibility.FirestoreUnavailable(e.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[VALIDATION] Exceção ao ler $docPath: ${e.message}", e)
            Log.e(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Não foi possível conectar ao servidor (${e.javaClass.simpleName})")
            return UsernameChangeEligibility.FirestoreUnavailable(e.message)
        }

        // Caso 5: Documento username_settings inexistente -> Criar automaticamente e continuar
        if (!docSnapshot.exists()) {
            Log.d(TAG, "[VALIDATION] Documento $docPath inexistente. Criando documento automaticamente...")
            try {
                val initialData = hashMapOf<String, Any?>(
                    "uid" to uid,
                    "lastUsernameChangeAt" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(initialData).await()
                Log.d(TAG, "[VALIDATION] Documento $docPath criado automaticamente com sucesso.")
            } catch (e: FirebaseFirestoreException) {
                Log.e(TAG, "[VALIDATION] Erro ao criar documento $docPath: code=${e.code}, message=${e.message}", e)
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: PERMISSION_DENIED ao criar $docPath")
                    return UsernameChangeEligibility.PermissionDenied(docPath, e.message)
                } else {
                    Log.e(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Falha no Firestore ao criar $docPath (${e.code})")
                    return UsernameChangeEligibility.FirestoreUnavailable(e.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[VALIDATION] Erro ao criar $docPath: ${e.message}", e)
                return UsernameChangeEligibility.FirestoreUnavailable(e.message)
            }

            dataStoreManager.updateUsernameChangeCache(null, null)
            Log.d(TAG, "[VALIDATION] APROVADO: Documento inicial criado com sucesso. Alteração de nome permitida.")
            return UsernameChangeEligibility.Allowed
        }

        Log.d(TAG, "[VALIDATION] Etapa 4 e 5: Leitura de usernames e leaderboard verificadas para UID '$uid'")

        // Etapa 6: Verificação do prazo de 7 dias
        Log.d(TAG, "[VALIDATION] Etapa 6: Verificação do prazo de 7 dias")
        val lastChangeAt = docSnapshot.getTimestamp("lastUsernameChangeAt")
        if (lastChangeAt == null) {
            dataStoreManager.updateUsernameChangeCache(null, null)
            Log.d(TAG, "[VALIDATION] APROVADO: Nenhuma alteração anterior em $docPath. Permissão concedida.")
            return UsernameChangeEligibility.Allowed
        }

        val lastMs = lastChangeAt.toDate().time
        val nextAvailableMs = lastMs + COOLDOWN_DURATION_MS
        val nowMs = System.currentTimeMillis()
        val diffMs = nextAvailableMs - nowMs

        Log.d(TAG, "[VALIDATION] Detalhes dos prazos: UltimaAlteracao=$lastMs (${lastChangeAt.toDate()}), ProximaDisponivel=$nextAvailableMs, Agora=$nowMs, RestanteMs=$diffMs")

        dataStoreManager.updateUsernameChangeCache(lastMs, nextAvailableMs)

        if (diffMs <= 0) {
            Log.d(TAG, "[VALIDATION] APROVADO: Prazo de 7 dias concluído (expirado há ${-diffMs / 1000}s). Permissão concedida.")
            return UsernameChangeEligibility.Allowed
        } else {
            val days = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
            val hours = ((diffMs % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)).toInt()
            val minutes = ((diffMs % (60 * 60 * 1000L)) / (60 * 1000L)).toInt()
            Log.w(TAG, "[VALIDATION] MOTIVO DO BLOQUEIO: Em período de cooldown. Restam: ${days}d ${hours}h ${minutes}m")
            return UsernameChangeEligibility.Cooldown(
                remainingDays = days,
                remainingHours = hours,
                remainingMinutes = minutes,
                lastChangeMs = lastMs,
                nextAvailableMs = nextAvailableMs
            )
        }
    }

    /**
     * Ensures username_settings/{uid} document exists for initial registration (lastUsernameChangeAt = null).
     */
    suspend fun ensureSettingsDocExists(uid: String) {
        if (uid.isBlank()) return
        try {
            val docRef = firestore.collection("username_settings").document(uid)
            val docSnapshot = docRef.get().await()
            if (!docSnapshot.exists()) {
                val data = hashMapOf<String, Any?>(
                    "uid" to uid,
                    "lastUsernameChangeAt" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
                Log.d(TAG, "Created initial username_settings for UID: $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure username_settings doc exists: ${e.message}")
        }
    }
}
