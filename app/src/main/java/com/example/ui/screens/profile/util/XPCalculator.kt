package com.example.ui.screens.profile.util

import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity

data class XPInfo(
    val level: Int,
    val totalXp: Long,
    val currentLevelXp: Long,
    val requiredLevelXp: Long,
    val progress: Float, // 0.0 to 1.0
    val xpRemaining: Long
)

object XPCalculator {
    fun calculateXp(player: PlayerEntity?, stats: StatisticsEntity?): XPInfo {
        val level = (player?.highestLevel ?: 1).coerceAtLeast(1)
        val score = player?.currentLevel?.let { it * 100L } ?: 0L
        val pairs = (stats?.totalPairsFound ?: 0).toLong()
        val wins = (stats?.wins ?: 0).toLong()
        val totalGames = (stats?.totalGames ?: 0).toLong()

        // Formula for accumulated total XP
        val totalXp = (level * 1000L) + (pairs * 15L) + (wins * 50L) + (totalGames * 10L)

        // Required XP for current level scale
        val requiredLevelXp = (level * 400L) + 600L

        // Current progress within the level
        val currentLevelXp = totalXp % requiredLevelXp
        val progress = (currentLevelXp.toFloat() / requiredLevelXp.toFloat()).coerceIn(0f, 1f)
        val xpRemaining = (requiredLevelXp - currentLevelXp).coerceAtLeast(0L)

        return XPInfo(
            level = level,
            totalXp = totalXp,
            currentLevelXp = currentLevelXp,
            requiredLevelXp = requiredLevelXp,
            progress = progress,
            xpRemaining = xpRemaining
        )
    }
}
