package com.example.data.repository

import android.util.Log
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.model.UsernameStatus
import com.example.util.UsernameNormalizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class LeaderboardPlayer(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "",
    val totalScore: Long = 0L,
    val highestLevel: Long = 1L,
    val totalPairs: Long = 0L,
    val bestStreak: Long = 0L,
    val gamesCompleted: Long = 0L,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false
)

class LeaderboardRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val LOG_TAG = "MemoryQuestUsername"
    }

    /**
     * Ensures an anonymous Firebase Auth user exists.
     * Reuses existing user UID if already authenticated.
     */
    suspend fun ensureAuthenticated(): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(LOG_TAG, "UID autenticado já existente: ${currentUser.uid}")
                currentUser.uid
            } else {
                Log.d(LOG_TAG, "Iniciando signInAnonymously no Firebase Auth...")
                val result = auth.signInAnonymously().await()
                val newUid = result.user?.uid
                Log.d(LOG_TAG, "Autenticado anonimamente com sucesso. UID: $newUid")
                newUid
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Falha na autenticação Firebase Auth: ${e.message}", e)
            auth.currentUser?.uid
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Syncs user's leaderboard public data to Firestore under leaderboard/{uid}.
     * Only runs if usernameStatus is CONFIRMED.
     */
    suspend fun syncLeaderboard(player: PlayerEntity, stats: StatisticsEntity?): Result<Unit> {
        if (player.usernameStatus != UsernameStatus.CONFIRMED.name) {
            Log.d(LOG_TAG, "Ordem preservada: Não criando leaderboard/{uid} pois usernameStatus é '${player.usernameStatus}' (não é CONFIRMED)")
            return Result.failure(IllegalStateException("Username not confirmed"))
        }

        return try {
            val uid = ensureAuthenticated() ?: return Result.failure(Exception("Sem autenticação disponível"))
            
            val totalPairs = stats?.totalPairsFound?.toLong() ?: 0L
            val bestStreak = stats?.highestStreak?.toLong() ?: 0L
            val gamesCompleted = stats?.totalGames?.toLong() ?: 0L
            val highestLevel = player.highestLevel.toLong()
            
            // Total score formula: Level weight + pairs + streak + games
            val totalScore = (highestLevel * 1000L) + (totalPairs * 10L) + (bestStreak * 50L) + (gamesCompleted * 20L)
            val displayName = player.confirmedDisplayName.ifEmpty { player.name }.trim().ifEmpty { "Explorador" }
            val normalizedName = UsernameNormalizer.normalizeUsername(displayName)

            val leaderboardData = hashMapOf(
                "uid" to uid,
                "name" to displayName,
                "normalizedName" to normalizedName,
                "avatar" to player.equippedFrameId,
                "totalScore" to totalScore,
                "highestLevel" to highestLevel,
                "totalPairs" to totalPairs,
                "bestStreak" to bestStreak,
                "gamesCompleted" to gamesCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            Log.d(LOG_TAG, "Criando/Atualizando leaderboard/$uid com normalizedName '$normalizedName'")

            firestore.collection("leaderboard")
                .document(uid)
                .set(leaderboardData, SetOptions.merge())
                .await()

            Log.d(LOG_TAG, "Leaderboard/$uid atualizado com sucesso no Firestore!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao sincronizar leaderboard: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches top 100 players ordered by totalScore descending from Firestore.
     */
    suspend fun fetchTop100Leaderboard(): Result<List<LeaderboardPlayer>> {
        return try {
            val currentUid = ensureAuthenticated()
            val querySnapshot = firestore.collection("leaderboard")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val list = querySnapshot.documents.mapIndexed { index, doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("name") ?: "Explorador"
                val avatar = doc.getString("avatar") ?: "classic"
                val totalScore = doc.getLong("totalScore") ?: 0L
                val highestLevel = doc.getLong("highestLevel") ?: 1L
                val totalPairs = doc.getLong("totalPairs") ?: 0L
                val bestStreak = doc.getLong("bestStreak") ?: 0L
                val gamesCompleted = doc.getLong("gamesCompleted") ?: 0L

                LeaderboardPlayer(
                    uid = uid,
                    name = name,
                    avatar = avatar,
                    totalScore = totalScore,
                    highestLevel = highestLevel,
                    totalPairs = totalPairs,
                    bestStreak = bestStreak,
                    gamesCompleted = gamesCompleted,
                    rank = index + 1,
                    isCurrentUser = (currentUid != null && uid == currentUid)
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
