package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_quests")
data class DailyQuestEntity(
    @PrimaryKey val id: String, // "quest_1", "quest_2", "quest_3"
    val questType: String, // "FIND_PAIRS", "COMPLETE_LEVELS", "THREE_STARS", "WIN_NO_HELP", "COMBO", "FINISH_WITH_LIFE"
    val targetProgress: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val dateString: String // "yyyy-MM-dd"
)
