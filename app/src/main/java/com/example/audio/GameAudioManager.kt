package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.data.local.DataStoreManager

enum class SoundEffect {
    CARD_FLIP,
    MATCH_SUCCESS,
    MATCH_ERROR,
    LIFE_LOST,
    COIN_GAIN,
    BUTTON_CLICK,
    LEVEL_COMPLETE,
    GAME_OVER,
    ACHIEVEMENT_UNLOCKED,
    PURCHASE_SUCCESS,
    COUNTDOWN,
    HINT_USED
}

enum class MusicTrack {
    HOME,
    GAME,
    SHOP,
    RANKING,
    VICTORY,
    DEFEAT
}

class GameAudioManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "GameAudioManager"

        @Volatile
        private var INSTANCE: GameAudioManager? = null

        fun getInstance(context: Context): GameAudioManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameAudioManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    val musicManager = MusicManager.getInstance(context)

    // Audio settings
    var isSoundEnabled: Boolean = true
        private set
    var isVibrationEnabled: Boolean = true
        private set
    var sfxVolume: Float = 0.8f
        private set

    // SoundPool for SFX
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private val lastPlayTimes = mutableMapOf<SoundEffect, Long>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSoundEffects()
    }

    /**
     * Binds GameAudioManager and MusicManager to DataStore preferences flow
     */
    fun observeSettings(dataStoreManager: DataStoreManager, scope: CoroutineScope) {
        musicManager.observeSettings(dataStoreManager, scope)
        scope.launch(Dispatchers.Main) {
            launch {
                dataStoreManager.soundEnabled.collectLatest { enabled ->
                    isSoundEnabled = enabled
                }
            }
            launch {
                dataStoreManager.vibrationEnabled.collectLatest { enabled ->
                    isVibrationEnabled = enabled
                }
            }
            launch {
                dataStoreManager.sfxVolume.collectLatest { vol ->
                    sfxVolume = vol
                }
            }
        }
    }

    private fun loadSoundEffects() {
        val soundResNames = mapOf(
            SoundEffect.CARD_FLIP to "card_flip",
            SoundEffect.MATCH_SUCCESS to "match",
            SoundEffect.MATCH_ERROR to "mismatch",
            SoundEffect.LIFE_LOST to "error",
            SoundEffect.COIN_GAIN to "combo",
            SoundEffect.BUTTON_CLICK to "button_click",
            SoundEffect.LEVEL_COMPLETE to "victory",
            SoundEffect.GAME_OVER to "error",
            SoundEffect.ACHIEVEMENT_UNLOCKED to "victory",
            SoundEffect.PURCHASE_SUCCESS to "match",
            SoundEffect.COUNTDOWN to "hint",
            SoundEffect.HINT_USED to "reveal"
        )

        for ((effect, resName) in soundResNames) {
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) {
                try {
                    val soundId = soundPool.load(context, resId, 1)
                    soundMap[effect] = soundId
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao carregar SFX $resName", e)
                }
            } else {
                Log.w(TAG, "Recurso de áudio raw/$resName não encontrado.")
            }
        }
    }

    /**
     * Plays short Sound Effect using SoundPool with rate limiting and optional vibration
     */
    fun playSfx(effect: SoundEffect) {
        if (isVibrationEnabled) {
            vibrateForEffect(effect)
        }

        if (!isSoundEnabled || sfxVolume <= 0f) return

        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[effect] ?: 0L
        if (now - lastTime < 50) return // Throttle duplicate rapid clicks
        lastPlayTimes[effect] = now

        val soundId = soundMap[effect]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f)
        }
    }

    private fun vibrateForEffect(effect: SoundEffect) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val durationMs = when (effect) {
            SoundEffect.CARD_FLIP, SoundEffect.BUTTON_CLICK -> 15L
            SoundEffect.MATCH_SUCCESS, SoundEffect.COIN_GAIN -> 35L
            SoundEffect.MATCH_ERROR, SoundEffect.LIFE_LOST -> 60L
            SoundEffect.LEVEL_COMPLETE, SoundEffect.GAME_OVER, SoundEffect.ACHIEVEMENT_UNLOCKED -> 120L
            else -> 20L
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao acionar vibração", e)
        }
    }

    /**
     * Plays background music for a given MusicTrack without restarting if already playing
     */
    fun playMusic(track: MusicTrack) {
        musicManager.playTrack(track)
    }

    fun pauseMusic() {
        musicManager.pauseMusic()
    }

    fun resumeMusic() {
        musicManager.resumeMusic()
    }

    fun stopMusic() {
        musicManager.stopMusic()
    }

    fun onAppForeground() {
        // Handled automatically via ProcessLifecycleOwner in MusicManager
    }

    fun onAppBackground() {
        // Handled automatically via ProcessLifecycleOwner in MusicManager
    }

    fun release() {
        try {
            soundPool.release()
            musicManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar áudio", e)
        }
    }
}
