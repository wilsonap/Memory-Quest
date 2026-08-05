package com.example.config

import com.example.BuildConfig

/**
 * AdMob Central Configuration
 * Manages official test IDs in DEBUG mode and real production IDs in RELEASE mode.
 */
object AdMobConfig {
    const val ADS_ENABLED = true

    // --- OFFICIAL GOOGLE ADMOB TEST IDs ---
    private const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"

    // --- PRODUCTION REAL ADMOB IDs ---
    private const val PROD_APP_ID = "ca-app-pub-9508188839425769~9719772999"
    private const val PROD_BANNER_ID = "ca-app-pub-9508188839425769/6933827559"
    private const val PROD_REWARDED_ID = "ca-app-pub-9508188839425769/0000000000" // Reserved for future expansion

    val isDebugMode: Boolean
        get() = BuildConfig.DEBUG

    val appId: String
        get() = if (isDebugMode || PROD_APP_ID.contains("0000000000")) TEST_APP_ID else PROD_APP_ID

    val bannerId: String
        get() = if (isDebugMode || PROD_BANNER_ID.contains("0000000000")) TEST_BANNER_ID else PROD_BANNER_ID

    val rewardedId: String
        get() = if (isDebugMode || PROD_REWARDED_ID.contains("0000000000")) TEST_REWARDED_ID else PROD_REWARDED_ID

    val interstitialId: String
        get() = TEST_INTERSTITIAL_ID

    val appOpenId: String
        get() = TEST_APP_OPEN_ID

    val rewardedInterstitialId: String
        get() = TEST_REWARDED_INTERSTITIAL_ID
}
