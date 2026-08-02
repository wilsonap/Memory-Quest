package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.model.GameCard
import com.example.data.model.GameTheme
import com.example.data.model.LevelConfig
import com.example.data.repository.GameRepository
import com.example.data.repository.LeaderboardRepository
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
        val levelCompletedNumber: Int
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
    val theme: GameTheme = GameTheme.ANIMALS,
    val frameId: String = "frame_classic",
    val totalFlips: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dataStore = DataStoreManager(application)
    val repository = GameRepository(db.memoryQuestDao(), dataStore)
    private val audioManager = GameAudioManager.getInstance(application)

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var firstFlippedIndex: Int? = null
    private var isProcessingFlip = false
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

            val startingLives = levelConfig.initialLives + (player?.extraLives ?: 0).coerceAtMost(2)
            val availableHints = player?.remainingHints ?: 3

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
        audioManager.playSfx(SoundEffect.CARD_FLIP)

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
                    audioManager.playSfx(SoundEffect.MATCH_SUCCESS)
                    audioManager.playSfx(SoundEffect.COIN_GAIN)
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

                    // Check for level completion
                    if (newPairsFound >= _uiState.value.totalPairs) {
                        onLevelSuccess()
                    }
                } else {
                    // MISMATCH!
                    audioManager.playSfx(SoundEffect.MATCH_ERROR)
                    audioManager.playSfx(SoundEffect.LIFE_LOST)
                    delay(800)
                    val newLives = _uiState.value.lives - 1
                    val newErrors = _uiState.value.errorsCount + 1

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
        audioManager.playSfx(SoundEffect.LEVEL_COMPLETE)
        viewModelScope.launch {
            val state = _uiState.value
            val flawless = state.errorsCount == 0
            val flawlessBonus = if (flawless) 50 else 0
            val levelWinBonus = 100
            val comboBonus = state.maxCombo * 10
            val totalCoinsEarned = state.coinsEarnedInGame + levelWinBonus + flawlessBonus + comboBonus

            _uiState.update {
                it.copy(
                    status = GameUiStatus.LevelCompleted(
                        coinsEarned = totalCoinsEarned,
                        flawlessBonus = flawlessBonus,
                        comboBonus = comboBonus,
                        timeSeconds = state.elapsedTimeSeconds,
                        levelCompletedNumber = state.levelNumber
                    )
                )
            }

            // Persist to Room!
            repository.updateStatsAfterGame(
                won = true,
                pairsFoundInGame = state.pairsFound,
                gameDurationSeconds = state.elapsedTimeSeconds,
                flawless = flawless,
                coinsEarnedInGame = totalCoinsEarned,
                maxStreakInGame = state.maxCombo,
                totalFlipsInGame = state.totalFlips
            )

            // Queue pending sync record in Room & trigger WorkManager
            com.example.sync.GameSyncUseCase.create(getApplication()).syncAfterGame()
        }
    }

    private fun onLevelDefeat() {
        timerJob?.cancel()
        audioManager.playSfx(SoundEffect.GAME_OVER)
        viewModelScope.launch {
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
            com.example.sync.GameSyncUseCase.create(getApplication()).syncAfterGame()
        }
    }

    fun useHint() {
        viewModelScope.launch {
            val success = repository.consumeHint()
            if (success) {
                audioManager.playSfx(SoundEffect.HINT_USED)
                _uiState.update { it.copy(remainingHints = it.remainingHints - 1) }

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
            val player = repository.playerFlow.firstOrNull() ?: return@launch
            if (player.coins >= 150) {
                repository.addCoins(-150)
                audioManager.playSfx(SoundEffect.HINT_USED)
                val unmatched = _uiState.value.cards.filter { !it.isMatched }
                val pair = unmatched.groupBy { it.pairId }.values.find { it.size >= 2 } ?: return@launch

                val updatedCards = _uiState.value.cards.map {
                    if (it.id == pair[0].id || it.id == pair[1].id) {
                        it.copy(isFaceUp = true, isMatched = true)
                    } else it
                }

                val newPairsFound = _uiState.value.pairsFound + 1
                _uiState.update { state ->
                    state.copy(
                        cards = updatedCards,
                        pairsFound = newPairsFound,
                        coinsEarnedInGame = state.coinsEarnedInGame + 10
                    )
                }

                if (newPairsFound >= _uiState.value.totalPairs) {
                    onLevelSuccess()
                }
            }
        }
    }

    fun freezeTimer() {
        viewModelScope.launch {
            val player = repository.playerFlow.firstOrNull() ?: return@launch
            if (player.coins >= 110) {
                repository.addCoins(-110)
                audioManager.playSfx(SoundEffect.HINT_USED)
                _uiState.update { it.copy(isTimerFrozen = true) }
                delay(10000)
                _uiState.update { it.copy(isTimerFrozen = false) }
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
