package com.codeancy.metroui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.codeancy.metroui.common.utils.MetroConfig
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.AnalyticsParams
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import com.codeancy.metroui.inAppReview.rememberShowInAppReview
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {

    val metroConfig = MetroConfig

    val deepLinkNavigator = koinInject<DeepLinkNavigator>()
    val lifecycle = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val showInAppReview = rememberShowInAppReview()

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            deepLinkNavigator.deepsLinks.collect { screen ->
                when (screen) {
                    Screen.AskForFeedback -> {
                        navController.navigate(HomeScreenRoute(feedback = true))
                    }

                    Screen.Home -> {
                        navController.navigate(HomeScreenRoute())
                    }

                    Screen.Map -> {
                        navController.navigate(MapScreenRoute())
                    }

                    Screen.RateUs -> {
                        showInAppReview.show()
                    }

                    is Screen.Route -> {
                        navController.navigate(
                            RouteScreenRoute(
                                sourceId = screen.sourceId,
                                destId = screen.destId,
                            )
                        )
                    }

                    null -> Unit
                }
            }
        }
    }

    MetroTheme {
        MetroNavigation(
            navController = navController,
            showInAppReview = {
                showInAppReview.show()
            }
        )
    }

    LaunchedEffect(Unit) {
        initFirebaseAnalytics(
            metroConfig.appName,
            metroConfig.appVersion
        )
    }

}

private fun initFirebaseAnalytics(
    platformName: String,
    appVersion: String
) {
    FirebaseAnalyticsTracker.setGlobalProperties(
        mapOf(
            AnalyticsParams.PLATFORM_TYPE to platformName,
            AnalyticsParams.APP_VERSION to appVersion
        )
    )
    FirebaseAnalyticsTracker.logEvent(
        AnalyticsEvents.APP_LAUNCH,
        screenName = ScreenName.HOME_SCREEN
    )
}