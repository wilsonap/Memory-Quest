package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.DataStoreManager
import com.example.data.repository.ConsentRepository

class ConsentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "sync_user_consent"
        private const val LOG_TAG = "ConsentSyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ConsentSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(LOG_TAG, "ConsentSyncWorker scheduled with NetworkType.CONNECTED")
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d(LOG_TAG, "Worker started: syncing user consent")
        return try {
            val dataStore = DataStoreManager(applicationContext)
            val consentRepo = ConsentRepository(dataStore)
            val success = consentRepo.syncConsentOnline()
            if (success) {
                Log.d(LOG_TAG, "Worker SUCCESS: consent synced to Firestore")
                Result.success()
            } else {
                Log.d(LOG_TAG, "Worker RETRY: consent sync temporary failure")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Worker EXCEPTION: ${e.message}", e)
            Result.retry()
        }
    }
}
