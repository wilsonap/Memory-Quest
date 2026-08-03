package com.example.config

/**
 * Legacy AdConfig wrapper pointing to AdMobConfig.
 */
object AdConfig {
    val currentBannerId: String
        get() = AdMobConfig.bannerId

    const val ADS_ENABLED = AdMobConfig.ADS_ENABLED
}
