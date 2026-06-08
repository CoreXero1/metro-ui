package com.codeancy.metroui.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.codeancy.metroui.ads.InterstitialAdController

expect class MapDrawableResource

data class MetroConfiguration(
    val appTitle: String,
    val allowLocationButtonText: String,
    val mapDrawableResource: MapDrawableResource,
    val appName: String,
    val appVersion: String,
    val interstitialAdController: InterstitialAdController? = null,
    val isPremiumUser: Boolean = false,
    val supportedCities: List<String> = emptyList(),
    val selectedCity: String = "",
    val onCitySelected: (String) -> Unit = {},
    val showBookTicket: Boolean = true,
    val showNearestMetro: Boolean = true,
    val showTimings: Boolean = true,
    // Build-time gate for the premium toolbar icon. Off for the AllIndia build,
    // on for every other build. Kept here (not only in Remote Config) so the
    // decision is deterministic and offline-safe instead of relying on a
    // network-cached `app.id` condition.
    val showPremium: Boolean = true
)

val LocalMetroConfiguration =
    compositionLocalOf<MetroConfiguration> { error("No MetroConfig provided") }

val MetroConfig
    @Composable
    get() = LocalMetroConfiguration.current
