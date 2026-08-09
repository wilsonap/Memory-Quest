package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

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
        private const val TAG = "MemoryQuest_Audio"

        @Volatile
        private var INSTANCE: GameAudioManager? = null

        fun getInstance(context: Context): GameAudioManager {
            return INSTANCE ?: synchronized(this) {
                // Use plain applicationContext for SoundPool.load / openRawResourceFd.
                // AttributionContext is not required for SFX and can interfere on some APIs.
                val appContext = context.applicationContext
                INSTANCE ?: GameAudioManager(appContext).also {
                    INSTANCE = it
                    Log.i(TAG, "GameAudioManager criado | sdk=${Build.VERSION.SDK_INT} | pkg=${appContext.packageName}")
                }
            }
        }
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    val musicManager = MusicManager.getInstance(context)

    // Audio settings
    var isSoundEnabled: Boolean = true
        private set
    var isVibrationEnabled: Boolean = true
        private set
    var sfxVolume: Float = 0.8f
        private set

    // SoundPool for SFX
    @Volatile
    private var soundPool: SoundPool? = null
    private var isReleased: Boolean = false
    private val soundMap = ConcurrentHashMap<SoundEffect, Int>()
    private val resToSoundIdMap = ConcurrentHashMap<Int, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val lastPlayTimes = ConcurrentHashMap<SoundEffect, Long>()

    init {
        ensureSoundPoolReady()
    }

    @Synchronized
    private fun ensureSoundPoolReady() {
        if (soundPool != null && !isReleased) {
            Log.d(TAG, "ensureSoundPoolReady() SKIP: pool ja pronto isReleased=$isReleased")
            return
        }

        Log.i(TAG, "ensureSoundPoolReady() CRIANDO SoundPool | isReleased=$isReleased poolWasNull=${soundPool == null}")
        isReleased = false
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val sp = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        sp.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
                Log.i(TAG, "OnLoadComplete SUCCESS: soundId=$sampleId, status=$status | loadedCount=${loadedSoundIds.size}")
            } else {
                // status=-10000 == AMEDIA_ERROR_UNKNOWN (decode falhou: MP3 corrompido / sem frame MPEG)
                Log.e(TAG, "OnLoadComplete FAILED: soundId=$sampleId, status=$status (0=ok, -10000=AMEDIA_ERROR_UNKNOWN/decode)")
            }
        }

        soundPool = sp
        Log.i(TAG, "SoundPool criado")
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
                    Log.i(TAG, "prefs soundEffectsEnabled=$isSoundEnabled (salvo/lido do DataStore, default=true)")
                }
            }
            launch {
                dataStoreManager.vibrationEnabled.collectLatest { enabled ->
                    isVibrationEnabled = enabled
                    Log.d(TAG, "prefs vibrationEnabled=$isVibrationEnabled")
                }
            }
            launch {
                dataStoreManager.sfxVolume.collectLatest { vol ->
                    sfxVolume = vol.coerceIn(0f, 1f)
                    Log.i(TAG, "prefs soundEffectsVolume=$sfxVolume (default=0.8)")
                }
            }
        }
    }

    private fun loadSoundEffects() {
        val sp = soundPool ?: return
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
                val existingSoundId = resToSoundIdMap[resId]
                if (existingSoundId != null) {
                    soundMap[effect] = existingSoundId
                    Log.d(TAG, "load() reused: effect=${effect.name}, resId=$resId -> soundId=$existingSoundId")
                } else {
                    try {
                        val afd = context.resources.openRawResourceFd(resId)
                        val afdInfo = if (afd != null) {
                            "afdLen=${afd.length} afdOffset=${afd.startOffset}"
                        } else {
                            "afd=NULL"
                        }
                        afd?.close()
                        val soundId = sp.load(context, resId, 1)
                        if (soundId != 0) {
                            resToSoundIdMap[resId] = soundId
                            soundMap[effect] = soundId
                            Log.i(TAG, "load(): effect=${effect.name}, resName=$actualResName, resId=$resId -> soundId=$soundId | $afdInfo")
                        } else {
                            Log.e(TAG, "load() FAILED soundId=0: effect=${effect.name}, resId=$resId | $afdInfo")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "load(): Falha ao carregar SFX $actualResName", e)
                    }
                }
            } else {
                Log.w(TAG, "load(): resId=0 para raw/${effect.resName} (recurso ausente no APK)")
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

        Log.d(
            TAG,
            "play() request effect=${effect.name} | soundEffectsEnabled=$isSoundEnabled | sfxVolume=$sfxVolume | vibrationEnabled=$isVibrationEnabled"
        )

        if (!isSoundEnabled) {
            Log.d(TAG, "play() SKIPPED: soundEffectsEnabled=false for effect=${effect.name}")
            return
        }

        if (sfxVolume <= 0f) {
            Log.d(TAG, "play() SKIPPED: sfxVolume=0.0 for effect=${effect.name}")
            return
        }

        ensureSoundPoolReady()

        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[effect] ?: 0L
        if (now - lastTime < effect.cooldownMs) {
            Log.d(TAG, "play() THROTTLED: effect=${effect.name}, delta=${now - lastTime}ms < cooldown=${effect.cooldownMs}ms")
            return
        }
        lastPlayTimes[effect] = now

        val soundId = soundMap[effect]
        if (soundId == null || soundId == 0) {
            Log.w(TAG, "play() FAILED: soundId ausente para ${effect.name} (resName=${effect.resName})")
            return
        }

        if (!loadedSoundIds.contains(soundId)) {
            Log.w(
                TAG,
                "play() REJECTED: soundId=$soundId effect=${effect.name} nao esta em loadedSoundIds (OnLoadComplete falhou ou pendente) | loaded=${loadedSoundIds.size}"
            )
            return
        }

        val volume = sfxVolume.coerceIn(0f, 1f)
        // play(soundId, leftVolume, rightVolume, priority, loop, rate)
        val streamId = soundPool?.play(soundId, volume, volume, 1, 0, 1.0f) ?: 0
        if (streamId == 0) {
            Log.e(
                TAG,
                "play() FAILED streamId=0: effect=${effect.name}, soundId=$soundId, left=$volume, right=$volume, priority=1, loop=0, rate=1.0 | poolNull=${soundPool == null}"
            )
        } else {
            Log.i(
                TAG,
                "play() OK: effect=${effect.name}, soundId=$soundId, left=$volume, right=$volume, priority=1, loop=0, rate=1.0, streamId=$streamId"
            )
        }
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
    fun playLifeLost() = playSfx(SoundEffect.LIFE_LOST)
    fun playGameOver() = playSfx(SoundEffect.GAME_OVER)
    fun playPurchaseSuccess() = playSfx(SoundEffect.PURCHASE_SUCCESS)
    fun playCountdown() = playSfx(SoundEffect.COUNTDOWN)

    /**
     * Test sequence for settings screen button
     */
    suspend fun testEffectsSequence() {
        Log.d(TAG, "--- INICIANDO TESTE EM SEQUENCIA DE EFEITOS SONOROS ---")
        Log.d(TAG, "Testando: button")
        playButton()
        delay(400)
        Log.d(TAG, "Testando: card_flip")
        playCardFlip()
        delay(400)
        Log.d(TAG, "Testando: match")
        playMatch()
        delay(400)
        Log.d(TAG, "Testando: match_error")
        playMismatch()
        delay(400)
        Log.d(TAG, "Testando: coin")
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
        Log.i(TAG, "release() / SoundPool destruído")
        try {
            isReleased = true
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            resToSoundIdMap.clear()
            loadedSoundIds.clear()
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

