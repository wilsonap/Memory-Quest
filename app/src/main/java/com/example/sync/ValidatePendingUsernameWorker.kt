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
import com.example.data.local.AppDatabase
import com.example.data.repository.LeaderboardRepository
import com.example.data.repository.UsernameRepository

class ValidatePendingUsernameWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "validate_pending_username"
        private const val LOG_TAG = "MemoryQuestUsername"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ValidatePendingUsernameWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(LOG_TAG, "Agendado ValidatePendingUsernameWorker com WORK_NAME '$WORK_NAME' e NetworkType.CONNECTED")
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d(LOG_TAG, "Worker iniciado: executando validação de username pendente")
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val usernameRepo = UsernameRepository(
                memoryQuestDao = db.memoryQuestDao(),
                leaderboardRepository = LeaderboardRepository()
            )

            val success = usernameRepo.validatePendingUsernameOnline()
            if (success) {
                Log.d(LOG_TAG, "Worker SUCESSO: reserva de username confirmada e salva")
                Result.success()
            } else {
                val player = db.memoryQuestDao().getPlayer()
                if (player?.usernameStatus == "CONFLICT") {
                    Log.d(LOG_TAG, "Worker FIM: nome em conflito, aguardando intervenção do usuário no diálogo")
                    Result.failure()
                } else {
                    Log.d(LOG_TAG, "Worker REPLAY: validação temporariamente malsucedida, tentando novamente")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Worker EXCEÇÃO: ${e.message}", e)
            Result.retry()
        }
    }
}

