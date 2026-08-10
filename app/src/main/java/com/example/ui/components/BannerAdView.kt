package com.example.ui.components

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.BuildConfig
import com.example.config.AdMobConfig
import com.example.config.BannerManager
import com.example.sync.ConnectivityObserver
import com.google.android.gms.ads.AdSize

private const val TAG = "MemoryQuest_AdMob"

/**
 * Componente reutilizável e padronizado para exibição de banners de anúncio em todo o aplicativo.
 * Garante reserva de espaço durante o carregamento (evita pulo de layout) e suporte a insets.
 */
@Composable
fun BannerAdContainer(
    screenName: String = "Unknown",
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier,
    onAdLoadedChanged: (Boolean) -> Unit = {}
) {
    val adUnitId = AdMobConfig.bannerId

    if (isAdsRemoved || !AdMobConfig.ADS_ENABLED || adUnitId.isBlank()) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var isAdLoaded by remember { mutableStateOf(false) }

    val adaptiveAdSize = remember(context) { getAdaptiveAdSize(context) }
    val bannerHeightDp = remember(adaptiveAdSize) {
        adaptiveAdSize.getHeight().coerceAtLeast(50)
    }

    val bannerManager = remember(screenName, adUnitId) {
        BannerManager(
            context = context,
            adUnitId = adUnitId,
            screenName = screenName,
            coroutineScope = coroutineScope,
            onAdLoadedStateChange = { loaded ->
                isAdLoaded = loaded
                onAdLoadedChanged(loaded)
            }
        )
    }

    // Gerenciamento de ciclo de vida do Banner (pause/resume/destroy ao sair da tela)
    DisposableEffect(bannerManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    bannerManager.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    bannerManager.resume()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d(TAG, "Screen=$screenName onDispose")
            lifecycleOwner.lifecycle.removeObserver(observer)
            bannerManager.destroy()
        }
    }

    // Re-tentar carregar ao reconectar à internet
    DisposableEffect(context, bannerManager) {
        val observer = ConnectivityObserver(context) {
            if (!isAdLoaded) {
                Log.d(TAG, "Screen=$screenName conexão restabelecida, tentando recarregar banner...")
                bannerManager.loadAd()
            }
        }
        observer.startListening()

        onDispose {
            observer.stopListening()
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeightDp.dp)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    bannerManager.createAdView(adaptiveAdSize)
                },
                update = { /* sem recomposições desnecessárias */ }
            )
        }
    }
}

/**
 * Componente oficial de banner AdMob para Jetpack Compose.
 */
@Composable
fun BannerAd(
    screenName: String = "Unknown",
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    BannerAdContainer(
        screenName = screenName,
        isAdsRemoved = isPremium,
        modifier = modifier
    )
}

/**
 * Alias de compatibilidade para reutilização direta do BannerAdContainer.
 */
@Composable
fun BannerAdView(
    screenName: String = "Unknown",
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    BannerAdContainer(
        screenName = screenName,
        isAdsRemoved = isAdsRemoved,
        modifier = modifier
    )
}

private fun getAdaptiveAdSize(context: Context): AdSize {
    return try {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val windowMetrics = windowManager?.currentWindowMetrics
            windowMetrics?.bounds?.width()?.toFloat() ?: displayMetrics.widthPixels.toFloat()
        } else {
            displayMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "[DEBUG LOG] Falha ao calcular tamanho adaptativo, usando BANNER padrão", e)
        }
        AdSize.BANNER
    }
}

