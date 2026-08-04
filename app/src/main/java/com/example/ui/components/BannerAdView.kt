package com.example.ui.components

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.config.AdMobConfig
import com.example.config.BannerManager
import com.example.sync.ConnectivityObserver
import com.google.android.gms.ads.AdSize

private const val TAG = "BannerAdView"

/**
 * Componente reutilizável e padronizado para exibição de banners de anúncio em todo o aplicativo.
 * Garante reserva de espaço durante o carregamento (evita pulo de layout) e suporte a insets.
 */
@Composable
fun BannerAdContainer(
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    val adUnitId = AdMobConfig.bannerId

    if (isAdsRemoved || !AdMobConfig.ADS_ENABLED || adUnitId.isBlank()) {
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var bannerManagerRef by remember { mutableStateOf<BannerManager?>(null) }

    val adaptiveAdSize = remember(context) { getAdaptiveAdSize(context) }
    val bannerHeightDp = remember(adaptiveAdSize) { adaptiveAdSize.getHeight().coerceAtLeast(50) }

    // Re-tentar carregar ao reconectar à internet
    DisposableEffect(context) {
        val observer = ConnectivityObserver(context) {
            if (!isAdLoaded) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[DEBUG LOG] Conexão restabelecida. Recarregando banner AdMob...")
                }
                bannerManagerRef?.loadAd()
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
                    val manager = BannerManager(
                        context = ctx,
                        adUnitId = adUnitId,
                        onAdLoadedStateChange = { loaded ->
                            isAdLoaded = loaded
                        }
                    )
                    bannerManagerRef = manager
                    manager.createAdView(adaptiveAdSize)
                },
                update = { /* sem recomposições desnecessárias */ }
            )
        }
    }

    DisposableEffect(adUnitId) {
        onDispose {
            bannerManagerRef?.destroy()
            bannerManagerRef = null
        }
    }
}

/**
 * Alias de compatibilidade para reutilização direta do BannerAdContainer.
 */
@Composable
fun BannerAdView(
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    BannerAdContainer(
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
