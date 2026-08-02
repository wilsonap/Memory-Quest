package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val title: String,
    val description: String,
    val iconName: String,
    val currentProgress: Int = 0,
    val maxProgress: Int = 1,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val rewardCoins: Int = 50
)
