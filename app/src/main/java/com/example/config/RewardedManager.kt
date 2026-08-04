package com.example.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "RewardedManager"

class RewardedManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.rewardedId
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(onAdLoaded: (() -> Unit)? = null, onAdFailed: (() -> Unit)? = null) {
        if (isLoading || rewardedAd != null) return
        isLoading = true

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Solicitando Rewarded ($adUnitId)...")
            }

            RewardedAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isLoading = false
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "[DEBUG LOG] Rewarded ad carregado com sucesso.")
                        }
                        onAdLoaded?.invoke()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isLoading = false
                        if (BuildConfig.DEBUG) {
                            Log.w(
                                TAG,
                                "[DEBUG LOG] Falha ao carregar Rewarded - Code: ${loadAdError.code}, Message: ${loadAdError.message}"
                            )
                        }
                        onAdFailed?.invoke()
                    }
                }
            )
        }
    }

    fun show(activity: Activity, onUserEarnedReward: (amount: Int, type: String) -> Unit, onAdDismissed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Rewarded ad dispensado.")
                    }
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[DEBUG LOG] Erro ao exibir Rewarded: ${adError.message}")
                    }
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Rewarded ad exibido.")
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
                Log.d(TAG, "[DEBUG LOG] Rewarded ad não disponível para exibição.")
            }
            onAdDismissed()
        }
    }
}
