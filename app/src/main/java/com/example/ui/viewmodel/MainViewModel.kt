package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import com.example.config.AdMobConfig
import com.example.config.InterstitialManager
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
import com.example.data.repository.UsernameChangeEligibility
import com.example.data.repository.UsernameRepository
import com.example.data.repository.UsernameReservationResult
import com.example.data.repository.UsernameSettingsRepository
import com.example.sync.ConnectivityObserver
import com.example.sync.EnsureLeaderboardWorker
import com.example.sync.ValidatePendingUsernameWorker
import com.example.util.UsernameNormalizer
import com.example.util.UsernameSuggestionGenerator
import com.example.util.UsernameValidator
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

import com.example.config.LegalConfig
import com.example.data.model.UserConsentState
import com.example.data.repository.ConsentRepository
import com.example.sync.ConsentSyncWorker

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dataStore = DataStoreManager(application)
    val repository = GameRepository(db.memoryQuestDao(), dataStore)
    val leaderboardRepository = LeaderboardRepository()
    val usernameRepository = UsernameRepository(db.memoryQuestDao(), leaderboardRepository)
    val usernameSettingsRepository = UsernameSettingsRepository(dataStore)
    val consentRepository = ConsentRepository(dataStore)
    val audioManager = GameAudioManager.getInstance(application)
    val interstitialManager = InterstitialManager(application)

    init {
        interstitialManager.loadAd()
    }

    fun showInterstitialAd(activity: Activity?, onAdDismissed: () -> Unit) {
        if (isAdsRemoved.value || !AdMobConfig.ADS_ENABLED) {
            onAdDismissed()
            return
        }
        if (activity != null) {
            interstitialManager.show(activity, onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    private val _usernameEligibility = MutableStateFlow<UsernameChangeEligibility>(UsernameChangeEligibility.Allowed)
    val usernameEligibility: StateFlow<UsernameChangeEligibility> = _usernameEligibility.asStateFlow()

    val userConsentState: StateFlow<UserConsentState?> = consentRepository.userConsentState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _usernameUiState = MutableStateFlow(UsernameUiState())
    val usernameUiState: StateFlow<UsernameUiState> = _usernameUiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    private val _leaderboardList = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardList: StateFlow<List<LeaderboardPlayer>> = _leaderboardList.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _leaderboardError = MutableStateFlow<String?>(null)
    val leaderboardError: StateFlow<String?> = _leaderboardError.asStateFlow()

    private val _lastLeaderboardFetchTime = MutableStateFlow<Long>(0L)
    val lastLeaderboardFetchTime: StateFlow<Long> = _lastLeaderboardFetchTime.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var connectivityObserver: ConnectivityObserver? = null
    private var wasOffline = false
    private var leaderboardFetchJob: Job? = null

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

        val context = getApplication<Application>()
        val initialOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
        _isOnline.value = initialOnline

        connectivityObserver = ConnectivityObserver(context) {
            viewModelScope.launch {
                val currentlyOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                _isOnline.value = currentlyOnline
                Log.d("MemoryQuestRanking", "Estado da conexão: isOnline=$currentlyOnline")

                if (currentlyOnline && wasOffline) {
                    wasOffline = false
                    Log.d("MemoryQuestRanking", "Conexão reestabelecida. Executando apenas uma atualização do ranking.")
                    loadLeaderboard()
                }
            }
        }
        connectivityObserver?.startListening()

        viewModelScope.launch {
            repository.ensureInitialized()
            leaderboardRepository.ensureAuthenticated()

            val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()

            if (isOnline) {
                // Tenta restaurar dados do usuário do Firestore caso o banco local tenha sido resetado
                leaderboardRepository.restoreUserDataFromFirestoreIfAvailable(db.memoryQuestDao())
            }

            val player = repository.playerFlow.firstOrNull()
            val stats = repository.statisticsFlow.firstOrNull()

            Log.d("MemoryQuestLeaderboardEnsure", "MainViewModel init: Estado da conexão online=$isOnline")

            if (player != null && player.usernameStatus == UsernameStatus.CONFIRMED.name) {
                if (isOnline) {
                    val result = leaderboardRepository.ensureLeaderboardExists(player, stats)
                    if (result.isFailure) {
                        Log.w("MemoryQuestLeaderboardEnsure", "Falha ao verificar leaderboard na abertura online. Agendando EnsureLeaderboardWorker.")
                        EnsureLeaderboardWorker.schedule(context)
                    }
                } else {
                    Log.d("MemoryQuestLeaderboardEnsure", "App aberto offline. Agendando EnsureLeaderboardWorker.")
                    EnsureLeaderboardWorker.schedule(context)
                }
            }

            if (player?.usernameStatus == UsernameStatus.PENDING_VALIDATION.name) {
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
        val context = getApplication<Application>()
        val online = ConnectivityObserver(context) {}.isNetworkAvailable()
        if (!online) {
            Log.d("MemoryQuestRanking", "Dispositivo offline. Pulo na sincronização remota do ranking.")
            return
        }
        viewModelScope.launch {
            usernameRepository.validatePendingUsernameOnline()
            val player = playerState.value ?: repository.playerFlow.firstOrNull()
            val stats = statsState.value ?: repository.statisticsFlow.firstOrNull()
            if (player != null) {
                if (player.usernameStatus == UsernameStatus.CONFIRMED.name) {
                    leaderboardRepository.ensureLeaderboardExists(player, stats)
                }
                leaderboardRepository.syncLeaderboard(player, stats)
            }
        }
    }

    fun loadLeaderboard() {
        val context = getApplication<Application>()
        val online = ConnectivityObserver(context) {}.isNetworkAvailable()
        _isOnline.value = online

        Log.d("MemoryQuestRanking", "Estado da conexão: isOnline=$online")

        if (!online) {
            wasOffline = true
            _isLeaderboardLoading.value = false
            _leaderboardError.value = "Sem conexão com a internet"

            Log.d("MemoryQuestRanking", "Início da consulta do ranking")
            Log.d("MemoryQuestRanking", "Origem: CACHE")

            leaderboardFetchJob?.cancel()

            viewModelScope.launch {
                try {
                    val cacheResult = leaderboardRepository.fetchTop100Leaderboard(Source.CACHE)
                    cacheResult.onSuccess { cachedList ->
                        if (cachedList.isNotEmpty()) {
                            _leaderboardList.value = cachedList
                            Log.d("MemoryQuestRanking", "Sucesso: ${cachedList.size} jogadores carregados do CACHE")
                        } else {
                            Log.d("MemoryQuestRanking", "CACHE local vazio. Mantendo ${_leaderboardList.value.size} itens em memória")
                        }
                    }.onFailure { err ->
                        Log.e("MemoryQuestRanking", "Erro ao buscar CACHE local: ${err.message}")
                    }
                } catch (e: Exception) {
                    Log.e("MemoryQuestRanking", "Exceção ao buscar CACHE: ${e.message}")
                } finally {
                    _isLeaderboardLoading.value = false
                    Log.d("MemoryQuestRanking", "Loading encerrado")
                }
            }
            return
        }

        if (leaderboardFetchJob?.isActive == true) {
            Log.d("MemoryQuestRanking", "Cancelando consulta anterior ainda em andamento...")
            leaderboardFetchJob?.cancel()
        }

        leaderboardFetchJob = viewModelScope.launch {
            _isLeaderboardLoading.value = true
            _leaderboardError.value = null

            Log.d("MemoryQuestRanking", "Início da consulta do ranking")
            Log.d("MemoryQuestRanking", "Origem: SERVER")

            try {
                val result = withTimeoutOrNull(8000L) {
                    val player = playerState.value ?: repository.playerFlow.firstOrNull()
                    val stats = statsState.value ?: repository.statisticsFlow.firstOrNull()
                    if (player != null && player.usernameStatus == UsernameStatus.CONFIRMED.name) {
                        try {
                            leaderboardRepository.syncLeaderboard(player, stats)
                        } catch (e: Exception) {
                            Log.w("MemoryQuestRanking", "Aviso na sincronização: ${e.message}")
                        }
                    }
                    leaderboardRepository.fetchTop100Leaderboard(Source.SERVER)
                }

                if (result == null) {
                    Log.w("MemoryQuestRanking", "Timeout atingido (max 8s)")
                    _leaderboardError.value = "Tempo limite excedido. Exibindo dados locais."

                    try {
                        Log.d("MemoryQuestRanking", "Início da consulta do ranking")
                        Log.d("MemoryQuestRanking", "Origem: CACHE")
                        val cacheResult = leaderboardRepository.fetchTop100Leaderboard(Source.CACHE)
                        cacheResult.onSuccess { cachedList ->
                            if (cachedList.isNotEmpty()) {
                                _leaderboardList.value = cachedList
                                Log.d("MemoryQuestRanking", "Sucesso: ${cachedList.size} jogadores do CACHE pós-timeout")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MemoryQuestRanking", "Erro no fallback de CACHE: ${e.message}")
                    }
                } else {
                    result.onSuccess { list ->
                        _leaderboardList.value = list
                        _lastLeaderboardFetchTime.value = System.currentTimeMillis()
                        _leaderboardError.value = null
                        Log.d("MemoryQuestRanking", "Sucesso: ${list.size} jogadores carregados do SERVER")
                    }.onFailure { err ->
                        Log.e("MemoryQuestRanking", "Erro na consulta SERVER: ${err.message}")
                        _leaderboardError.value = err.message ?: "Erro ao conectar ao servidor"

                        try {
                            Log.d("MemoryQuestRanking", "Início da consulta do ranking")
                            Log.d("MemoryQuestRanking", "Origem: CACHE")
                            val cacheResult = leaderboardRepository.fetchTop100Leaderboard(Source.CACHE)
                            cacheResult.onSuccess { cachedList ->
                                if (cachedList.isNotEmpty()) {
                                    _leaderboardList.value = cachedList
                                    Log.d("MemoryQuestRanking", "Sucesso: ${cachedList.size} jogadores do CACHE pós-erro")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MemoryQuestRanking", "Erro no fallback de CACHE: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MemoryQuestRanking", "Erro na corrotina: ${e.message}")
                _leaderboardError.value = e.message ?: "Erro inesperado"
            } finally {
                _isLeaderboardLoading.value = false
                Log.d("MemoryQuestRanking", "Loading encerrado")
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

    fun checkUsernameChangeEligibility(onResult: (UsernameChangeEligibility) -> Unit = {}) {
        viewModelScope.launch {
            val uid = leaderboardRepository.ensureAuthenticated() ?: ""
            val online = isOnline.value
            val result = usernameSettingsRepository.checkUsernameChangeEligibility(uid, online)
            _usernameEligibility.value = result
            onResult(result)
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
                checkUsernameChangeEligibility()
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
            audioManager.playButton()
        }
    }

    fun buyTheme(theme: GameTheme) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= theme.priceCoins) {
                repository.unlockTheme(theme.id, theme.priceCoins)
                audioManager.playCoin()
            } else {
                audioManager.playMismatch()
            }
        }
    }

    fun buyFrame(frameItem: ShopItem) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= frameItem.price) {
                repository.addCoins(-frameItem.price)
                repository.updateEquippedFrame(frameItem.id)
                audioManager.playCoin()
            } else {
                audioManager.playMismatch()
            }
        }
    }

    fun buyBooster(boosterId: String, price: Int) {
        viewModelScope.launch {
            val player = playerState.value ?: return@launch
            if (player.coins >= price) {
                repository.addCoins(-price)
                when (boosterId) {
                    "booster_life" -> repository.addExtraLives(3)
                    "booster_hint" -> repository.addHints(3)
                    else -> {}
                }
                audioManager.playCoin()
            } else {
                audioManager.playMismatch()
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

    fun acceptConsent(onResult: (syncedOnline: Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val syncedOnline = consentRepository.recordConsent()
            if (!syncedOnline) {
                ConsentSyncWorker.schedule(getApplication())
            }
            onResult(syncedOnline)
        }
    }

    fun resetGameProgress() {
        viewModelScope.launch { repository.resetGameProgress() }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityObserver?.stopListening()
    }
}
