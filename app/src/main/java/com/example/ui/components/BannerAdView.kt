package com.example.ui.components

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.config.AdConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "MemoryQuestAds"

@Composable
fun BannerAdView(
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    val adUnitId = AdConfig.currentBannerId

    if (isAdsRemoved || !AdConfig.ADS_ENABLED || adUnitId.isBlank()) {
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var isAdFailed by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    val adaptiveAdSize = remember(context) { getAdaptiveAdSize(context) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isAdLoaded && !isAdFailed) {
                    Modifier
                        .wrapContentHeight()
                        .padding(vertical = 4.dp)
                } else {
                    Modifier.height(0.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adaptiveAdSize)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            isAdFailed = false
                            Log.d(TAG, "onAdLoaded - Banner ($adUnitId) carregado com sucesso.")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isAdLoaded = false
                            isAdFailed = true
                            Log.w(
                                TAG,
                                "onAdFailedToLoad - Code: ${loadAdError.code}, " +
                                "Domain: ${loadAdError.domain}, " +
                                "Message: ${loadAdError.message}, " +
                                "ResponseInfo: ${loadAdError.responseInfo}, " +
                                "Cause: ${loadAdError.cause}"
                            )
                        }

                        override fun onAdOpened() {
                            Log.d(TAG, "onAdOpened")
                        }

                        override fun onAdClosed() {
                            Log.d(TAG, "onAdClosed")
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "onAdImpression")
                        }
                    }

                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Exceção ao carregar AdMob banner", e)
                        isAdFailed = true
                    }

                    adViewRef = this
                }
            },
            update = { /* sem recarregamentos em recomposições */ }
        )
    }

    DisposableEffect(adUnitId) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao destruir AdView: ${e.message}")
            }
            adViewRef = null
        }
    }
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
        Log.w(TAG, "Falha ao calcular tamanho adaptativo de anúncio, usando BANNER padrão", e)
        AdSize.BANNER
    }
}

