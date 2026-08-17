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

private const val TAG = "MemoryQuestAds"

class RewardedManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.rewardedId
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(onAdLoaded: (() -> Unit)? = null, onAdFailed: (() -> Unit)? = null) {
        if (!AdMobManager.canRequestAds()) {
            Log.d(TAG, "canRequestAds e falso, ignorando solitação de Rewarded.")
            onAdFailed?.invoke()
            return
        }
        if (isLoading || rewardedAd != null) return
        isLoading = true

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Log.d(TAG, "Rewarded ad solicitado: adUnitId=$adUnitId")

            RewardedAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isLoading = false
                        Log.d(TAG, "onAdLoaded - Rewarded $adUnitId carregado com sucesso.")
                        onAdLoaded?.invoke()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
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

    fun show(activity: Activity, onUserEarnedReward: (amount: Int, type: String) -> Unit, onAdDismissed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            val rewardDelivered = java.util.concurrent.atomic.AtomicBoolean(false)
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
                if (rewardDelivered.compareAndSet(false, true)) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[DEBUG LOG] Recompensa recebida: ${rewardItem.amount} x ${rewardItem.type}")
                    }
                    onUserEarnedReward(rewardItem.amount, rewardItem.type)
                } else {
                    Log.w(TAG, "Recompensa duplicada do mesmo Rewarded Ad ignorada.")
                }
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[DEBUG LOG] Rewarded ad não disponível para exibição.")
            }
            onAdDismissed()
        }
    }
}
