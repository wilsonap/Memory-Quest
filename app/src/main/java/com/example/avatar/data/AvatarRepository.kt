package com.example.avatar.data

import android.content.Context
import android.util.Log
import com.example.avatar.model.AvatarPreset
import com.example.avatar.model.AvatarType
import com.example.avatar.util.AvatarStorageManager
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.entity.PlayerEntity
import com.example.data.repository.LeaderboardRepository
import com.example.data.repository.PendingSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AvatarRepository(
    private val dao: MemoryQuestDao,
    private val storageManager: AvatarStorageManager,
    private val leaderboardRepository: LeaderboardRepository,
    private val pendingSyncRepository: PendingSyncRepository
) {
    companion object {
        private const val LOG_TAG = "AvatarRepository"
    }

    val playerFlow: Flow<PlayerEntity?> = dao.getPlayerFlow()

    suspend fun selectPresetAvatar(presetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val preset = AvatarPreset.getById(presetId)
            val now = System.currentTimeMillis()
            
            // Delete custom file if any to free disk space
            storageManager.removeCustomAvatarFile()

            dao.updatePlayerAvatar(
                avatarType = AvatarType.PRESET.name,
                presetId = preset.id,
                localPath = "",
                updatedAt = now
            )

            // Trigger leaderboard sync if applicable
            syncLeaderboardIfPossible()

            Log.d(LOG_TAG, "Avatar preset selecionado: ${preset.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao selecionar avatar preset: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveCustomAvatar(localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()

            dao.updatePlayerAvatar(
                avatarType = AvatarType.CUSTOM.name,
                presetId = "avatar_01", // Default fallback preset for ranking
                localPath = localPath,
                updatedAt = now
            )

            syncLeaderboardIfPossible()

            Log.d(LOG_TAG, "Avatar customizado salvo: $localPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao salvar avatar customizado: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resetToDefaultAvatar(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storageManager.removeCustomAvatarFile()
            val now = System.currentTimeMillis()

            dao.updatePlayerAvatar(
                avatarType = AvatarType.PRESET.name,
                presetId = "avatar_01",
                localPath = "",
                updatedAt = now
            )

            syncLeaderboardIfPossible()

            Log.d(LOG_TAG, "Avatar resetado para o padrão avatar_01")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao resetar avatar: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncLeaderboardIfPossible() {
        try {
            val player = dao.getPlayer()
            val stats = dao.getStatistics()
            if (player != null) {
                leaderboardRepository.syncLeaderboard(player, stats)
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Aviso ao sincronizar leaderboard após trocar avatar: ${e.message}")
        }
    }
}
