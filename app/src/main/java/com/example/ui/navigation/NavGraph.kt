package com.example.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.audio.MusicTrack
import com.example.audio.SoundEffect
import com.example.avatar.data.AvatarRepository
import com.example.avatar.ui.AvatarCropperDialog
import com.example.avatar.ui.SelectAvatarDialog
import com.example.avatar.util.AvatarStorageManager
import com.example.data.local.AppDatabase
import com.example.data.model.UsernameStatus
import com.example.data.repository.LeaderboardRepository
import com.example.data.repository.PendingSyncRepository
import com.example.sync.ConnectivityObserver
import com.example.ui.components.NameEntryDialog
import com.example.ui.screens.achievements.AchievementsScreen
import com.example.ui.screens.game.GameScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.ranking.RankingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.shop.ShopScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.viewmodel.GameUiStatus
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

import android.widget.Toast
import com.example.config.LegalConfig
import com.example.ui.screens.consent.ConsentScreen

fun routeToMusic(route: String?, gameStatus: GameUiStatus? = null): MusicTrack? {
    return when (route) {
        Screen.Home.route -> MusicTrack.HOME
        Screen.Profile.route -> MusicTrack.HOME
        Screen.Shop.route -> MusicTrack.SHOP
        Screen.Ranking.route -> MusicTrack.RANKING
        Screen.Settings.route -> MusicTrack.HOME
        Screen.Stats.route -> MusicTrack.HOME
        Screen.Achievements.route -> MusicTrack.HOME
        Screen.Game.route -> {
            when (gameStatus) {
                is GameUiStatus.LevelCompleted -> MusicTrack.VICTORY
                is GameUiStatus.Defeat -> MusicTrack.DEFEAT
                else -> MusicTrack.GAME
            }
        }
        else -> null
    }
}

@Composable
fun MemoryQuestNavGraph(
    mainViewModel: MainViewModel,
    gameViewModel: GameViewModel,
    navController: NavHostController = rememberNavController()
) {
    val player by mainViewModel.playerState.collectAsStateWithLifecycle()
    val stats by mainViewModel.statsState.collectAsStateWithLifecycle()
    val unlockedThemes by mainViewModel.unlockedThemesState.collectAsStateWithLifecycle()
    val achievements by mainViewModel.achievementsState.collectAsStateWithLifecycle()
    val usernameUiState by mainViewModel.usernameUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val soundEnabled by mainViewModel.soundEnabled.collectAsStateWithLifecycle()
    val musicEnabled by mainViewModel.musicEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by mainViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val musicVolume by mainViewModel.musicVolume.collectAsStateWithLifecycle()
    val sfxVolume by mainViewModel.sfxVolume.collectAsStateWithLifecycle()
    val isAdsRemoved by mainViewModel.isAdsRemoved.collectAsStateWithLifecycle()
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val darkMode by mainViewModel.darkMode.collectAsStateWithLifecycle()

    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val userConsentState by mainViewModel.userConsentState.collectAsStateWithLifecycle()
    val usernameEligibility by mainViewModel.usernameEligibility.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val avatarRepository = remember(context) {
        val db = AppDatabase.getDatabase(context)
        val storageManager = AvatarStorageManager(context)
        val leaderboardRepo = LeaderboardRepository()
        val pendingSyncRepo = PendingSyncRepository(db.pendingSyncDao(), db.memoryQuestDao())
        AvatarRepository(db.memoryQuestDao(), storageManager, leaderboardRepo, pendingSyncRepo)
    }

    var showSelectAvatarDialog by remember { mutableStateOf(false) }
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val requestedTrack = routeToMusic(currentRoute, gameState.status)

    val leaderboardList by mainViewModel.leaderboardList.collectAsStateWithLifecycle()
    val currentUserInLeaderboard = remember(leaderboardList) {
        leaderboardList.find { it.isCurrentUser }
    }
    val rankingDisplay = remember(currentUserInLeaderboard, leaderboardList) {
        when {
            currentUserInLeaderboard != null -> "#${currentUserInLeaderboard.rank}"
            leaderboardList.isNotEmpty() -> "Top 100"
            else -> "--"
        }
    }

    // Centralized Music Track Switcher
    LaunchedEffect(requestedTrack) {
        if (requestedTrack == null) {
            mainViewModel.audioManager.stopMusic()
        } else {
            mainViewModel.audioManager.playMusic(requestedTrack)
        }
    }

    // Mandatory Privacy & Terms Consent Gate
    val isConsentValid = userConsentState?.isValid(LegalConfig.TERMS_VERSION, LegalConfig.PRIVACY_VERSION) == true
    if (userConsentState != null && !isConsentValid) {
        ConsentScreen(
            onAccept = {
                mainViewModel.acceptConsent()
            }
        )
        return
    }

    // Onboarding check: if player != null and name is empty or NOT_SELECTED
    if (player != null && (player?.name.isNullOrEmpty() || player?.usernameStatus == UsernameStatus.NOT_SELECTED.name)) {
        NameEntryDialog(
            title = stringResource(R.string.dialog_welcome_title),
            subtitle = stringResource(R.string.dialog_welcome_subtitle),
            uiState = usernameUiState,
            onNameInputChange = { input, isOnline ->
                mainViewModel.onUsernameInputChanged(input, isOnline)
            },
            onConfirm = { name ->
                val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                mainViewModel.reserveUsername(name, isOnline) { _ -> }
            }
        )
    }

    // Conflict check: if player username status is CONFLICT
    if (player != null && player?.usernameStatus == UsernameStatus.CONFLICT.name) {
        NameEntryDialog(
            initialName = player?.name ?: "",
            title = stringResource(R.string.username_conflict_title),
            subtitle = stringResource(R.string.username_conflict_desc),
            uiState = usernameUiState,
            onNameInputChange = { input, isOnline ->
                mainViewModel.onUsernameInputChanged(input, isOnline)
            },
            onConfirm = { name ->
                val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                mainViewModel.reserveUsername(name, isOnline) { _ -> }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                player = player,
                onPlayClick = {
                    mainViewModel.audioManager.playButton()
                    val currentLvl = player?.currentLevel ?: 1
                    gameViewModel.startLevel(
                        level = currentLvl,
                        themeId = player?.equippedThemeId ?: "animals",
                        frameId = player?.equippedFrameId ?: "frame_classic"
                    )
                    navController.navigate(Screen.Game.route)
                },
                onProfileClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Profile.route)
                },
                onRankingClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Ranking.route)
                },
                onShopClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Shop.route)
                },
                onSettingsClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Settings.route)
                },
                onStatsClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Stats.route)
                },
                onAchievementsClick = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Achievements.route)
                },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                player = player,
                stats = stats,
                achievements = achievements,
                unlockedThemes = unlockedThemes,
                rankingDisplay = rankingDisplay,
                usernameUiState = usernameUiState,
                usernameEligibility = usernameEligibility,
                onCheckEligibility = { callback ->
                    mainViewModel.checkUsernameChangeEligibility(callback)
                },
                onNameInputChange = { input, isOnline ->
                    mainViewModel.onUsernameInputChanged(input, isOnline)
                },
                onReserveUsername = { name, onResult ->
                    val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                    mainViewModel.reserveUsername(name, isOnline, onResult)
                },
                onEditAvatarClick = {
                    mainViewModel.audioManager.playButton()
                    showSelectAvatarDialog = true
                },
                onNavigateToStats = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToShop = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Shop.route)
                },
                onNavigateToAchievements = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Achievements.route)
                },
                onNavigateToRanking = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Ranking.route)
                },
                onBackClick = {
                    mainViewModel.audioManager.playButton()
                    navController.popBackStack()
                },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                state = gameState,
                coins = player?.coins ?: 0,
                player = player,
                onCardClick = { index -> gameViewModel.onCardClick(index) },
                onUseHint = { gameViewModel.useHint() },
                onRevealPair = { gameViewModel.useRevealPair() },
                onFreezeTimer = { gameViewModel.freezeTimer() },
                onNextLevel = {
                    mainViewModel.showNextLevelInterstitial(context.findActivity()) {
                        gameViewModel.nextLevel()
                    }
                },
                onRestartLevel = {
                    gameViewModel.restartLevel()
                },
                onGoToShop = { navController.navigate(Screen.Shop.route) },
                onBackToHome = {
                    mainViewModel.showBackToHomeInterstitial(context.findActivity()) {
                        navController.popBackStack(Screen.Home.route, false)
                    }
                },
                onAppBackgrounded = { gameViewModel.onAppBackgrounded() },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Shop.route) {
            ShopScreen(
                player = player,
                unlockedThemes = unlockedThemes,
                onBuyTheme = { theme -> mainViewModel.buyTheme(theme) },
                onSelectTheme = { themeId -> mainViewModel.selectTheme(themeId) },
                onBuyFrame = { frame -> mainViewModel.buyFrame(frame) },
                onBuyBooster = { id, price -> mainViewModel.buyBooster(id, price) },
                onBackClick = { navController.popBackStack() },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                player = player,
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                vibrationEnabled = vibrationEnabled,
                musicVolume = musicVolume,
                sfxVolume = sfxVolume,
                language = language,
                darkMode = darkMode,
                onSetSound = { mainViewModel.setSoundEnabled(it) },
                onSetMusic = { mainViewModel.setMusicEnabled(it) },
                onSetVibration = { mainViewModel.setVibrationEnabled(it) },
                onSetMusicVolume = { mainViewModel.setMusicVolume(it) },
                onSetSfxVolume = { mainViewModel.setSfxVolume(it) },
                onSetLanguage = { mainViewModel.setLanguage(it) },
                onSetDarkMode = { mainViewModel.setDarkMode(it) },
                onResetDefaults = { mainViewModel.resetSettings() },
                onResetGameProgress = {
                    mainViewModel.audioManager.playButton()
                    mainViewModel.resetGameProgress()
                },
                onBackClick = {
                    mainViewModel.audioManager.playButton()
                    navController.popBackStack()
                },
                isAdsRemoved = isAdsRemoved,
                consentState = userConsentState
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                player = player,
                stats = stats,
                onNavigateToProfile = {
                    mainViewModel.audioManager.playButton()
                    navController.navigate(Screen.Profile.route)
                },
                onBackClick = {
                    mainViewModel.audioManager.playButton()
                    navController.popBackStack()
                },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Ranking.route) {
            val isLeaderboardLoading by mainViewModel.isLeaderboardLoading.collectAsStateWithLifecycle()
            val leaderboardError by mainViewModel.leaderboardError.collectAsStateWithLifecycle()
            val lastFetchTime by mainViewModel.lastLeaderboardFetchTime.collectAsStateWithLifecycle()
            val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                mainViewModel.loadLeaderboard()
            }

            RankingScreen(
                player = player,
                stats = stats,
                leaderboardList = leaderboardList,
                isLoading = isLeaderboardLoading,
                errorMessage = leaderboardError,
                lastFetchTime = lastFetchTime,
                isOnline = isOnline,
                onRefresh = { mainViewModel.loadLeaderboard() },
                onBackClick = { navController.popBackStack() },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(
                player = player,
                achievements = achievements,
                onBackClick = { navController.popBackStack() },
                isAdsRemoved = isAdsRemoved
            )
        }
    }

    if (showSelectAvatarDialog) {
        SelectAvatarDialog(
            player = player,
            onPresetSelected = { presetId ->
                scope.launch { avatarRepository.selectPresetAvatar(presetId) }
                showSelectAvatarDialog = false
            },
            onCustomPhotoSelected = { uri ->
                showSelectAvatarDialog = false
                croppingUri = uri
            },
            onResetDefault = {
                scope.launch { avatarRepository.resetToDefaultAvatar() }
                showSelectAvatarDialog = false
            },
            onDismiss = { showSelectAvatarDialog = false }
        )
    }

    if (croppingUri != null) {
        AvatarCropperDialog(
            imageUri = croppingUri!!,
            onCroppedAndSaved = { localPath ->
                croppingUri = null
                scope.launch { avatarRepository.saveCustomAvatar(localPath) }
            },
            onCancel = { croppingUri = null }
        )
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
