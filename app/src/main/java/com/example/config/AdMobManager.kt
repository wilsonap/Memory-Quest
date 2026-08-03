package com.example.config

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AdMobManager"

object AdMobManager {
    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) {
            return
        }

        try {
            if (BuildConfig.DEBUG) {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(
                        listOf(
                            AdRequest.DEVICE_ID_EMULATOR
                        )
                    )
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)
                Log.d(TAG, "[DEBUG LOG] AdMob inicializado em MODO DEBUG.")
                Log.d(TAG, "[DEBUG LOG] App ID: ${AdMobConfig.appId}")
                Log.d(TAG, "[DEBUG LOG] Banner ID: ${AdMobConfig.bannerId}")
            }

            MobileAds.initialize(context) { initializationStatus ->
                if (BuildConfig.DEBUG) {
                    val statusMap = initializationStatus.adapterStatusMap
                    for ((adapterClass, status) in statusMap) {
                        Log.d(
                            TAG,
                            "[DEBUG LOG] Adapter: $adapterClass, State: ${status.initializationState}, Latency: ${status.latency}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "[DEBUG LOG] Erro ao inicializar AdMob: ${e.message}", e)
            }
        }
    }
}
