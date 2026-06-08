package com.codeancy.metroui.firebase

import org.corexero.sutradhar.remoteConfig.ConfigKey

sealed class MetroConfigKey<T>(
    key: String,
    defaultValue: T
) : ConfigKey<T>(key, defaultValue) {
    data object EnableInAppReview : MetroConfigKey<Boolean>(
        key = "ENABLE_IN_APP_REVIEW",
        defaultValue = true
    )

    data object AppUpdateInfo : MetroConfigKey<String>(
        key = "APP_UPDATE_INFO",
        defaultValue = ""
    )

    data object EnableAds: MetroConfigKey<Boolean>(
        key = "ENABLE_AD",
        defaultValue = false
    )

    data object EnablePremium : MetroConfigKey<Boolean>(
        key = "ENABLE_PREMIUM",
        defaultValue = false
    )
}