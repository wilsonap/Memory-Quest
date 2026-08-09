package com.example.audio

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
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
        private const val LOG_TAG = "MemoryQuest_Audio"

        @Volatile
        private var INSTANCE: MusicManager? = null

        fun getInstance(context: Context): MusicManager {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                INSTANCE ?: MusicManager(appContext).also {
                    INSTANCE = it
                    it.initLifecycleObserver()
                    Log.i(LOG_TAG, "MusicManager criado | sdk=${Build.VERSION.SDK_INT}")
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
    var isSettingsLoaded: Boolean = false
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
                            Log.e(LOG_TAG, "Erro de reprodução no ExoPlayer para faixa $currentTrack: ${error.message}", error)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateStr = when (playbackState) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> "UNKNOWN($playbackState)"
                            }
                            Log.i(LOG_TAG, "ExoPlayer state changed to $stateStr for track $currentTrack")
                        }
                    })
                }
            Log.i(LOG_TAG, "ExoPlayer único inicializado com sucesso. Instâncias ativas: $playerInstanceCount | volume=$musicVolume")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Falha ao inicializar ExoPlayer para música de fundo: ${e.message}", e)
        }
    }

    private fun initLifecycleObserver() {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            Log.i(LOG_TAG, "Observador de ciclo de vida do processo registrado")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Não foi possível registrar o observador de ciclo de vida do processo: ${e.message}", e)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        Log.i(LOG_TAG, "Lifecycle: Foreground | Track: ${currentTrack?.name ?: "NONE"} | Resuming music | isMusicEnabled=$isMusicEnabled")
        resumeMusic()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        Log.i(LOG_TAG, "Lifecycle: Background | Track: ${currentTrack?.name ?: "NONE"} | Pausing music")
        pauseMusic()
    }

    fun observeSettings(dataStoreManager: DataStoreManager, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            launch {
                dataStoreManager.musicEnabled.collectLatest { enabled ->
                    val prevEnabled = isMusicEnabled
                    val prevLoaded = isSettingsLoaded
                    isSettingsLoaded = true
                    isMusicEnabled = enabled

                    Log.i(
                        LOG_TAG,
                        "Settings loaded: musicEnabled=$enabled (was $prevEnabled, wasLoaded=$prevLoaded) | currentTrack=${currentTrack?.name ?: "NONE"}"
                    )

                    if (!enabled) {
                        Log.i(LOG_TAG, "Música desativada nas configurações -> Parando reprodução")
                        stopMusic()
                    } else {
                        if (!prevLoaded || !prevEnabled) {
                            if (currentTrack != null && isAppInForeground) {
                                Log.i(LOG_TAG, "Música ativada nas configurações -> Iniciando $currentTrack")
                                playTrack(currentTrack!!)
                            }
                        }
                    }
                }
            }
            launch {
                dataStoreManager.musicVolume.collectLatest { vol ->
                    musicVolume = vol
                    Log.i(LOG_TAG, "Settings loaded: musicVolume=$musicVolume")
                    if (fadeJob?.isActive != true) {
                        exoPlayer?.volume = vol
                    }
                }
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        val player = exoPlayer ?: run {
            Log.e(LOG_TAG, "playTrack() cancelado: ExoPlayer e nulo")
            return
        }
        val prevTrack = currentTrack
        currentTrack = track

        Log.i(
            LOG_TAG,
            "playTrack() solicitado: ${track.name} | isMusicEnabled=$isMusicEnabled | isAppInForeground=$isAppInForeground | musicVolume=$musicVolume"
        )

        if (!isMusicEnabled || !isAppInForeground) {
            Log.i(
                LOG_TAG,
                "playTrack() bloqueado: isMusicEnabled=$isMusicEnabled, isAppInForeground=$isAppInForeground"
            )
            stopMusic()
            return
        }

        if (prevTrack == track && player.isPlaying && player.mediaItemCount > 0) {
            Log.i(LOG_TAG, "Faixa $track ja esta tocando. Mantendo reproducao.")
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
            Log.e(LOG_TAG, "Recurso de audio para a faixa ${track.name} nao encontrado em res/raw")
            stopMusic()
            return
        }

        Log.i(LOG_TAG, "Carregando faixa ${track.name} (raw/$loadedName, resId=$foundResId)...")

        fadeJob?.cancel()
        fadeJob = mainScope.launch {
            if (player.isPlaying) {
                val startVol = player.volume
                val steps = 8
                val delayMs = 15L
                for (i in steps downTo 0) {
                    player.volume = startVol * (i / steps.toFloat())
                    delay(delayMs)
                }
            }

            try {
                player.stop()
                player.clearMediaItems()

                val rawUri = RawResourceDataSource.buildRawResourceUri(foundResId)
                val mediaItem = MediaItem.fromUri(rawUri)

                val isLooping = (track != MusicTrack.VICTORY && track != MusicTrack.DEFEAT)
                player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                player.setMediaItem(mediaItem)
                player.prepare()
                player.volume = 0f
                player.play()

                Log.i(LOG_TAG, "Playback iniciado para faixa ${track.name} (raw/$loadedName, looping=$isLooping)")

                val steps = 10
                val delayMs = 20L
                val targetVol = musicVolume
                for (i in 1..steps) {
                    player.volume = targetVol * (i / steps.toFloat())
                    delay(delayMs)
                }
                player.volume = targetVol
                Log.i(LOG_TAG, "Volume da musica ajustado para $targetVol para a faixa ${track.name}")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "ERROR loading/playing music track ${track.name} (raw/$loadedName): ${e.message}", e)
            }
        }
    }

    fun pauseMusic() {
        fadeJob?.cancel()
        try {
            Log.i(LOG_TAG, "pauseMusic() executado | Track: ${currentTrack?.name ?: "NONE"}")
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "ERROR ao pausar musica no ExoPlayer: ${e.message}", e)
        }
    }

    fun resumeMusic() {
        Log.i(
            LOG_TAG,
            "resumeMusic() | Track: ${currentTrack?.name ?: "NONE"} | isMusicEnabled=$isMusicEnabled | isAppInForeground=$isAppInForeground"
        )
        if (!isMusicEnabled || !isAppInForeground) {
            stopMusic()
            return
        }
        val player = exoPlayer ?: return

        try {
            if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                Log.i(LOG_TAG, "Retomando reproducao do ExoPlayer para ${currentTrack?.name ?: "NONE"}")
                player.volume = musicVolume
                player.play()
            } else if (currentTrack != null) {
                playTrack(currentTrack!!)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "ERROR ao retomar musica no ExoPlayer: ${e.message}", e)
        }
    }

    fun stopMusic() {
        fadeJob?.cancel()
        try {
            Log.i(LOG_TAG, "stopMusic() executado | Track: ${currentTrack?.name ?: "NONE"}")
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "ERROR ao parar musica no ExoPlayer: ${e.message}", e)
        }
    }

    fun release() {
        fadeJob?.cancel()
        try {
            Log.i(LOG_TAG, "MusicManager release() executado")
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            Log.e(LOG_TAG, "ERROR ao liberar ExoPlayer: ${e.message}", e)
        }
    }
}

