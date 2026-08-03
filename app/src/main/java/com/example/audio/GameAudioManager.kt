package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SoundEffect(val resName: String, val cooldownMs: Long = 80L) {
    CARD_FLIP("card_flip", 60L),
    MATCH_SUCCESS("match", 120L),
    MATCH_ERROR("match_error", 120L),
    VICTORY("victory", 300L),
    COIN_GAIN("coin", 100L),
    BUTTON_CLICK("button", 60L),
    ACHIEVEMENT_UNLOCKED("achievement", 300L),
    LEVEL_UP("level_up", 300L),
    HINT_USED("hint", 150L),
    REVEAL("reveal", 150L),
    FREEZE("freeze", 150L),
    
    // Legacy aliases for backward compatibility
    LIFE_LOST("match_error", 120L),
    GAME_OVER("game_over", 300L),
    PURCHASE_SUCCESS("coin", 100L),
    COUNTDOWN("button", 60L)
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
        private const val TAG = "MemoryQuestSFX"

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
    private val loadedSoundIds = mutableSetOf<Int>()
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

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
                Log.d(TAG, "onLoadComplete SUCCESS: soundId=$sampleId, status=$status")
            } else {
                Log.e(TAG, "onLoadComplete FAILED: soundId=$sampleId, status=$status")
            }
        }

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
                    Log.d(TAG, "Setting updated - soundEnabled: $isSoundEnabled")
                }
            }
            launch {
                dataStoreManager.vibrationEnabled.collectLatest { enabled ->
                    isVibrationEnabled = enabled
                    Log.d(TAG, "Setting updated - vibrationEnabled: $isVibrationEnabled")
                }
            }
            launch {
                dataStoreManager.sfxVolume.collectLatest { vol ->
                    sfxVolume = vol.coerceIn(0f, 1f)
                    Log.d(TAG, "Setting updated - sfxVolume: $sfxVolume")
                }
            }
        }
    }

    private fun loadSoundEffects() {
        val fallbacks = mapOf(
            "match_error" to listOf("mismatch", "match", "button"),
            "mismatch" to listOf("match_error", "match", "button"),
            "game_over" to listOf("match_error", "mismatch", "button"),
            "victory" to listOf("level_up", "achievement"),
            "reveal" to listOf("hint", "button")
        )

        for (effect in SoundEffect.values()) {
            var resId = context.resources.getIdentifier(effect.resName, "raw", context.packageName)
            var actualResName = effect.resName
            if (resId == 0 && fallbacks.containsKey(effect.resName)) {
                for (fallback in fallbacks[effect.resName]!!) {
                    resId = context.resources.getIdentifier(fallback, "raw", context.packageName)
                    if (resId != 0) {
                        actualResName = fallback
                        break
                    }
                }
            }

            if (resId != 0) {
                try {
                    val soundId = soundPool.load(context, resId, 1)
                    soundMap[effect] = soundId
                    Log.d(TAG, "loadSoundEffects: effect=${effect.name}, resName=$actualResName, resId=$resId -> soundId=$soundId")
                } catch (e: Exception) {
                    Log.w(TAG, "loadSoundEffects: Falha ao carregar SFX $actualResName", e)
                }
            } else {
                Log.w(TAG, "loadSoundEffects: Recurso raw/${effect.resName} NAO ENCONTRADO em res/raw")
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

        if (!isSoundEnabled || sfxVolume <= 0f) {
            Log.d(TAG, "playSfx SKIPPED: effect=${effect.name}, isSoundEnabled=$isSoundEnabled, sfxVolume=$sfxVolume")
            return
        }

        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[effect] ?: 0L
        if (now - lastTime < effect.cooldownMs) {
            Log.d(TAG, "playSfx THROTTLED: effect=${effect.name}")
            return
        }
        lastPlayTimes[effect] = now

        val soundId = soundMap[effect]
        if (soundId == null || soundId == 0) {
            Log.w(TAG, "playSfx FAILED: soundId nao encontrado para ${effect.name} (resName=${effect.resName})")
            return
        }

        if (!loadedSoundIds.contains(soundId)) {
            Log.w(TAG, "playSfx REJECTED: soundId=$soundId para ${effect.name} ainda nao concluiu onLoadComplete!")
            return
        }

        val streamId = soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f)
        Log.d(TAG, "playSfx PLAYING: effect=${effect.name}, resName=${effect.resName}.ogg, soundId=$soundId, volume=$sfxVolume, streamId=$streamId")
    }

    // Specific spec methods
    fun playButton() = playSfx(SoundEffect.BUTTON_CLICK)
    fun playCardFlip() = playSfx(SoundEffect.CARD_FLIP)
    fun playMatch() = playSfx(SoundEffect.MATCH_SUCCESS)
    fun playMismatch() = playSfx(SoundEffect.MATCH_ERROR)
    fun playVictory() = playSfx(SoundEffect.VICTORY)
    fun playCoin() = playSfx(SoundEffect.COIN_GAIN)
    fun playAchievement() = playSfx(SoundEffect.ACHIEVEMENT_UNLOCKED)
    fun playLevelUp() = playSfx(SoundEffect.LEVEL_UP)
    fun playHint() = playSfx(SoundEffect.HINT_USED)
    fun playReveal() = playSfx(SoundEffect.REVEAL)
    fun playFreeze() = playSfx(SoundEffect.FREEZE)

    /**
     * Test sequence for settings screen button
     */
    suspend fun testEffectsSequence() {
        Log.d(TAG, "--- INICIANDO TESTE EM SEQUENCIA DE EFEITOS SONOROS ---")
        Log.d(TAG, "Testando: button.ogg")
        playButton()
        delay(400)
        Log.d(TAG, "Testando: card_flip.ogg")
        playCardFlip()
        delay(400)
        Log.d(TAG, "Testando: match.ogg")
        playMatch()
        delay(400)
        Log.d(TAG, "Testando: mismatch.ogg")
        playMismatch()
        delay(400)
        Log.d(TAG, "Testando: coin.ogg")
        playCoin()
        Log.d(TAG, "--- FIM DO TESTE EM SEQUENCIA ---")
    }

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

    private fun vibrateForEffect(effect: SoundEffect) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val durationMs = when (effect) {
            SoundEffect.CARD_FLIP, SoundEffect.BUTTON_CLICK -> 15L
            SoundEffect.MATCH_SUCCESS, SoundEffect.COIN_GAIN -> 35L
            SoundEffect.MATCH_ERROR, SoundEffect.LIFE_LOST -> 60L
            SoundEffect.VICTORY, SoundEffect.ACHIEVEMENT_UNLOCKED, SoundEffect.LEVEL_UP -> 120L
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

    fun onAppForeground() {}
    fun onAppBackground() {}

    fun release() {
        try {
            soundPool.release()
            musicManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar áudio", e)
        } finally {
            synchronized(GameAudioManager::class.java) {
                INSTANCE = null
            }
        }
    }
}
