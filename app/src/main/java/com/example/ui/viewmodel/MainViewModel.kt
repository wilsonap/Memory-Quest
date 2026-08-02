package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.GameTheme
import com.example.data.model.ShopItem
import com.example.data.repository.GameRepository
import com.example.data.repository.LeaderboardPlayer
import com.example.data.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.model.UsernameStatus
import com.example.data.model.UsernameUiState
import com.example.data.repository.UsernameRepository
import com.example.data.repository.UsernameReservationResult
import com.example.sync.ConnectivityObserver
import com.example.sync.ValidatePendingUsernameWorker
import com.example.util.UsernameNormalizer
import com.example.util.UsernameSuggestionGenerator
import com.example.util.UsernameValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dataStore = DataStoreManager(application)
    val repository = GameRepository(db.memoryQuestDao(), dataStore)
    val leaderboardRepository = LeaderboardRepository()
    val usernameRepository = UsernameRepository(db.memoryQuestDao(), leaderboardRepository)
    val audioManager = GameAudioManager.getInstance(application)

    private val _usernameUiState = MutableStateFlow(UsernameUiState())
    val usernameUiState: StateFlow<UsernameUiState> = _usernameUiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    private val _leaderboardList = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardList: StateFlow<List<LeaderboardPlayer>> = _leaderboardList.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _leaderboardError = MutableStateFlow<String?>(null)
    val leaderboardError: StateFlow<String?> = _leaderboardError.asStateFlow()

    val playerState: StateFlow<PlayerEntity?> = repository.playerFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val statsState: StateFlow<StatisticsEntity?> = repository.statisticsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val unlockedThemesState: StateFlow<List<UnlockedThemeEntity>> = repository.unlockedThemesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val achievementsState: StateFlow<List<AchievementEntity>> = repository.achievementsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val musicEnabled: StateFlow<Boolean> = repository.musicEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val musicVolume: StateFlow<Float> = repository.musicVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.5f
    )

    val sfxVolume: StateFlow<Float> = repository.sfxVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.8f
    )

    init {
        audioManager.observeSettings(dataStore, viewModelScope)

        viewModelScope.launch {
            repository.ensureInitialized()
            leaderboardRepository.ensureAuthenticated()

            val player = repository.playerFlow.firstOrNull()
            if (player?.usernameStatus == UsernameStatus.PENDING_VALIDATION.name) {
                val context = getApplication<Application>()
                val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                if (isOnline) {
                    Log.d("MemoryQuestUsername", "App aberto com status PENDING_VALIDATION e conexão online: executando validação e agendando Worker")
                    usernameRepository.validatePendingUsernameOnline()
                }
                ValidatePendingUsernameWorker.schedule(context)
            }

            syncLeaderboard()
            loadLeaderboard()
        }

        viewModelScope.launch {
            playerState.collect { player ->
                if (player?.usernameStatus == UsernameStatus.PENDING_VALIDATION.name) {
                    val context = getApplication<Application>()
                    ValidatePendingUsernameWorker.schedule(context)
                    val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                    if (isOnline) {
                        usernameRepository.validatePendingUsernameOnline()
                    }
                }
            }
        }
    }

    fun syncLeaderboard() {
        viewModelScope.launch {
            usernameRepository.validatePendingUsernameOnline()
            val player = playerState.value ?: repository.playerFlow.firstOrNull()
            val stats = statsState.value ?: repository.statisticsFlow.firstOrNull()
            if (player != null) {
                leaderboardRepository.syncLeaderboard(player, stats)
            }
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            _leaderboardError.value = null

            usernameRepository.validatePendingUsernameOnline()
            val player = playerState.value ?: repository.playerFlow.firstOrNull()
            val stats = statsState.value ?: repository.statisticsFlow.firstOrNull()
            if (player != null) {
                leaderboardRepository.syncLeaderboard(player, stats)
            }

            val result = leaderboardRepository.fetchTop100Leaderboard()
            result.onSuccess { list ->
                _leaderboardList.value = list
                _isLeaderboardLoading.value = false
            }.onFailure { err ->
                _leaderboardError.value = err.message ?: "Conexão offline. Exibindo dados locais."
                _isLeaderboardLoading.value = false
            }
        }
    }

    val isAdsRemoved: StateFlow<Boolean> = repository.isAdsRemoved.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val vibrationEnabled: StateFlow<Boolean> = repository.vibrationEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val language: StateFlow<String> = repository.language.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "PT"
    )

    val darkMode: StateFlow<String> = repository.darkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "AUTO"
    )

    fun onUsernameInputChanged(newInput: String, isOnline: Boolean) {
        usernameCheckJob?.cancel()
        val validation = UsernameValidator.validate(newInput)
        val normalized = UsernameNormalizer.normalizeUsername(newInput)

        if (validation is UsernameValidator.ValidationResult.Invalid) {
            _usernameUiState.value = UsernameUiState(
                rawInput = newInput,
                normalizedName = normalized,
                validationResult = validation,
                isCheckingAvailability = false,
                isAvailable = false,
                suggestions = emptyList(),
                isOffline = !isOnline
            )
            return
        }

        _usernameUiState.value = UsernameUiState(
            rawInput = newInput,
            normalizedName = normalized,
            validationResult = UsernameValidator.ValidationResult.Valid,
            isCheckingAvailability = isOnline,
            isAvailable = null,
            suggestions = emptyList(),
            isOffline = !isOnline
        )

        if (!isOnline) {
            val suggestions = UsernameSuggestionGenerator.generateSuggestions(newInput)
            _usernameUiState.value = _usernameUiState.value.copy(
                isCheckingAvailability = false,
                suggestions = suggestions
            )
            return
        }

        usernameCheckJob = viewModelScope.launch {
            delay(500)
            val currentUid = leaderboardRepository.ensureAuthenticated()
            val available = usernameRepository.checkAvailabilityOnline(normalized, currentUid)

            var suggestions = emptyList<String>()
            if (!available) {
                suggestions = usernameRepository.getAvailableSuggestionsOnline(newInput, currentUid)
            }

            _usernameUiState.value = _usernameUiState.value.copy(
                isCheckingAvailability = false,
                isAvailable = available,
                suggestions = suggestions
            )
        }
    }

    fun reserveUsername(displayName: String, isOnline: Boolean, onResult: (UsernameReservationResult) -> Unit) {
        viewModelScope.launch {
            val result = usernameRepository.reserveUsername(displayName, isOnline)
            if (result is UsernameReservationResult.PendingOffline) {
                ValidatePendingUsernameWorker.schedule(getApplication())
            }
            onResult(result)
            if (result is UsernameReservationResult.Success || result is UsernameReservationResult.PendingOffline) {
                loadLeaderboard()
            }
        }
    }

    fun setPlayerName(name: String) {
        viewModelScope.launch {
            repository.setPlayerName(name)
            syncLeaderboard()
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            repository.updateEquippedTheme(themeId)
            audioManager.playSfx(SoundEffect.BUTTON_CLICK)
        }
    }

    fun buyTheme(theme: GameTheme) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= theme.priceCoins) {
                repository.unlockTheme(theme.id, theme.priceCoins)
                audioManager.playSfx(SoundEffect.PURCHASE_SUCCESS)
            } else {
                audioManager.playSfx(SoundEffect.MATCH_ERROR)
            }
        }
    }

    fun buyFrame(frameItem: ShopItem) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= frameItem.price) {
                repository.addCoins(-frameItem.price)
                repository.updateEquippedFrame(frameItem.id)
                audioManager.playSfx(SoundEffect.PURCHASE_SUCCESS)
            } else {
                audioManager.playSfx(SoundEffect.MATCH_ERROR)
            }
        }
    }

    fun buyBooster(boosterId: String, price: Int) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= price) {
                repository.addCoins(-price)
                when (boosterId) {
                    "booster_life" -> repository.addExtraLives(1)
                    "booster_hint" -> repository.addHints(3)
                    else -> {}
                }
                audioManager.playSfx(SoundEffect.PURCHASE_SUCCESS)
            } else {
                audioManager.playSfx(SoundEffect.MATCH_ERROR)
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setMusicEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setMusicVolume(vol: Float) {
        viewModelScope.launch { repository.setMusicVolume(vol) }
    }

    fun setSfxVolume(vol: Float) {
        viewModelScope.launch { repository.setSfxVolume(vol) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { repository.setLanguage(lang) }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { repository.setDarkMode(mode) }
    }

    fun resetSettings() {
        viewModelScope.launch { repository.resetDataStore() }
    }
}
