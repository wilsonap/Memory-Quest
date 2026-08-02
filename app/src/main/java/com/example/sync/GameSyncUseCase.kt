package com.example.sync

import android.content.Context
import com.example.data.repository.PendingSyncRepository

class GameSyncUseCase(
    private val pendingSyncRepository: PendingSyncRepository
) {
    suspend fun onGameFinished(
        context: Context,
        totalScore: Long,
        highestLevel: Int,
        bestStreak: Int,
        totalPairs: Int,
        gamesCompleted: Int
    ) {
        pendingSyncRepository.createPendingSync(
            totalScore = totalScore,
            highestLevel = highestLevel,
            bestStreak = bestStreak,
            totalPairs = totalPairs,
            gamesCompleted = gamesCompleted
        )

        SyncManager.scheduleSync(context)
    }
}
