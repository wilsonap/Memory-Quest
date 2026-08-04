package com.example.data.repository

import com.example.data.local.DataStoreManager
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.GameTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(
    private val dao: MemoryQuestDao,
    private val dataStoreManager: DataStoreManager
) {
    val playerFlow: Flow<PlayerEntity?> = dao.getPlayerFlow()
    val statisticsFlow: Flow<StatisticsEntity?> = dao.getStatisticsFlow()

    suspend fun getStatistics(): StatisticsEntity? = dao.getStatistics()
    val unlockedThemesFlow: Flow<List<UnlockedThemeEntity>> = dao.getUnlockedThemesFlow()
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.getAchievementsFlow()

    val soundEnabled: Flow<Boolean> = dataStoreManager.soundEnabled
    val musicEnabled: Flow<Boolean> = dataStoreManager.musicEnabled
    val vibrationEnabled: Flow<Boolean> = dataStoreManager.vibrationEnabled
    val musicVolume: Flow<Float> = dataStoreManager.musicVolume
    val sfxVolume: Flow<Float> = dataStoreManager.sfxVolume
    val isAdsRemoved: Flow<Boolean> = dataStoreManager.isAdsRemoved
    val language: Flow<String> = dataStoreManager.language
    val darkMode: Flow<String> = dataStoreManager.darkMode

    suspend fun ensureInitialized() {
        val player = dao.getPlayer()
        if (player == null) {
            // New user setup
            val newPlayer = PlayerEntity(
                id = 1,
                name = "", // Empty name signals first-run onboarding!
                coins = 100,
                currentLevel = 1,
                highestLevel = 1,
                firstGameDate = System.currentTimeMillis(),
                lastAccessDate = System.currentTimeMillis(),
                remainingHints = 3,
                extraLives = 0,
                equippedThemeId = GameTheme.ANIMALS.id,
                equippedFrameId = "frame_classic"
            )
            dao.insertOrUpdatePlayer(newPlayer)

            // Initial stats
            val initialStats = StatisticsEntity(
                id = 1,
                totalGames = 0,
                wins = 0,
                losses = 0,
                totalTimeSeconds = 0,
                highestStreak = 0,
                totalPairsFound = 0,
                consecutiveDays = 1,
                totalFlawlessWins = 0,
                totalCoinsEarned = 100,
                totalFlips = 0,
                correctFlips = 0
            )
            dao.insertOrUpdateStatistics(initialStats)

            // Unlock default animal theme
            dao.unlockTheme(UnlockedThemeEntity(themeId = GameTheme.ANIMALS.id))

            // Default achievements
            val defaultAchievements = listOf(
                AchievementEntity("ach_first_pair", "Primeiro Par", "Encontre seu 1º par de cartas.", "EmojiEvents", 0, 1, false, rewardCoins = 30),
                AchievementEntity("ach_first_win", "Primeira Vitória", "Conclua a primeira fase com sucesso.", "MilitaryTech", 0, 1, false, rewardCoins = 50),
                AchievementEntity("ach_flawless", "Mente Perfeita", "Vença uma fase sem cometer nenhum erro.", "AutoAwesome", 0, 1, false, rewardCoins = 100),
                AchievementEntity("ach_10_levels", "Explorador de Fases", "Chegue até a Fase 10.", "Explore", 0, 10, false, rewardCoins = 200),
                AchievementEntity("ach_50_levels", "Mestre Supremo", "Alcance a Fase 50.", "MilitaryTech", 0, 50, false, rewardCoins = 500),
                AchievementEntity("ach_100_pairs", "Colecionador de Pares", "Encontre 100 pares no total.", "Extension", 0, 100, false, rewardCoins = 150),
                AchievementEntity("ach_1000_coins", "Magnata das Moedas", "Acumule 1000 moedas.", "MonetizationOn", 0, 1000, false, rewardCoins = 300),
                AchievementEntity("ach_streak_5", "Combo Imparável", "Faça uma sequência de 5 acertos seguidos.", "Bolt", 0, 5, false, rewardCoins = 100)
            )
            dao.insertAchievements(defaultAchievements)
        } else {
            // Update last access date
            dao.insertOrUpdatePlayer(player.copy(lastAccessDate = System.currentTimeMillis()))
        }
    }

    suspend fun setPlayerName(name: String) {
        dao.updatePlayerName(name.trim())
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        dao.insertOrUpdatePlayer(player)
    }

    suspend fun addCoins(amount: Int) {
        dao.addCoins(amount)
        // Check achievement for coins
        val player = dao.getPlayer()
        if (player != null) {
            checkAndIncrementAchievement("ach_1000_coins", player.coins)
        }
    }

    suspend fun updateEquippedTheme(themeId: String) {
        dao.updateEquippedTheme(themeId)
    }

    suspend fun updateEquippedFrame(frameId: String) {
        dao.updateEquippedFrame(frameId)
    }

    suspend fun unlockTheme(themeId: String, price: Int) {
        val player = dao.getPlayer() ?: return
        if (player.coins >= price) {
            dao.addCoins(-price)
            dao.unlockTheme(UnlockedThemeEntity(themeId = themeId))
            dao.updateEquippedTheme(themeId)
        }
    }

    suspend fun updateStatsAfterGame(
        won: Boolean,
        pairsFoundInGame: Int,
        gameDurationSeconds: Long,
        flawless: Boolean,
        coinsEarnedInGame: Int,
        maxStreakInGame: Int,
        totalFlipsInGame: Int
    ): List<AchievementEntity> {
        val currentStats = dao.getStatistics() ?: StatisticsEntity()
        val currentPlayer = dao.getPlayer() ?: PlayerEntity()

        val newGames = currentStats.totalGames + 1
        val newWins = currentStats.wins + (if (won) 1 else 0)
        val newLosses = currentStats.losses + (if (!won) 1 else 0)
        val newTotalTime = currentStats.totalTimeSeconds + gameDurationSeconds
        val newTotalPairs = currentStats.totalPairsFound + pairsFoundInGame
        val newFlawless = currentStats.totalFlawlessWins + (if (won && flawless) 1 else 0)
        val newTotalCoins = currentStats.totalCoinsEarned + coinsEarnedInGame
        val newStreak = maxOf(currentStats.highestStreak, maxStreakInGame)
        val newTotalFlips = currentStats.totalFlips + totalFlipsInGame
        val newCorrectFlips = currentStats.correctFlips + (pairsFoundInGame * 2)

        val updatedStats = currentStats.copy(
            totalGames = newGames,
            wins = newWins,
            losses = newLosses,
            totalTimeSeconds = newTotalTime,
            totalPairsFound = newTotalPairs,
            totalFlawlessWins = newFlawless,
            totalCoinsEarned = newTotalCoins,
            highestStreak = newStreak,
            totalFlips = newTotalFlips,
            correctFlips = newCorrectFlips
        )
        dao.insertOrUpdateStatistics(updatedStats)

        val unlockedList = mutableListOf<AchievementEntity>()

        // Award coins & level progression
        if (won) {
            val nextLevel = currentPlayer.currentLevel + 1
            dao.updatePlayerLevel(nextLevel)
            dao.addCoins(coinsEarnedInGame)

            checkAndIncrementAchievement("ach_first_win", 1)?.let { unlockedList.add(it) }
            checkAndIncrementAchievement("ach_10_levels", nextLevel)?.let { unlockedList.add(it) }
            checkAndIncrementAchievement("ach_50_levels", nextLevel)?.let { unlockedList.add(it) }
            if (flawless) {
                checkAndIncrementAchievement("ach_flawless", 1)?.let { unlockedList.add(it) }
            }
        } else {
            if (coinsEarnedInGame > 0) {
                dao.addCoins(coinsEarnedInGame)
            }
        }

        if (pairsFoundInGame > 0) {
            checkAndIncrementAchievement("ach_first_pair", 1)?.let { unlockedList.add(it) }
            checkAndIncrementAchievement("ach_100_pairs", newTotalPairs)?.let { unlockedList.add(it) }
        }

        if (maxStreakInGame >= 5) {
            checkAndIncrementAchievement("ach_streak_5", maxStreakInGame)?.let { unlockedList.add(it) }
        }

        return unlockedList
    }

    private suspend fun checkAndIncrementAchievement(achievementId: String, currentProgressValue: Int): AchievementEntity? {
        val achievements = dao.getAchievements()
        val ach = achievements.find { it.achievementId == achievementId } ?: return null
        if (!ach.isUnlocked) {
            val newProgress = maxOf(ach.currentProgress, currentProgressValue)
            if (newProgress >= ach.maxProgress) {
                val unlocked = ach.copy(
                    currentProgress = ach.maxProgress,
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                )
                dao.updateAchievement(unlocked)
                dao.addCoins(ach.rewardCoins)
                return unlocked
            } else {
                dao.updateAchievement(ach.copy(currentProgress = newProgress))
            }
        }
        return null
    }

    // Sound / Music / Vibration settings
    suspend fun setSoundEnabled(enabled: Boolean) = dataStoreManager.setSoundEnabled(enabled)
    suspend fun setMusicEnabled(enabled: Boolean) = dataStoreManager.setMusicEnabled(enabled)
    suspend fun setVibrationEnabled(enabled: Boolean) = dataStoreManager.setVibrationEnabled(enabled)
    suspend fun setMusicVolume(vol: Float) = dataStoreManager.setMusicVolume(vol)
    suspend fun setSfxVolume(vol: Float) = dataStoreManager.setSfxVolume(vol)
    suspend fun setLanguage(lang: String) = dataStoreManager.setLanguage(lang)
    suspend fun setDarkMode(mode: String) = dataStoreManager.setDarkMode(mode)
    suspend fun resetDataStore() = dataStoreManager.resetToDefaults()

    suspend fun consumeHint(): Boolean {
        val p = dao.getPlayer() ?: return false
        if (p.remainingHints > 0) {
            dao.addHints(-1)
            return true
        }
        return false
    }

    suspend fun addHints(amount: Int) {
        dao.addHints(amount)
    }

    suspend fun addExtraLives(amount: Int) {
        dao.addExtraLives(amount)
    }

    suspend fun setExtraLives(count: Int) {
        dao.setExtraLives(count)
    }

    suspend fun resetGameProgress() {
        val player = dao.getPlayer()
        if (player != null) {
            val resetPlayer = player.copy(
                coins = 100,
                currentLevel = 1,
                highestLevel = 1,
                remainingHints = 3,
                extraLives = 0
            )
            dao.insertOrUpdatePlayer(resetPlayer)

            val initialStats = StatisticsEntity(
                id = 1,
                totalGames = 0,
                wins = 0,
                losses = 0,
                totalTimeSeconds = 0,
                highestStreak = 0,
                totalPairsFound = 0,
                consecutiveDays = 1,
                totalFlawlessWins = 0,
                totalCoinsEarned = 100,
                totalFlips = 0,
                correctFlips = 0
            )
            dao.insertOrUpdateStatistics(initialStats)
        }
    }

    /** Apaga todo o progresso local após exclusão remota bem-sucedida da conta. */
    suspend fun wipeAllLocalDataForAccountDeletion() {
        dao.clearAchievements()
        dao.clearInventory()
        dao.clearUnlockedThemes()
        dao.deleteAllStatistics()
        dao.deleteAllPlayers()
    }
}
