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

class EnsureLeaderboardWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ensure_leaderboard_exists"
        private const val LOG_TAG = "MemoryQuestLeaderboardEnsure"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<EnsureLeaderboardWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(LOG_TAG, "EnsureLeaderboardWorker agendado com WORK_NAME '$WORK_NAME' e NetworkType.CONNECTED")
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d(LOG_TAG, "EnsureLeaderboardWorker iniciado: executando verificação de leaderboard")
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.memoryQuestDao()
            val player = dao.getPlayer()
            val stats = dao.getStatistics()

            if (player == null) {
                Log.d(LOG_TAG, "Player local nulo no EnsureLeaderboardWorker")
                Log.d(LOG_TAG, "EnsureLeaderboardWorker concluído")
                return ListenableWorker.Result.success()
            }

            val leaderboardRepository = LeaderboardRepository()
            val result = leaderboardRepository.ensureLeaderboardExists(player, stats)

            if (result.isSuccess) {
                Log.d(LOG_TAG, "EnsureLeaderboardWorker concluído com sucesso")
                ListenableWorker.Result.success()
            } else {
                Log.w(LOG_TAG, "EnsureLeaderboardWorker falhou na execução, tentando novamente (retry)")
                ListenableWorker.Result.retry()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "EnsureLeaderboardWorker exceção: ${e.message}", e)
            ListenableWorker.Result.retry()
        }
    }
}
