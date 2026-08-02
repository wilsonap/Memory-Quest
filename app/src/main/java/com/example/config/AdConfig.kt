package com.example.config

import com.example.BuildConfig

object AdConfig {
    const val ADMOB_APP_ID = "ca-app-pub-9508188839425769~9719772999"
    const val PRODUCTION_BANNER_ID = "ca-app-pub-9508188839425769/6933827559"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val ADS_ENABLED = true

    val currentBannerId: String
        get() {
            return if (BuildConfig.DEBUG) {
                TEST_BANNER_ID
            } else {
                PRODUCTION_BANNER_ID
            }
        }
}
