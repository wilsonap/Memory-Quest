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

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!FirebaseBootstrap.isReady) {
            setContent {
                MemoryQuestTheme {
                    FirebaseConfigErrorScreen()
                }
            }
            return
        }

        // Garante criação dos ViewModels na thread principal antes do callback de rede
        mainViewModel
        gameViewModel

        // Initialize AdMob via centralized AdMobManager
        AdMobManager.initialize(this)

        // Initialize offline sync connectivity observer
        connectivityObserver = ConnectivityObserver(applicationContext) {
            SyncManager.triggerImmediateSync(applicationContext)
            com.example.sync.EnsureLeaderboardWorker.schedule(applicationContext)
            mainViewModel.syncLeaderboard()
        }
        connectivityObserver.startListening()

        setContent {
            val darkMode by mainViewModel.darkMode.collectAsStateWithLifecycle()
            val language by mainViewModel.language.collectAsStateWithLifecycle()

            LaunchedEffect(language) {
                applyAppLanguage(language)
            }

            MemoryQuestTheme(darkMode = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemoryQuestNavGraph(
                        mainViewModel = mainViewModel,
                        gameViewModel = gameViewModel
                    )
                }
            }
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
        if (::connectivityObserver.isInitialized) {
            connectivityObserver.stopListening()
        }
        GameAudioManager.getInstance(this).release()
    }
}

