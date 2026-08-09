package com.example.data.repository

import android.util.Log
import com.example.data.local.DataStoreManager
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.GameTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    companion object {
        val DAILY_REWARDS = listOf(50, 75, 100, 125, 150, 200, 300)
    }

    data class DailyRewardStatus(
        val canClaim: Boolean,
        val currentStreak: Int,
        val nextRewardAmount: Int,
        val isClaimedToday: Boolean
    )

    suspend fun ensureInitialized() {
        val player = dao.getPlayer()
        if (player == null) {
            // New user setup: starts with 300 coins
            val newPlayer = PlayerEntity(
                id = 1,
                name = "", // Empty name signals first-run onboarding!
                coins = 300,
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
                totalCoinsEarned = 300,
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

    suspend fun addCoins(amount: Int, reason: String = "EARNED") {
        if (amount <= 0) return
        dao.addCoins(amount)
        val stats = dao.getStatistics()
        if (stats != null) {
            dao.insertOrUpdateStatistics(
                stats.copy(totalCoinsEarned = stats.totalCoinsEarned + amount)
            )
        }
        val player = dao.getPlayer()
        if (player != null) {
            checkAndIncrementAchievement("ach_1000_coins", player.coins)
        }
        Log.d("MemoryQuestEconomy", "addCoins: +$amount ($reason)")
    }

    suspend fun spendCoins(amount: Int, reason: String = "PURCHASE"): Boolean {
        if (amount <= 0) return true
        val player = dao.getPlayer() ?: return false
        if (player.coins >= amount) {
            dao.addCoins(-amount)
            Log.d("MemoryQuestEconomy", "spendCoins: -$amount ($reason)")
            return true
        }
        Log.d("MemoryQuestEconomy", "spendCoins failed: balance ${player.coins} < $amount ($reason)")
        return false
    }

    suspend fun getDailyRewardStatus(): DailyRewardStatus {
        val player = dao.getPlayer() ?: return DailyRewardStatus(false, 0, 50, false)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayString = dateFormat.format(Date())

        val lastRewardDateString = if (player.lastDailyRewardDate > 0) {
            dateFormat.format(Date(player.lastDailyRewardDate))
        } else ""

        if (lastRewardDateString == todayString) {
            val currentStreak = if (player.dailyRewardStreak in 1..7) player.dailyRewardStreak else 1
            val nextAmount = DAILY_REWARDS[(currentStreak - 1).coerceIn(0, 6)]
            return DailyRewardStatus(
                canClaim = false,
                currentStreak = currentStreak,
                nextRewardAmount = nextAmount,
                isClaimedToday = true
            )
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayString = dateFormat.format(calendar.time)

        val nextStreak = if (lastRewardDateString == yesterdayString) {
            if (player.dailyRewardStreak >= 7) 1 else player.dailyRewardStreak + 1
        } else {
            1
        }

        val nextAmount = DAILY_REWARDS[(nextStreak - 1).coerceIn(0, 6)]
        return DailyRewardStatus(
            canClaim = true,
            currentStreak = nextStreak,
            nextRewardAmount = nextAmount,
            isClaimedToday = false
        )
    }

    suspend fun claimDailyReward(): Int? {
        val status = getDailyRewardStatus()
        if (!status.canClaim) return null

        val player = dao.getPlayer() ?: return null
        val updatedPlayer = player.copy(
            lastDailyRewardDate = System.currentTimeMillis(),
            dailyRewardStreak = status.currentStreak
        )
        dao.insertOrUpdatePlayer(updatedPlayer)
        addCoins(status.nextRewardAmount, "DAILY_REWARD_DAY_${status.currentStreak}")
        return status.nextRewardAmount
    }

    suspend fun claimRewardedAdCoins(): Boolean {
        val player = dao.getPlayer() ?: return false
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayString = dateFormat.format(Date())

        val (todayCount, dateToSave) = if (player.rewardedAdsDate == todayString) {
            player.rewardedAdsToday to todayString
        } else {
            0 to todayString
        }

        if (todayCount >= 5) {
            Log.w("RewardedAd", "Daily limit of 5 ads reached for today.")
            return false
        }

        val newCount = todayCount + 1
        val updatedPlayer = player.copy(
            rewardedAdsToday = newCount,
            rewardedAdsDate = dateToSave
        )
        dao.insertOrUpdatePlayer(updatedPlayer)
        addCoins(100, "REWARDED_AD")
        return true
    }

    suspend fun getRemainingRewardedAdsToday(): Int {
        val player = dao.getPlayer() ?: return 5
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayString = dateFormat.format(Date())
        return if (player.rewardedAdsDate == todayString) {
            (5 - player.rewardedAdsToday).coerceAtLeast(0)
        } else {
            5
        }
    }

    suspend fun updateEquippedTheme(themeId: String) {
        dao.updateEquippedTheme(themeId)
    }

    suspend fun updateEquippedFrame(frameId: String) {
        dao.updateEquippedFrame(frameId)
    }

    suspend fun unlockTheme(themeId: String, price: Int) {
        if (spendCoins(price, "UNLOCK_THEME_$themeId")) {
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
            if (coinsEarnedInGame > 0) {
                addCoins(coinsEarnedInGame, "LEVEL_WIN_REWARD")
            }

            checkAndIncrementAchievement("ach_first_win", 1)?.let { unlockedList.add(it) }
            checkAndIncrementAchievement("ach_10_levels", nextLevel)?.let { unlockedList.add(it) }
            checkAndIncrementAchievement("ach_50_levels", nextLevel)?.let { unlockedList.add(it) }
            if (flawless) {
                checkAndIncrementAchievement("ach_flawless", 1)?.let { unlockedList.add(it) }
            }
        } else {
            if (coinsEarnedInGame > 0) {
                addCoins(coinsEarnedInGame, "GAME_LOSS_PARTIAL_REWARD")
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
                addCoins(ach.rewardCoins, "ACHIEVEMENT_$achievementId")
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
            // Preserve current user's coins and historical stats!
            val resetPlayer = player.copy(
                currentLevel = 1,
                highestLevel = 1,
                remainingHints = 3,
                extraLives = 0
            )
            dao.insertOrUpdatePlayer(resetPlayer)

            val currentStats = dao.getStatistics()
            if (currentStats != null) {
                val resetStats = currentStats.copy(
                    totalGames = 0,
                    wins = 0,
                    losses = 0,
                    totalTimeSeconds = 0,
                    highestStreak = 0,
                    totalPairsFound = 0,
                    totalFlawlessWins = 0,
                    totalFlips = 0,
                    correctFlips = 0
                )
                dao.insertOrUpdateStatistics(resetStats)
            }
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
