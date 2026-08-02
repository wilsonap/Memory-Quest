package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.LeaderboardRepository
import com.example.data.repository.PendingSyncRepository
import com.example.data.repository.UsernameRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "MemoryQuestSyncWorker"
        private const val TAG = "GameSync"
    }

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d(TAG, "SYNC START: SyncWorker execution initiated")
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val pendingSyncDao = db.pendingSyncDao()
            val memoryQuestDao = db.memoryQuestDao()
            val leaderboardRepo = LeaderboardRepository()

            // 1. Validate pending username first
            val usernameRepo = UsernameRepository(
                memoryQuestDao = memoryQuestDao,
                leaderboardRepository = leaderboardRepo
            )
            val usernameValid = usernameRepo.validatePendingUsernameOnline()
            Log.d(TAG, "Username validation result: $usernameValid")

            // 2. Sync game pending records to leaderboard
            val pendingRepo = PendingSyncRepository(
                pendingSyncDao = pendingSyncDao,
                memoryQuestDao = memoryQuestDao,
                leaderboardRepository = leaderboardRepo
            )

            val syncSuccess = pendingRepo.syncAll()
            if (syncSuccess && usernameValid) {
                Log.d(TAG, "SYNC SUCCESS: SyncWorker completed successfully")
                ListenableWorker.Result.success()
            } else {
                Log.d(TAG, "SYNC FAILED: SyncWorker encountered errors or pending username conflict")
                ListenableWorker.Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SYNC FAILED: SyncWorker exception: ${e.message}")
            ListenableWorker.Result.retry()
        }
    }
}
