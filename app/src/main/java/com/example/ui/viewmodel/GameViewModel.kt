package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.ui.screens.profile.util.XPCalculator
import com.example.data.model.GameCard
import com.example.data.model.GameTheme
import com.example.data.model.LevelConfig
import com.example.data.repository.GameRepository
import com.example.data.repository.LeaderboardRepository
import com.example.data.repository.PendingSyncRepository
import com.example.sync.GameSyncUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GameUiStatus {
    data class Previewing(val remainingSeconds: Int) : GameUiStatus
    object Playing : GameUiStatus
    object Paused : GameUiStatus
    data class LevelCompleted(
        val coinsEarned: Int,
        val flawlessBonus: Int,
        val comboBonus: Int,
        val timeSeconds: Long,
        val levelCompletedNumber: Int,
        val errorsCount: Int = 0,
        val pairsFound: Int = 0,
        val totalPairs: Int = 0,
        val maxCombo: Int = 0,
        val totalFlips: Int = 0,
        val accuracyPercent: Int = 100,
        val starsCount: Int = 3,
        val xpEarned: Int = 100,
        val oldXpProgress: Float = 0f,
        val newXpProgress: Float = 0.5f,
        val isLevelUp: Boolean = false,
        val isNewRecord: Boolean = false,
        val unlockedAchievement: AchievementEntity? = null,
        val themeNameRes: Int = 0,
        val themeCategory: String = "Fase"
    ) : GameUiStatus
    data class Defeat(val pairsFoundCount: Int, val levelNumber: Int) : GameUiStatus
}

data class GameState(
    val levelNumber: Int = 1,
    val cards: List<GameCard> = emptyList(),
    val lives: Int = 3,
    val initialLives: Int = 3,
    val coinsEarnedInGame: Int = 0,
    val pairsFound: Int = 0,
    val totalPairs: Int = 0,
    val currentCombo: Int = 0,
    val maxCombo: Int = 0,
    val errorsCount: Int = 0,
    val status: GameUiStatus = GameUiStatus.Previewing(3),
    val elapsedTimeSeconds: Long = 0,
    val isTimerFrozen: Boolean = false,
    val remainingHints: Int = 3,
    val freeHintsCount: Int = 0,
    val freeRevealsCount: Int = 0,
    val freeFreezesCount: Int = 0,
    val theme: GameTheme = GameTheme.ANIMALS,
    val frameId: String = "frame_classic",
    val totalFlips: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dataStore = DataStoreManager(application)
    val repository = GameRepository(db.memoryQuestDao(), dataStore)
    private val pendingSyncRepository = PendingSyncRepository(db.pendingSyncDao(), db.memoryQuestDao())
    private val gameSyncUseCase = GameSyncUseCase(pendingSyncRepository)
    private val audioManager = GameAudioManager.getInstance(application)

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.inventoryFlow.collect { items ->
                val hints = items.find { it.itemId == "booster_hint" }?.quantity ?: 0
                val reveals = items.find { it.itemId == "booster_reveal" }?.quantity ?: 0
                val freezes = items.find { it.itemId == "booster_freeze" }?.quantity ?: 0
                _uiState.update {
                    it.copy(
                        freeHintsCount = hints,
                        freeRevealsCount = reveals,
                        freeFreezesCount = freezes
                    )
                }
            }
        }
    }

    private var firstFlippedIndex: Int? = null
    private var isProcessingFlip = false
    private var isFreezingTimer = false
    private var timerJob: Job? = null
    private var previewJob: Job? = null
    private var startTimeMillis: Long = 0

    fun startLevel(level: Int, themeId: String? = null, frameId: String? = "frame_classic") {
        viewModelScope.launch {
            val player = repository.playerFlow.firstOrNull()
            val selectedTheme = GameTheme.ALL_THEMES.find { it.id == (themeId ?: player?.equippedThemeId ?: "animals") } ?: GameTheme.ANIMALS
            val equippedFrame = frameId ?: player?.equippedFrameId ?: "frame_classic"

            val levelConfig = LevelConfig.getConfigForLevel(level)
            val pairSymbols = selectedTheme.symbols.shuffled().take(levelConfig.pairCount)

            // Duplicate symbols to create pairs
            val rawCards = mutableListOf<GameCard>()
            var idCounter = 1
            pairSymbols.forEachIndexed { index, pair ->
                val pairId = index + 1
                rawCards.add(GameCard(id = idCounter++, pairId = pairId, symbol = pair.first, name = pair.second, isFaceUp = true))
                rawCards.add(GameCard(id = idCounter++, pairId = pairId, symbol = pair.first, name = pair.second, isFaceUp = true))
            }
            rawCards.shuffle()

            val defaultLives = levelConfig.initialLives
            val bonusLives = player?.extraLives ?: 0
            val startingLives = defaultLives + bonusLives
            val availableHints = player?.remainingHints ?: 3

            // Preserva quantidades do inventário Room (startLevel não pode zerar free*Count)
            val inventoryItems = repository.inventoryFlow.firstOrNull().orEmpty()
            val freeHints = inventoryItems.find { it.itemId == "booster_hint" }?.quantity ?: 0
            val freeReveals = inventoryItems.find { it.itemId == "booster_reveal" }?.quantity ?: 0
            val freeFreezes = inventoryItems.find { it.itemId == "booster_freeze" }?.quantity ?: 0

            _uiState.value = GameState(
                levelNumber = level,
                cards = rawCards,
                lives = startingLives,
                initialLives = startingLives,
                coinsEarnedInGame = 0,
                pairsFound = 0,
                totalPairs = levelConfig.pairCount,
                currentCombo = 0,
                maxCombo = 0,
                errorsCount = 0,
                status = GameUiStatus.Previewing(levelConfig.previewSeconds),
                elapsedTimeSeconds = 0,
                remainingHints = availableHints,
                freeHintsCount = freeHints,
                freeRevealsCount = freeReveals,
                freeFreezesCount = freeFreezes,
                theme = selectedTheme,
                frameId = equippedFrame
            )

            firstFlippedIndex = null
            isProcessingFlip = false
            startPreviewCountdown(levelConfig.previewSeconds)
        }
    }

    private fun startPreviewCountdown(seconds: Int) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            for (s in seconds downTo 1) {
                _uiState.update { it.copy(status = GameUiStatus.Previewing(s)) }
                audioManager.playSfx(SoundEffect.COUNTDOWN)
                delay(1000)
            }
            // Turn all cards face down and start playing
            _uiState.update { state ->
                val faceDownCards = state.cards.map { it.copy(isFaceUp = false) }
                state.copy(cards = faceDownCards, status = GameUiStatus.Playing)
            }
            startTimeMillis = System.currentTimeMillis()
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (uiState.value.status == GameUiStatus.Playing) {
                delay(1000)
                if (!_uiState.value.isTimerFrozen) {
                    _uiState.update { it.copy(elapsedTimeSeconds = it.elapsedTimeSeconds + 1) }
                }
            }
        }
    }

    fun onCardClick(cardIndex: Int) {
        val currentState = _uiState.value
        if (currentState.status != GameUiStatus.Playing || isProcessingFlip) return

        val card = currentState.cards.getOrNull(cardIndex) ?: return
        if (card.isFaceUp || card.isMatched) return

        // Flip target card
        val updatedCards = currentState.cards.toMutableList()
        updatedCards[cardIndex] = card.copy(isFaceUp = true)
        _uiState.update { it.copy(cards = updatedCards, totalFlips = it.totalFlips + 1) }
        audioManager.playCardFlip()

        if (firstFlippedIndex == null) {
            // First card flipped
            firstFlippedIndex = cardIndex
        } else {
            // Second card flipped
            val firstIdx = firstFlippedIndex!!
            firstFlippedIndex = null
            isProcessingFlip = true

            val firstCard = updatedCards[firstIdx]
            val secondCard = updatedCards[cardIndex]

            viewModelScope.launch {
                if (firstCard.pairId == secondCard.pairId) {
                    // MATCH!
                    audioManager.playMatch()
                    audioManager.playCoin()
                    delay(300)
                    val newPairsFound = _uiState.value.pairsFound + 1
                    val newCombo = _uiState.value.currentCombo + 1
                    val maxCombo = maxOf(_uiState.value.maxCombo, newCombo)
                    val comboBonus = if (newCombo > 1) (newCombo * 5) else 0
                    val pairCoins = 10 + comboBonus

                    val matchedCards = _uiState.value.cards.toMutableList()
                    matchedCards[firstIdx] = firstCard.copy(isMatched = true)
                    matchedCards[cardIndex] = secondCard.copy(isMatched = true)

                    _uiState.update { state ->
                        state.copy(
                            cards = matchedCards,
                            pairsFound = newPairsFound,
                            currentCombo = newCombo,
                            maxCombo = maxCombo,
                            coinsEarnedInGame = state.coinsEarnedInGame + pairCoins
                        )
                    }

                    isProcessingFlip = false

                    // Update daily quest progress
                    repository.updateDailyQuestProgress("FIND_PAIRS", 1)
                    if (newCombo >= 3) {
                        repository.updateDailyQuestProgress("COMBO", 1)
                    }

                    // Check for level completion
                    if (newPairsFound >= _uiState.value.totalPairs) {
                        onLevelSuccess()
                    }
                } else {
                    // MISMATCH!
                    audioManager.playMismatch()
                    delay(800)
                    val newLives = _uiState.value.lives - 1
                    val newErrors = _uiState.value.errorsCount + 1

                    val levelConfig = LevelConfig.getConfigForLevel(_uiState.value.levelNumber)
                    val defaultLives = levelConfig.initialLives
                    val remainingExtraLives = maxOf(0, newLives - defaultLives)
                    repository.setExtraLives(remainingExtraLives)

                    val resetCards = _uiState.value.cards.toMutableList()
                    resetCards[firstIdx] = firstCard.copy(isFaceUp = false)
                    resetCards[cardIndex] = secondCard.copy(isFaceUp = false)

                    _uiState.update { state ->
                        state.copy(
                            cards = resetCards,
                            lives = newLives,
                            errorsCount = newErrors,
                            currentCombo = 0
                        )
                    }

                    isProcessingFlip = false

                    if (newLives <= 0) {
                        onLevelDefeat()
                    }
                }
            }
        }
    }

    private fun onLevelSuccess() {
        timerJob?.cancel()
        viewModelScope.launch {
            val state = _uiState.value
            val flawless = state.errorsCount == 0
            val noHelpUsed = state.remainingHints == LevelConfig.getConfigForLevel(state.levelNumber).initialLives // or true if no booster consumed

            val stars = when {
                state.errorsCount == 0 -> 3
                state.errorsCount <= 2 -> 2
                else -> 1
            }

            // Update daily quest progress
            repository.updateDailyQuestProgress("COMPLETE_LEVELS", 1)
            if (stars == 3) {
                repository.updateDailyQuestProgress("THREE_STARS", 1)
            }
            if (noHelpUsed || flawless) {
                repository.updateDailyQuestProgress("WIN_NO_HELP", 1)
            }
            if (state.lives >= 1) {
                repository.updateDailyQuestProgress("FINISH_WITH_LIFE", 1)
            }

            // Game economy rewards:
            // Base completion: +40
            // 3 stars bonus: +30
            // No help used bonus: +20
            val baseCompletionReward = 40
            val starsBonus = if (stars == 3) 30 else 0
            val noHelpBonus = if (flawless) 20 else 0
            val totalCoinsEarned = baseCompletionReward + starsBonus + noHelpBonus

            val totalFlipsCount = maxOf(state.totalFlips, state.totalPairs * 2)
            val accuracy = if (totalFlipsCount > 0) {
                (((state.totalPairs * 2).toFloat() / totalFlipsCount.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 100

            val statsBefore = repository.getStatistics()
            val playerBefore = repository.playerFlow.firstOrNull()
            val xpBefore = XPCalculator.calculateXp(playerBefore, statsBefore)

            // Persist to Room!
            val unlockedAchievements = repository.updateStatsAfterGame(
                won = true,
                pairsFoundInGame = state.pairsFound,
                gameDurationSeconds = state.elapsedTimeSeconds,
                flawless = flawless,
                coinsEarnedInGame = totalCoinsEarned,
                maxStreakInGame = state.maxCombo,
                totalFlipsInGame = state.totalFlips
            )

            val statsAfter = repository.getStatistics()
            val playerAfter = repository.playerFlow.firstOrNull()
            val xpAfter = XPCalculator.calculateXp(playerAfter, statsAfter)

            val xpEarned = (stars * 40) + (if (flawless) 50 else 0) + (state.maxCombo * 10)
            val isLevelUp = xpAfter.level > xpBefore.level || (playerAfter?.highestLevel ?: 1) > (playerBefore?.highestLevel ?: 1)
            val isNewRecord = (statsBefore != null) && (
                state.maxCombo > statsBefore.highestStreak ||
                statsBefore.wins == 0 ||
                state.levelNumber > (playerBefore?.highestLevel ?: 1)
            )

            val unlockedAch = unlockedAchievements.firstOrNull()

            _uiState.update {
                it.copy(
                    status = GameUiStatus.LevelCompleted(
                        coinsEarned = totalCoinsEarned,
                        flawlessBonus = noHelpBonus,
                        comboBonus = starsBonus,
                        timeSeconds = state.elapsedTimeSeconds,
                        levelCompletedNumber = state.levelNumber,
                        errorsCount = state.errorsCount,
                        pairsFound = state.pairsFound,
                        totalPairs = state.totalPairs,
                        maxCombo = state.maxCombo,
                        totalFlips = state.totalFlips,
                        accuracyPercent = accuracy,
                        starsCount = stars,
                        xpEarned = xpEarned,
                        oldXpProgress = xpBefore.progress,
                        newXpProgress = xpAfter.progress,
                        isLevelUp = isLevelUp,
                        isNewRecord = isNewRecord,
                        unlockedAchievement = unlockedAch,
                        themeNameRes = state.theme.nameRes,
                        themeCategory = state.theme.category
                    )
                )
            }

            // Queue pending sync record in Room & trigger WorkManager
            gameSyncUseCase.onGameFinished(
                context = getApplication(),
                totalScore = (statsAfter?.totalCoinsEarned ?: 0).toLong(),
                highestLevel = playerAfter?.highestLevel ?: state.levelNumber,
                bestStreak = statsAfter?.highestStreak ?: state.maxCombo,
                totalPairs = statsAfter?.totalPairsFound ?: state.pairsFound,
                gamesCompleted = statsAfter?.totalGames ?: 1
            )
        }
    }

    private fun onLevelDefeat() {
        timerJob?.cancel()
        audioManager.playSfx(SoundEffect.GAME_OVER)
        viewModelScope.launch {
            repository.setExtraLives(0)
            val state = _uiState.value
            _uiState.update { it.copy(status = GameUiStatus.Defeat(state.pairsFound, state.levelNumber)) }

            // Persist stats
            repository.updateStatsAfterGame(
                won = false,
                pairsFoundInGame = state.pairsFound,
                gameDurationSeconds = state.elapsedTimeSeconds,
                flawless = false,
                coinsEarnedInGame = state.coinsEarnedInGame,
                maxStreakInGame = state.maxCombo,
                totalFlipsInGame = state.totalFlips
            )

            // Queue pending sync record in Room & trigger WorkManager
            val stats = repository.getStatistics()
            val player = repository.playerFlow.firstOrNull()
            gameSyncUseCase.onGameFinished(
                context = getApplication(),
                totalScore = (stats?.totalCoinsEarned ?: 0).toLong(),
                highestLevel = player?.highestLevel ?: state.levelNumber,
                bestStreak = stats?.highestStreak ?: state.maxCombo,
                totalPairs = stats?.totalPairsFound ?: state.pairsFound,
                gamesCompleted = stats?.totalGames ?: 1
            )
        }
    }

    fun useHint() {
        viewModelScope.launch {
            val hasFree = repository.consumeInventoryBooster("booster_hint")
            val success = if (hasFree) true else repository.consumeHint()
            if (success) {
                audioManager.playHint()
                if (!hasFree) {
                    _uiState.update { it.copy(remainingHints = it.remainingHints - 1) }
                }

                // Find 1 unmatched pair and highlight them
                val unmatched = _uiState.value.cards.filter { !it.isMatched && !it.isFaceUp }
                val pair = unmatched.groupBy { it.pairId }.values.find { it.size >= 2 } ?: return@launch

                val highlightedCards = _uiState.value.cards.map {
                    if (it.id == pair[0].id || it.id == pair[1].id) {
                        it.copy(isFaceUp = true, isHighlighted = true)
                    } else it
                }
                _uiState.update { it.copy(cards = highlightedCards) }

                delay(1600)

                val restoredCards = _uiState.value.cards.map {
                    if (it.id == pair[0].id || it.id == pair[1].id) {
                        it.copy(isFaceUp = false, isHighlighted = false)
                    } else it
                }
                _uiState.update { it.copy(cards = restoredCards) }
            }
        }
    }

    fun useRevealPair() {
        viewModelScope.launch {
            if (_uiState.value.status != GameUiStatus.Playing) return@launch

            // 1) Seleciona um par ainda não encontrado (sem consumir ainda)
            val unmatched = _uiState.value.cards.filter { !it.isMatched }
            val pair = unmatched.groupBy { it.pairId }.values.find { it.size >= 2 }
            if (pair == null) {
                audioManager.playMismatch()
                return@launch
            }

            val hasFree = repository.getInventoryQuantity("booster_reveal") > 0
            if (!hasFree) {
                val playerCoins = repository.playerFlow.firstOrNull()?.coins ?: 0
                if (playerCoins < 150) {
                    audioManager.playMismatch()
                    return@launch
                }
            }

            // 2) Revela o par conforme a mecânica atual
            audioManager.playReveal()
            val updatedCards = _uiState.value.cards.map {
                if (it.id == pair[0].id || it.id == pair[1].id) {
                    it.copy(isFaceUp = true, isMatched = true)
                } else it
            }

            val newPairsFound = _uiState.value.pairsFound + 1
            _uiState.update { state ->
                state.copy(
                    cards = updatedCards,
                    pairsFound = newPairsFound
                )
            }

            // 3) Consome inventário / moedas somente após sucesso da ação
            if (hasFree) {
                repository.consumeInventoryBooster("booster_reveal")
            } else if (!repository.spendCoins(150, "REVEAL_PAIR")) {
                audioManager.playMismatch()
                return@launch
            }

            repository.updateDailyQuestProgress("FIND_PAIRS", 1)

            if (newPairsFound >= _uiState.value.totalPairs) {
                onLevelSuccess()
            }
        }
    }

    fun freezeTimer() {
        if (isFreezingTimer) return
        val currentState = _uiState.value
        if (currentState.status !is GameUiStatus.Previewing && currentState.status != GameUiStatus.Playing) return

        viewModelScope.launch {
            isFreezingTimer = true
            try {
                val hasFree = repository.consumeInventoryBooster("booster_freeze")
                if (hasFree || repository.spendCoins(110, "EXTRA_TIME")) {
                    audioManager.playFreeze()
                    val latestState = _uiState.value
                    if (latestState.status is GameUiStatus.Previewing) {
                        val remaining = (latestState.status as GameUiStatus.Previewing).remainingSeconds
                        val newRemaining = remaining + 10
                        startPreviewCountdown(newRemaining)
                    } else if (latestState.status == GameUiStatus.Playing) {
                        _uiState.update { it.copy(isTimerFrozen = true) }
                        delay(30000) // Freeze timer for 30s during gameplay
                        _uiState.update { it.copy(isTimerFrozen = false) }
                    }
                } else {
                    audioManager.playMismatch()
                }
            } finally {
                isFreezingTimer = false
            }
        }
    }

    fun shuffleBoard() {
        viewModelScope.launch {
            if (repository.spendCoins(80, "SHUFFLE_BOARD")) {
                audioManager.playSfx(SoundEffect.CARD_FLIP)
                val currentCards = _uiState.value.cards
                val unmatchedFaceDownIndices = currentCards.indices.filter { !currentCards[it].isMatched && !currentCards[it].isFaceUp }
                if (unmatchedFaceDownIndices.size > 1) {
                    val shuffledCardsList = currentCards.toMutableList()
                    val cardObjectsToShuffle = unmatchedFaceDownIndices.map { currentCards[it] }.shuffled()
                    unmatchedFaceDownIndices.forEachIndexed { i, targetIdx ->
                        shuffledCardsList[targetIdx] = cardObjectsToShuffle[i]
                    }
                    _uiState.update { it.copy(cards = shuffledCardsList) }
                }
            } else {
                audioManager.playMismatch()
            }
        }
    }

    fun secondChance() {
        viewModelScope.launch {
            if (repository.spendCoins(150, "SECOND_CHANCE")) {
                audioManager.playMatch()
                _uiState.update { state ->
                    state.copy(
                        lives = state.lives + 2,
                        status = GameUiStatus.Playing
                    )
                }
                startTimer()
            } else {
                audioManager.playMismatch()
            }
        }
    }

    fun restartLevel() {
        startLevel(_uiState.value.levelNumber, _uiState.value.theme.id, _uiState.value.frameId)
    }

    fun nextLevel() {
        startLevel(_uiState.value.levelNumber + 1, _uiState.value.theme.id, _uiState.value.frameId)
    }

    fun onAppBackgrounded() {
        val currentState = _uiState.value
        if (currentState.status is GameUiStatus.Previewing) {
            // Cancel preview countdown immediately
            previewJob?.cancel()
            previewJob = null
            // Immediately turn all cards face down and transition to Playing state
            _uiState.update { state ->
                val faceDownCards = state.cards.map { it.copy(isFaceUp = false) }
                state.copy(cards = faceDownCards, status = GameUiStatus.Playing)
            }
            startTimeMillis = System.currentTimeMillis()
            startTimer()
        } else if (currentState.status == GameUiStatus.Playing) {
            // Turn any face-up, unmatched cards (or highlighted hint cards) back face down
            _uiState.update { state ->
                val faceDownCards = state.cards.map { card ->
                    if (!card.isMatched && card.isFaceUp) {
                        card.copy(isFaceUp = false, isHighlighted = false)
                    } else card
                }
                state.copy(cards = faceDownCards)
            }
            firstFlippedIndex = null
            isProcessingFlip = false
        }
    }
}
