package com.example.data.repository

import android.util.Log
import com.example.avatar.util.AvatarStorageManager
import com.example.config.FirebaseBootstrap
import com.example.data.local.DataStoreManager
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.dao.PendingSyncDao
import com.example.data.local.entity.PlayerEntity
import com.example.util.UsernameNormalizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Exclusão completa de conta: Firestore (batch) → Auth.delete() → limpeza local.
 *
 * Documentos ausentes no Firestore não são erro — o batch.delete() é idempotente.
 * Não chama signInAnonymously() neste fluxo.
 */
class DeleteAccountRepository(
    authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val memoryQuestDao: MemoryQuestDao,
    private val pendingSyncDao: PendingSyncDao,
    private val dataStoreManager: DataStoreManager,
    private val avatarStorageManager: AvatarStorageManager,
    private val gameRepository: GameRepository
) {

    private val auth: FirebaseAuth by lazy {
        FirebaseBootstrap.requireReady()
        authProvider()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseBootstrap.requireReady()
        firestoreProvider()
    }

    companion object {
        const val TAG = "MemoryQuestDeleteAccount"

        fun resolveNormalizedNameForDeletion(player: PlayerEntity?): String {
            if (player == null) return ""
            return when {
                player.confirmedNormalizedName.isNotBlank() -> player.confirmedNormalizedName.trim()
                player.pendingNormalizedName.isNotBlank() -> player.pendingNormalizedName.trim()
                player.name.isNotBlank() -> UsernameNormalizer.normalizeUsername(player.name)
                else -> ""
            }
        }
    }

    suspend fun deleteAccount(isOnline: Boolean): DeleteAccountResult {
        logStep("=== INÍCIO EXCLUSÃO DE CONTA ===")

        if (!isOnline) {
            logStep("Abortado: sem conexão com a internet")
            return DeleteAccountResult.NoInternet
        }

        // 1. Obter currentUser e UID
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        logStep("1. UID atual: ${uid ?: "null"}")
        logStep("2. usuário autenticado existe? ${currentUser != null}")

        if (currentUser == null || uid.isNullOrBlank()) {
            logStep("Abortado: sessão inválida (currentUser nulo)")
            return DeleteAccountResult.InvalidSession
        }

        // 2. Obter normalizedName local
        val player = memoryQuestDao.getPlayer()
        val normalizedName = resolveNormalizedNameForDeletion(player)
        logStep("3. normalizedName encontrado? '${normalizedName.ifEmpty { "(vazio — usernames não será excluído)" }}'")

        // 3–4. Montar e executar batch Firestore
        try {
            val batch = firestore.batch()
            var queued = 0

            if (normalizedName.isNotBlank()) {
                val docPath = "usernames/$normalizedName"
                logStep("4. usernames/$normalizedName — incluir delete no batch (doc pode não existir)")
                batch.delete(firestore.collection("usernames").document(normalizedName))
                queued++
            } else {
                logStep("4. normalizedName vazio — pulando usernames (não excluir usernames/null)")
            }

            logStep("5. leaderboard/$uid — incluir delete no batch (doc pode não existir)")
            batch.delete(firestore.collection("leaderboard").document(uid))
            queued++

            logStep("6. username_settings/$uid — incluir delete no batch (doc pode não existir)")
            batch.delete(firestore.collection("username_settings").document(uid))
            queued++

            logStep("7. user_consents/$uid — incluir delete no batch (doc pode não existir)")
            batch.delete(firestore.collection("user_consents").document(uid))
            queued++

            logStep("8. batch criado com $queued documento(s)")

            batch.commit().await()
            logStep("9. batch commit concluído com sucesso")
        } catch (e: FirebaseFirestoreException) {
            logFailure("batch.commit", docPath = "batch", e)
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> DeleteAccountResult.PermissionDenied
                FirebaseFirestoreException.Code.UNAVAILABLE -> DeleteAccountResult.NoInternet
                else -> DeleteAccountResult.RemoteFailure(e)
            }
        } catch (e: Exception) {
            logFailure("batch.commit", docPath = "batch", e)
            return DeleteAccountResult.RemoteFailure(e)
        }

        // 7. Excluir FirebaseAuth ANTES da limpeza local (preserva dados se auth falhar)
        try {
            currentUser.delete().await()
            logStep("11. FirebaseAuth.currentUser.delete() concluído")
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            logFailure("FirebaseAuth.currentUser.delete()", docPath = "auth/$uid", e)
            logStep("ERROR_REQUIRES_RECENT_LOGIN — dados locais preservados, conta auth não excluída")
            return DeleteAccountResult.RequiresRecentLogin
        } catch (e: Exception) {
            logFailure("FirebaseAuth.currentUser.delete()", docPath = "auth/$uid", e)
            return DeleteAccountResult.AuthDeleteFailure(e)
        }

        // 6. Limpeza local somente após sucesso remoto + auth
        try {
            clearAllLocalData()
            logStep("10. limpeza local concluída")
        } catch (e: Exception) {
            logFailure("limpeza local", docPath = "local", e)
            // Auth já foi excluído; remoto já limpo — tratar como sucesso parcial
            logStep("Aviso: auth e remoto OK, mas limpeza local falhou parcialmente")
        }

        logStep("12. novo usuário anônimo NÃO criado neste método")
        logStep("=== EXCLUSÃO CONCLUÍDA COM SUCESSO ===")
        return DeleteAccountResult.Success
    }

    private suspend fun clearAllLocalData() {
        avatarStorageManager.removeCustomAvatarFile()
        pendingSyncDao.deleteAll()
        gameRepository.wipeAllLocalDataForAccountDeletion()
        dataStoreManager.clearAccountData()
        gameRepository.ensureInitialized()
    }

    private fun logStep(message: String) {
        Log.d(TAG, message)
    }

    private fun logFailure(step: String, docPath: String, e: Exception) {
        val firebaseCode = (e as? FirebaseFirestoreException)?.code?.name ?: "N/A"
        Log.e(
            TAG,
            "FALHA na etapa '$step' | doc='$docPath' | código=$firebaseCode | msg=${e.message}",
            e
        )
    }
}
