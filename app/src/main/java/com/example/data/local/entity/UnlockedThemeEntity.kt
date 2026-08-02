package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_themes")
data class UnlockedThemeEntity(
    @PrimaryKey val themeId: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
