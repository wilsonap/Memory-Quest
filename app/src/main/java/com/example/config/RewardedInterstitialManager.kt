package com.example.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

private const val TAG = "RewardedInterstitial"

class RewardedInterstitialManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.rewardedInterstitialId
) {
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false

    fun loadAd(onAdLoaded: (() -> Unit)? = null, onAdFailed: (() -> Unit)? = null) {
        if (isLoading || rewardedInterstitialAd != null) return
        isLoading = true

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[DEBUG LOG] Solicitando Rewarded Interstitial ($adUnitId)...")
        }

        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoading = false
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Rewarded Interstitial carregado.")
                    }
                    onAdLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoading = false
                    if (BuildConfig.DEBUG) {
                        Log.w(
                            TAG,
                            "[DEBUG LOG] Falha ao carregar Rewarded Interstitial - Code: ${loadAdError.code}, Message: ${loadAdError.message}"
                        )
                    }
                    onAdFailed?.invoke()
                }
            }
        )
    }

    fun show(activity: Activity, onUserEarnedReward: (amount: Int, type: String) -> Unit, onAdDismissed: () -> Unit = {}) {
        val ad = rewardedInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Rewarded Interstitial dispensado.")
                    }
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    rewardedInterstitialAd = null
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[DEBUG LOG] Erro ao exibir Rewarded Interstitial: ${adError.message}")
                    }
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Rewarded Interstitial exibido.")
                    }
                }
            }

            ad.show(activity) { rewardItem ->
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[DEBUG LOG] Recompensa recebida: ${rewardItem.amount} x ${rewardItem.type}")
                }
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Rewarded Interstitial não disponível.")
            }
            onAdDismissed()
        }
    }
}
