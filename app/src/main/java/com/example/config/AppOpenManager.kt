package com.example.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

private const val TAG = "AppOpenManager"

class AppOpenManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.appOpenId
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false

    fun fetchAd(onAdLoaded: (() -> Unit)? = null) {
        if (isLoading || isAdAvailable()) return
        isLoading = true

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[DEBUG LOG] Carregando App Open Ad ($adUnitId)...")
        }

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoading = false
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] App Open Ad carregado.")
                    }
                    onAdLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isLoading = false
                    if (BuildConfig.DEBUG) {
                        Log.w(
                            TAG,
                            "[DEBUG LOG] Falha ao carregar App Open Ad - Code: ${loadAdError.code}, Message: ${loadAdError.message}"
                        )
                    }
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] App Open Ad dispensado.")
                    }
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    appOpenAd = null
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[DEBUG LOG] Erro ao exibir App Open Ad: ${adError.message}")
                    }
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] App Open Ad exibido.")
                    }
                }
            }
            ad.show(activity)
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] App Open Ad não disponível para exibição.")
            }
            fetchAd()
            onAdDismissed()
        }
    }
}
