package com.example.audio

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicManager private constructor(private val context: Context) : DefaultLifecycleObserver {

    companion object {
        private const val LOG_TAG = "MemoryQuestMusic"

        @Volatile
        private var INSTANCE: MusicManager? = null

        fun getInstance(context: Context): MusicManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicManager(context.applicationContext).also {
                    INSTANCE = it
                    it.initLifecycleObserver()
                }
            }
        }
    }

    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private var fadeJob: Job? = null

    private var exoPlayer: ExoPlayer? = null

    val playerInstanceCount: Int
        get() = if (exoPlayer != null) 1 else 0

    var isMusicEnabled: Boolean = true
        private set
    var musicVolume: Float = 0.5f
        private set

    var currentTrack: MusicTrack? = null
        private set

    private var isAppInForeground: Boolean = true

    init {
        initExoPlayer()
    }

    private fun initExoPlayer() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_GAME)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .build().apply {
                    volume = musicVolume
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            Log.w(LOG_TAG, "Erro de reprodução no ExoPlayer para faixa $currentTrack: ${error.message}")
                        }
                    })
                }
            Log.d(LOG_TAG, "ExoPlayer único inicializado com sucesso. Instâncias ativas: $playerInstanceCount")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Falha ao inicializar ExoPlayer para música de fundo", e)
        }
    }

    private fun initLifecycleObserver() {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Não foi possível registrar o observador de ciclo de vida do processo", e)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        Log.d(LOG_TAG, "Lifecycle: Foreground | Current track: ${currentTrack?.name ?: "NONE"} | Action: RESUME | Player instances: $playerInstanceCount")
        resumeMusic()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        Log.d(LOG_TAG, "Lifecycle: Background | Current track: ${currentTrack?.name ?: "NONE"} | Action: PAUSE | Player instances: $playerInstanceCount")
        pauseMusic()
    }

    fun observeSettings(dataStoreManager: DataStoreManager, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            launch {
                dataStoreManager.musicEnabled.collectLatest { enabled ->
                    val prevEnabled = isMusicEnabled
                    isMusicEnabled = enabled
                    if (!enabled) {
                        Log.d(LOG_TAG, "Setting changed: Music disabled | Current track: ${currentTrack?.name ?: "NONE"} | Action: PAUSE | Player instances: $playerInstanceCount")
                        pauseMusic()
                    } else if (!prevEnabled && currentTrack != null && isAppInForeground) {
                        Log.d(LOG_TAG, "Setting changed: Music enabled | Current track: ${currentTrack?.name ?: "NONE"} | Action: RESUME | Player instances: $playerInstanceCount")
                        playTrack(currentTrack!!)
                    }
                }
            }
            launch {
                dataStoreManager.musicVolume.collectLatest { vol ->
                    musicVolume = vol
                    if (fadeJob?.isActive != true) {
                        exoPlayer?.volume = vol
                    }
                }
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        val player = exoPlayer ?: return
        val prevTrack = currentTrack

        if (!isMusicEnabled || !isAppInForeground) {
            Log.d(
                LOG_TAG,
                "Current track: ${prevTrack?.name ?: "NONE"} | Requested track: ${track.name} | Action: PAUSE (Disabled or Background) | Player instances: $playerInstanceCount"
            )
            if (player.isPlaying) {
                pauseMusic()
            }
            currentTrack = track
            return
        }

        if (prevTrack == track && (player.isPlaying || player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY)) {
            Log.d(
                LOG_TAG,
                "Current track: ${prevTrack.name} | Requested track: ${track.name} | Action: KEEP | Player instances: $playerInstanceCount"
            )
            return
        }

        val resCandidates = when (track) {
            MusicTrack.HOME -> listOf("music_home")
            MusicTrack.GAME -> listOf("music_game")
            MusicTrack.SHOP -> listOf("music_shop")
            MusicTrack.RANKING -> listOf("music_ranking")
            MusicTrack.VICTORY -> listOf("music_victory")
            MusicTrack.DEFEAT -> listOf("music_defeat")
        }

        var foundResId = 0
        var loadedName = ""

        for (name in resCandidates) {
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            if (id != 0) {
                foundResId = id
                loadedName = name
                break
            }
        }

        if (foundResId == 0) {
            Log.w(
                LOG_TAG,
                "Current track: ${prevTrack?.name ?: "NONE"} | Requested track: ${track.name} | Action: STOP (Resource for ${track.name} not found) | Player instances: $playerInstanceCount"
            )
            stopMusic()
            currentTrack = track
            return
        }

        val actionName = if (prevTrack == null) "PLAY" else "SWITCH"
        Log.d(
            LOG_TAG,
            "Current track: ${prevTrack?.name ?: "NONE"} | Requested track: ${track.name} | Action: $actionName ($loadedName) | Player instances: $playerInstanceCount"
        )

        currentTrack = track

        fadeJob?.cancel()
        fadeJob = mainScope.launch {
            if (player.isPlaying) {
                val startVol = player.volume
                val steps = 10
                val delayMs = 20L
                for (i in steps downTo 0) {
                    player.volume = startVol * (i / steps.toFloat())
                    delay(delayMs)
                }
            }

            try {
                player.stop()
                player.clearMediaItems()

                val uri = "android.resource://${context.packageName}/$foundResId"
                val mediaItem = MediaItem.fromUri(uri)

                val isLooping = (track != MusicTrack.VICTORY && track != MusicTrack.DEFEAT)
                player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                player.setMediaItem(mediaItem)
                player.prepare()
                player.volume = 0f
                player.play()

                val steps = 10
                val delayMs = 20L
                val targetVol = musicVolume
                for (i in 1..steps) {
                    player.volume = targetVol * (i / steps.toFloat())
                    delay(delayMs)
                }
                player.volume = targetVol
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Erro ao carregar/tocar faixa de música ${track.name} (recurso $loadedName)", e)
            }
        }
    }

    fun pauseMusic() {
        fadeJob?.cancel()
        try {
            if (exoPlayer?.isPlaying == true) {
                Log.d(LOG_TAG, "Current track: ${currentTrack?.name ?: "NONE"} | Action: PAUSE | Player instances: $playerInstanceCount")
                exoPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao pausar música no ExoPlayer", e)
        }
    }

    fun resumeMusic() {
        if (!isMusicEnabled || !isAppInForeground) return
        val player = exoPlayer ?: return

        try {
            if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                Log.d(LOG_TAG, "Current track: ${currentTrack?.name ?: "NONE"} | Action: RESUME | Player instances: $playerInstanceCount")
                player.volume = musicVolume
                player.play()
            } else if (currentTrack != null) {
                playTrack(currentTrack!!)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao retomar música no ExoPlayer", e)
        }
    }

    fun stopMusic() {
        fadeJob?.cancel()
        try {
            Log.d(LOG_TAG, "Current track: ${currentTrack?.name ?: "NONE"} | Action: STOP | Player instances: $playerInstanceCount")
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao parar música no ExoPlayer", e)
        }
    }

    fun release() {
        fadeJob?.cancel()
        try {
            Log.d(LOG_TAG, "Action: RELEASE | Player instances: 0")
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao liberar ExoPlayer", e)
        }
    }
}
