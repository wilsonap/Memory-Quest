package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey val id: Int = 1,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalTimeSeconds: Long = 0,
    val highestStreak: Int = 0,
    val totalPairsFound: Int = 0,
    val consecutiveDays: Int = 1,
    val totalFlawlessWins: Int = 0,
    val totalCoinsEarned: Int = 0,
    val totalFlips: Int = 0,
    val correctFlips: Int = 0
)
