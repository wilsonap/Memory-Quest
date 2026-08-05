package com.example.config

/**
 * Legacy AdConfig wrapper pointing to AdMobConfig.
 */
object AdConfig {
    val currentBannerId: String
        get() = AdMobConfig.bannerId

    val interstitialId: String
        get() = AdMobConfig.interstitialId

    val appOpenId: String
        get() = AdMobConfig.appOpenId

    val rewardedInterstitialId: String
        get() = AdMobConfig.rewardedInterstitialId

    const val ADS_ENABLED = AdMobConfig.ADS_ENABLED
}
