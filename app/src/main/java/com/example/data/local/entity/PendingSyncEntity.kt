package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val totalScore: Long,
    val highestLevel: Int,
    val bestStreak: Int,
    val totalPairs: Int,
    val gamesCompleted: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val retryCount: Int = 0
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCING = "SYNCING"
        const val STATUS_FAILED = "FAILED"
    }
}
