package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.NameEntryDialog
import com.example.ui.screens.achievements.AchievementsScreen
import com.example.ui.screens.game.GameScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.ranking.RankingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.shop.ShopScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MainViewModel

import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.audio.GameAudioManager
import com.example.audio.MusicTrack
import com.example.audio.SoundEffect
import com.example.ui.viewmodel.GameUiStatus

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.UsernameStatus
import com.example.sync.ConnectivityObserver

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Centralized Music Track Switcher
    LaunchedEffect(currentRoute, gameState.status) {
        val targetTrack = when (currentRoute) {
            Screen.Home.route, Screen.Stats.route, Screen.Achievements.route -> MusicTrack.HOME
            Screen.Shop.route -> MusicTrack.SHOP
            Screen.Ranking.route -> MusicTrack.RANKING
            Screen.Settings.route -> MusicTrack.HOME
            Screen.Game.route -> {
                when (gameState.status) {
                    is GameUiStatus.LevelCompleted -> MusicTrack.VICTORY
                    is GameUiStatus.Defeat -> MusicTrack.DEFEAT
                    else -> MusicTrack.GAME
                }
            }
            else -> MusicTrack.HOME
        }
        mainViewModel.audioManager.playMusic(targetTrack)
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
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    val currentLvl = player?.currentLevel ?: 1
                    gameViewModel.startLevel(
                        level = currentLvl,
                        themeId = player?.equippedThemeId ?: "animals",
                        frameId = player?.equippedFrameId ?: "frame_classic"
                    )
                    navController.navigate(Screen.Game.route)
                },
                onRankingClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Ranking.route)
                },
                onShopClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Shop.route)
                },
                onSettingsClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Settings.route)
                },
                onStatsClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Stats.route)
                },
                onAchievementsClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Achievements.route)
                },
                onEditNameClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Settings.route)
                },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                state = gameState,
                coins = player?.coins ?: 0,
                onCardClick = { index -> gameViewModel.onCardClick(index) },
                onUseHint = { gameViewModel.useHint() },
                onRevealPair = { gameViewModel.useRevealPair() },
                onFreezeTimer = { gameViewModel.freezeTimer() },
                onNextLevel = { gameViewModel.nextLevel() },
                onRestartLevel = { gameViewModel.restartLevel() },
                onGoToShop = { navController.navigate(Screen.Shop.route) },
                onBackToHome = { navController.popBackStack(Screen.Home.route, false) },
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
                usernameUiState = usernameUiState,
                onNameInputChange = { input, isOnline ->
                    mainViewModel.onUsernameInputChanged(input, isOnline)
                },
                onReserveUsername = { name, onResult ->
                    val isOnline = ConnectivityObserver(context) {}.isNetworkAvailable()
                    mainViewModel.reserveUsername(name, isOnline, onResult)
                },
                onSetSound = { mainViewModel.setSoundEnabled(it) },
                onSetMusic = { mainViewModel.setMusicEnabled(it) },
                onSetVibration = { mainViewModel.setVibrationEnabled(it) },
                onSetMusicVolume = { mainViewModel.setMusicVolume(it) },
                onSetSfxVolume = { mainViewModel.setSfxVolume(it) },
                onSetLanguage = { mainViewModel.setLanguage(it) },
                onSetDarkMode = { mainViewModel.setDarkMode(it) },
                onEditName = { name -> mainViewModel.setPlayerName(name) },
                onResetDefaults = { mainViewModel.resetSettings() },
                onGoToRanking = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.navigate(Screen.Ranking.route)
                },
                onBackClick = {
                    mainViewModel.audioManager.playSfx(SoundEffect.BUTTON_CLICK)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                player = player,
                stats = stats,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Ranking.route) {
            val leaderboardList by mainViewModel.leaderboardList.collectAsStateWithLifecycle()
            val isLeaderboardLoading by mainViewModel.isLeaderboardLoading.collectAsStateWithLifecycle()
            val leaderboardError by mainViewModel.leaderboardError.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                mainViewModel.loadLeaderboard()
            }

            RankingScreen(
                player = player,
                stats = stats,
                leaderboardList = leaderboardList,
                isLoading = isLeaderboardLoading,
                errorMessage = leaderboardError,
                onRefresh = { mainViewModel.loadLeaderboard() },
                onBackClick = { navController.popBackStack() },
                isAdsRemoved = isAdsRemoved
            )
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(
                player = player,
                achievements = achievements,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
