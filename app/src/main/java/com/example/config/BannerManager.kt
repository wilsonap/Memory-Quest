package com.example.config

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerManager"

class BannerManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.bannerId,
    private val onAdLoadedStateChange: (Boolean) -> Unit = {}
) {
    private var adView: AdView? = null

    fun createAdView(adSize: AdSize): AdView {
        val view = AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = this@BannerManager.adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    onAdLoadedStateChange(true)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] onAdLoaded - Banner ($adUnitId) carregado com sucesso.")
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdLoadedStateChange(false)
                    if (BuildConfig.DEBUG) {
                        Log.w(
                            TAG,
                            "[DEBUG LOG] onAdFailedToLoad - Code: ${loadAdError.code}, " +
                            "Domain: ${loadAdError.domain}, Message: ${loadAdError.message}"
                        )
                    }
                }

                override fun onAdOpened() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] onAdOpened - Banner expandido/aberto.")
                    }
                }

                override fun onAdClosed() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] onAdClosed - Banner fechado.")
                    }
                }

                override fun onAdImpression() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] onAdImpression - Impressão do banner registrada.")
                    }
                }

                override fun onAdClicked() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] onAdClicked - Banner clicado pelo usuário.")
                    }
                }
            }
        }
        this.adView = view
        loadAd()
        return view
    }

    fun loadAd() {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Carregando AdRequest para $adUnitId...")
            }
            adView?.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "[DEBUG LOG] Exceção ao carregar AdMob banner: ${e.message}", e)
            }
        }
    }

    fun destroy() {
        try {
            adView?.destroy()
            adView = null
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Banner AdView destruído.")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "[DEBUG LOG] Erro ao destruir banner: ${e.message}")
            }
        }
    }
}
