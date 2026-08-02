package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.dao.PendingSyncDao
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.model.UsernameStatus
import com.example.util.UsernameNormalizer
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PendingSyncRepository(
    private val pendingSyncDao: PendingSyncDao,
    private val memoryQuestDao: MemoryQuestDao,
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "GameSync"
    }

    val unsyncedCountFlow: Flow<Int> = pendingSyncDao.getUnsyncedCountFlow()

    suspend fun createPendingSync(
        totalScore: Long,
        highestLevel: Int,
        bestStreak: Int,
        totalPairs: Int,
        gamesCompleted: Int
    ): PendingSyncEntity? {
        Log.d(TAG, "SYNC START: Enqueuing pending sync record")
        val uid = leaderboardRepository.ensureAuthenticated() ?: "local_player"

        val entity = PendingSyncEntity(
            uid = uid,
            totalScore = totalScore,
            highestLevel = highestLevel,
            bestStreak = bestStreak,
            totalPairs = totalPairs,
            gamesCompleted = gamesCompleted,
            createdAt = System.currentTimeMillis(),
            status = PendingSyncEntity.STATUS_PENDING,
            retryCount = 0
        )

        val id = pendingSyncDao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun syncAll(): Boolean {
        val player = memoryQuestDao.getPlayer()
        // Do not publish to global leaderboard until username is confirmed
        val isConfirmed = player?.usernameStatus == UsernameStatus.CONFIRMED.name
        if (!isConfirmed) {
            Log.d(TAG, "SYNC START: Skipping leaderboard publish because username status is '${player?.usernameStatus}'")
            return false
        }

        val pendingList = pendingSyncDao.getUnsynced()
        if (pendingList.isEmpty()) {
            Log.d(TAG, "SYNC START: No pending game records to sync")
            return true
        }

        Log.d(TAG, "SYNC START: Found ${pendingList.size} pending sync items")
        val displayName = player?.confirmedDisplayName?.ifEmpty { player.name }?.trim()?.ifEmpty { "Explorador" } ?: "Explorador"
        val avatar = player?.equippedFrameId ?: "classic"

        var allSuccessful = true

        for (item in pendingList) {
            Log.d(TAG, "SYNC START: Syncing item #${item.id} (attempt ${item.retryCount + 1})")

            val syncingItem = item.copy(status = PendingSyncEntity.STATUS_SYNCING)
            pendingSyncDao.update(syncingItem)

            try {
                val uid = if (item.uid == "local_player") {
                    leaderboardRepository.ensureAuthenticated() ?: throw IllegalStateException("No authenticated user")
                } else {
                    item.uid
                }

                var existingScore = 0L
                var existingLevel = 1L
                var existingStreak = 0L
                var existingPairs = 0L
                var existingGames = 0L

                try {
                    val docSnapshot = firestore.collection("leaderboard").document(uid).get().await()
                    if (docSnapshot.exists()) {
                        existingScore = docSnapshot.getLong("totalScore") ?: 0L
                        existingLevel = docSnapshot.getLong("highestLevel") ?: 1L
                        existingStreak = docSnapshot.getLong("bestStreak") ?: 0L
                        existingPairs = docSnapshot.getLong("totalPairs") ?: 0L
                        existingGames = docSnapshot.getLong("gamesCompleted") ?: 0L
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "SYNC RETRY: Could not fetch existing doc, merging: ${e.message}")
                }

                val finalScore = maxOf(existingScore, item.totalScore)
                val finalLevel = maxOf(existingLevel, item.highestLevel.toLong())
                val finalStreak = maxOf(existingStreak, item.bestStreak.toLong())
                val finalPairs = maxOf(existingPairs, item.totalPairs.toLong())
                val finalGames = maxOf(existingGames, item.gamesCompleted.toLong())
                val normalizedName = UsernameNormalizer.normalizeUsername(displayName)

                val data = hashMapOf(
                    "uid" to uid,
                    "name" to displayName,
                    "normalizedName" to normalizedName,
                    "avatar" to avatar,
                    "totalScore" to finalScore,
                    "highestLevel" to finalLevel,
                    "totalPairs" to finalPairs,
                    "bestStreak" to finalStreak,
                    "gamesCompleted" to finalGames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("leaderboard")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .await()

                Log.d(TAG, "SYNC SUCCESS: Item #${item.id} synced to Firestore leaderboard")
                pendingSyncDao.deleteById(item.id)

            } catch (e: Exception) {
                allSuccessful = false
                Log.d(TAG, "SYNC FAILED: Item #${item.id} failed: ${e.message}")

                val failedItem = item.copy(
                    status = PendingSyncEntity.STATUS_FAILED,
                    retryCount = item.retryCount + 1
                )
                pendingSyncDao.update(failedItem)
            }
        }

        return allSuccessful
    }

    suspend fun hasPendingSyncs(): Boolean {
        return pendingSyncDao.getUnsyncedCount() > 0
    }
}
