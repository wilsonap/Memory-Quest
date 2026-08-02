package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSyncEntity): Long

    @Update
    suspend fun update(entity: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getUnsynced(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync WHERE status != 'SYNCED'")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM pending_sync WHERE status != 'SYNCED'")
    fun getUnsyncedCountFlow(): Flow<Int>

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
