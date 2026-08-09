package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val coins: Int = 300,
    val currentLevel: Int = 1,
    val highestLevel: Int = 1,
    val firstGameDate: Long = System.currentTimeMillis(),
    val lastAccessDate: Long = System.currentTimeMillis(),
    val remainingHints: Int = 3,
    val extraLives: Int = 0,
    val equippedThemeId: String = "animals",
    val equippedFrameId: String = "classic",
    val usernameStatus: String = "NOT_SELECTED",
    val pendingDisplayName: String = "",
    val pendingNormalizedName: String = "",
    val confirmedDisplayName: String = "",
    val confirmedNormalizedName: String = "",
    val avatarType: String = "PRESET",
    val avatarPresetId: String = "avatar_01",
    val avatarLocalPath: String = "",
    val avatarUpdatedAt: Long = System.currentTimeMillis(),
    val lastDailyRewardDate: Long = 0L,
    val dailyRewardStreak: Int = 0,
    val rewardedAdsToday: Int = 0,
    val rewardedAdsDate: String = ""
) {
    val bonusLives: Int get() = extraLives
    val defaultLives: Int get() = 3
    val totalLives: Int get() = defaultLives + bonusLives
}
