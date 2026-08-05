package com.example.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TAG = "MemoryQuestAds"

class InterstitialManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.interstitialId
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd(onAdLoaded: (() -> Unit)? = null, onAdFailed: (() -> Unit)? = null) {
        if (!AdMobManager.canRequestAds()) {
            Log.d(TAG, "canRequestAds e falso, ignorando solitação de Interstitial.")
            onAdFailed?.invoke()
            return
        }
        if (isLoading || interstitialAd != null) return
        isLoading = true

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Log.d(TAG, "Interstitial solicitado: adUnitId=$adUnitId")

            InterstitialAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoading = false
                        Log.d(TAG, "onAdLoaded - Interstitial $adUnitId carregado com sucesso.")
                        onAdLoaded?.invoke()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isLoading = false
                        Log.w(
                            TAG,
                            "onAdFailedToLoad - Code: ${loadAdError.code}, " +
                            "Domain: ${loadAdError.domain}, Message: ${loadAdError.message}, " +
                            "ResponseInfo: ${loadAdError.responseInfo}"
                        )
                        onAdFailed?.invoke()
                    }
                }
            )
        }
    }

    fun isReady(): Boolean = interstitialAd != null

    fun show(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Interstitial dispensado.")
                    }
                    loadAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[DEBUG LOG] Erro ao exibir Interstitial: ${adError.message}")
                    }
                    loadAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Interstitial exibido.")
                    }
                }
            }
            ad.show(activity)
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Interstitial não pronto para exibição. Carregando...")
            }
            loadAd()
            onAdDismissed()
        }
    }
}
