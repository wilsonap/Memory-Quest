package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.InventoryEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryQuestDao {

    // --- Player ---
    @Query("SELECT * FROM player WHERE id = 1")
    fun getPlayerFlow(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun getPlayer(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlayer(player: PlayerEntity)

    @Query("UPDATE player SET name = :name WHERE id = 1")
    suspend fun updatePlayerName(name: String)

    @Query("UPDATE player SET coins = coins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)

    @Query("UPDATE player SET currentLevel = :level, highestLevel = CASE WHEN :level > highestLevel THEN :level ELSE highestLevel END WHERE id = 1")
    suspend fun updatePlayerLevel(level: Int)

    @Query("UPDATE player SET equippedThemeId = :themeId WHERE id = 1")
    suspend fun updateEquippedTheme(themeId: String)

    @Query("UPDATE player SET equippedFrameId = :frameId WHERE id = 1")
    suspend fun updateEquippedFrame(frameId: String)

    @Query("UPDATE player SET avatarType = :avatarType, avatarPresetId = :presetId, avatarLocalPath = :localPath, avatarUpdatedAt = :updatedAt WHERE id = 1")
    suspend fun updatePlayerAvatar(avatarType: String, presetId: String, localPath: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE player SET remainingHints = remainingHints + :amount WHERE id = 1")
    suspend fun addHints(amount: Int)

    @Query("UPDATE player SET extraLives = extraLives + :amount WHERE id = 1")
    suspend fun addExtraLives(amount: Int)

    @Query("UPDATE player SET extraLives = :count WHERE id = 1")
    suspend fun setExtraLives(count: Int)


    // --- Statistics ---
    @Query("SELECT * FROM statistics WHERE id = 1")
    fun getStatisticsFlow(): Flow<StatisticsEntity?>

    @Query("SELECT * FROM statistics WHERE id = 1")
    suspend fun getStatistics(): StatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatistics(stats: StatisticsEntity)


    // --- Unlocked Themes ---
    @Query("SELECT * FROM unlocked_themes")
    fun getUnlockedThemesFlow(): Flow<List<UnlockedThemeEntity>>

    @Query("SELECT * FROM unlocked_themes")
    suspend fun getUnlockedThemes(): List<UnlockedThemeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockTheme(theme: UnlockedThemeEntity)


    // --- Inventory ---
    @Query("SELECT * FROM inventory")
    fun getInventoryFlow(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInventoryItem(item: InventoryEntity)

    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getInventoryItem(itemId: String): InventoryEntity?


    // --- Achievements ---
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, achievementId ASC")
    fun getAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM unlocked_themes")
    suspend fun clearUnlockedThemes()

    @Query("DELETE FROM inventory")
    suspend fun clearInventory()

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    @Query("DELETE FROM player")
    suspend fun deleteAllPlayers()

    @Query("DELETE FROM statistics")
    suspend fun deleteAllStatistics()
}
