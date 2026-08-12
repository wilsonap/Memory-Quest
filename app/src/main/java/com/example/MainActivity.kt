package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.GameAudioManager
import com.example.sync.ConnectivityObserver
import com.example.sync.SyncManager
import com.google.android.gms.ads.MobileAds
import com.example.ui.navigation.MemoryQuestNavGraph
import com.example.ui.theme.MemoryQuestTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MainViewModel

import com.example.config.AdMobManager
import com.example.config.FirebaseBootstrap
import com.example.ui.screens.config.FirebaseConfigErrorScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import com.example.update.InAppUpdateManager
import com.example.ui.components.InAppUpdateBanner

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("MemoryQuestAudio", "MainActivity criada")
        enableEdgeToEdge()

        // Inicializa o gerenciador de atualizações In-App oficial da Google Play
        try {
            inAppUpdateManager = InAppUpdateManager.getInstance(applicationContext)
            inAppUpdateManager.checkForUpdate(this)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Erro ao inicializar InAppUpdateManager: ${e.message}")
        }

        if (!FirebaseBootstrap.isReady) {
            try {
                val app = com.google.firebase.FirebaseApp.initializeApp(this)
                    ?: com.google.firebase.FirebaseApp.getApps(this).firstOrNull()
                if (app != null) {
                    FirebaseBootstrap.markReady()
                }
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "Firebase fallback initialization: ${e.message}")
            }
        }

        // Garante criação dos ViewModels na thread principal
        try {
            mainViewModel
            gameViewModel
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Erro ao acessar ViewModels: ${e.message}", e)
        }

        // Initialize AdMob & check Google UMP consent safely
        try {
            AdMobManager.requestConsentAndInitialize(this)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Erro ao inicializar AdMobManager: ${e.message}")
        }

        // Initialize offline sync connectivity observer safely
        try {
            connectivityObserver = ConnectivityObserver(applicationContext) {
                try {
                    SyncManager.triggerImmediateSync(applicationContext)
                    com.example.sync.EnsureLeaderboardWorker.schedule(applicationContext)
                    mainViewModel.syncLeaderboard()
                } catch (e: Throwable) {
                    android.util.Log.w("MainActivity", "Erro no callback de conectividade: ${e.message}")
                }
            }
            connectivityObserver.startListening()
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Erro ao iniciar ConnectivityObserver: ${e.message}")
        }

        setContent {
            val language by mainViewModel.language.collectAsStateWithLifecycle()
            val isUpdateDownloaded by if (::inAppUpdateManager.isInitialized) {
                inAppUpdateManager.isUpdateDownloaded.collectAsStateWithLifecycle()
            } else {
                androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            }

            LaunchedEffect(language) {
                applyAppLanguage(language)
            }

            MemoryQuestTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MemoryQuestNavGraph(
                            mainViewModel = mainViewModel,
                            gameViewModel = gameViewModel
                        )
                    }

                    InAppUpdateBanner(
                        isDownloaded = isUpdateDownloaded,
                        onRestartAndInstallClick = {
                            if (::inAppUpdateManager.isInitialized) {
                                inAppUpdateManager.completeUpdate()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onResume(this)
        }
    }

    private fun applyAppLanguage(languageCode: String) {
        val targetLocaleTag = when (languageCode.uppercase()) {
            "EN" -> "en"
            "PT", "PT-BR" -> "pt-BR"
            "SYSTEM" -> ""
            else -> "pt-BR"
        }

        val currentAppLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = if (currentAppLocales.isEmpty) "" else currentAppLocales.toLanguageTags()

        if (targetLocaleTag.isEmpty()) {
            if (!currentAppLocales.isEmpty) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        } else {
            if (currentTag != targetLocaleTag && !currentTag.startsWith(targetLocaleTag)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLocaleTag))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        GameAudioManager.getInstance(this).onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        GameAudioManager.getInstance(this).onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.unregisterListener()
        }
        if (::connectivityObserver.isInitialized) {
            connectivityObserver.stopListening()
        }
    }
}

