package com.example.config

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MemoryQuest_AdMob"

class BannerManager(
    private val context: Context,
    private val adUnitId: String = AdMobConfig.bannerId,
    private val screenName: String = "Unknown",
    private val coroutineScope: CoroutineScope,
    private val onAdLoadedStateChange: (Boolean) -> Unit = {}
) {
    private var adView: AdView? = null
    private var isDestroyed = false
    private var retryCount = 0
    private var retryJob: Job? = null

    private val retryDelays = listOf(5000L, 15000L, 30000L)

    private fun getMaskedAdUnitId(): String {
        return if (BuildConfig.DEBUG || adUnitId.contains("3940256099942544")) {
            "TEST_AD_UNIT ($adUnitId)"
        } else {
            "PRODUCTION_AD_UNIT (***${adUnitId.takeLast(6)})"
        }
    }

    fun createAdView(adSize: AdSize): AdView {
        if (isDestroyed) {
            val view = AdView(context)
            this.adView = view
            return view
        }

        Log.d(TAG, "Screen=$screenName AdView created")

        val view = AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = this@BannerManager.adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (isDestroyed) return
                    retryCount = 0
                    Log.d(TAG, "Screen=$screenName onAdLoaded")
                    onAdLoadedStateChange(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isDestroyed) return
                    Log.w(
                        TAG,
                        "Screen=$screenName onAdFailedToLoad code=${loadAdError.code} message=${loadAdError.message}"
                    )
                    onAdLoadedStateChange(false)
                    scheduleRetry()
                }

                override fun onAdOpened() {
                    if (isDestroyed) return
                    Log.d(TAG, "Screen=$screenName onAdOpened")
                }

                override fun onAdClosed() {
                    if (isDestroyed) return
                    Log.d(TAG, "Screen=$screenName onAdClosed")
                }

                override fun onAdImpression() {
                    if (isDestroyed) return
                    Log.d(TAG, "Screen=$screenName onAdImpression")
                }

                override fun onAdClicked() {
                    if (isDestroyed) return
                    Log.d(TAG, "Screen=$screenName onAdClicked")
                }
            }
        }
        this.adView = view
        loadAd()
        return view
    }

    private fun scheduleRetry() {
        if (isDestroyed) return
        if (retryCount < retryDelays.size) {
            val delayMs = retryDelays[retryCount]
            val attempt = retryCount + 1
            retryCount++
            Log.d(TAG, "Screen=$screenName retry=$attempt scheduled=${delayMs / 1000}s")
            retryJob?.cancel()
            retryJob = coroutineScope.launch {
                delay(delayMs)
                if (!isDestroyed) {
                    Log.d(TAG, "Screen=$screenName retry=$attempt loadAd")
                    loadAdInternal()
                }
            }
        }
    }

    fun loadAd() {
        if (isDestroyed) return
        retryCount = 0
        retryJob?.cancel()
        loadAdInternal()
    }

    private fun loadAdInternal() {
        if (isDestroyed) return
        if (!AdMobManager.canRequestAds()) {
            Log.d(TAG, "Screen=$screenName canRequestAds is false, ignoring banner request.")
            return
        }

        Handler(Looper.getMainLooper()).post {
            if (isDestroyed) return@post
            try {
                Log.d(TAG, "Screen=$screenName loadAd requested")
                adView?.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e(TAG, "Screen=$screenName exception loading banner: ${e.message}", e)
            }
        }
    }

    fun pause() {
        if (isDestroyed) return
        try {
            adView?.pause()
            Log.d(TAG, "Screen=$screenName paused")
        } catch (e: Exception) {
            Log.w(TAG, "Screen=$screenName error pausing adView: ${e.message}")
        }
    }

    fun resume() {
        if (isDestroyed) return
        try {
            adView?.resume()
            Log.d(TAG, "Screen=$screenName resumed")
        } catch (e: Exception) {
            Log.w(TAG, "Screen=$screenName error resuming adView: ${e.message}")
        }
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        retryJob?.cancel()
        retryJob = null
        try {
            adView?.destroy()
            adView = null
            Log.d(TAG, "Screen=$screenName AdView destroyed")
        } catch (e: Exception) {
            Log.w(TAG, "Screen=$screenName error destroying adView: ${e.message}")
        }
    }
}


