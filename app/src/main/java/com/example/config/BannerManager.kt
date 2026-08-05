package com.example.config

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "MemoryQuestAds"

class BannerManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.bannerId,
    private val onAdLoadedStateChange: (Boolean) -> Unit = {}
) {
    private var adView: AdView? = null

    private fun getMaskedAdUnitId(): String {
        return if (BuildConfig.DEBUG || adUnitId.contains("3940256099942544")) {
            "TEST_AD_UNIT ($adUnitId)"
        } else {
            "PRODUCTION_AD_UNIT (***${adUnitId.takeLast(6)})"
        }
    }

    fun createAdView(adSize: AdSize): AdView {
        val view = AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = this@BannerManager.adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    onAdLoadedStateChange(true)
                    Log.d(TAG, "onAdLoaded - Banner ${getMaskedAdUnitId()} (largura: ${adSize.width}) carregado com sucesso.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdLoadedStateChange(false)
                    Log.w(
                        TAG,
                        "onAdFailedToLoad - Code: ${loadAdError.code}, " +
                        "Domain: ${loadAdError.domain}, Message: ${loadAdError.message}, " +
                        "ResponseInfo: ${loadAdError.responseInfo}"
                    )
                }

                override fun onAdOpened() {
                    Log.d(TAG, "onAdOpened - Banner expandido/aberto.")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "onAdClosed - Banner fechado.")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "onAdImpression - Impressão do banner registrada.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "onAdClicked - Banner clicado pelo usuário.")
                }
            }
        }
        this.adView = view
        loadAd()
        return view
    }

    fun loadAd() {
        if (!AdMobManager.canRequestAds()) {
            Log.d(TAG, "canRequestAds e falso, ignorando solitação de banner.")
            return
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "Banner solicitado: unit=${getMaskedAdUnitId()}")
                adView?.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e(TAG, "Exceção ao carregar banner AdMob: ${e.message}", e)
            }
        }
    }

    fun destroy() {
        try {
            adView?.destroy()
            adView = null
            Log.d(TAG, "Banner AdView destruído.")
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao destruir banner: ${e.message}")
        }
    }
}

