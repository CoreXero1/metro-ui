package com.codeancy.metroui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.codeancy.metroui.common.utils.MetroConfig
import com.codeancy.metroui.map.MapScreen
import com.codeancy.metroui.map.MetroTopAppBar

@Composable
fun MetroNavigation(
    navController: NavHostController,
    showInAppReview: () -> Unit
) {

    NavHost(
        navController,
        startDestination = HomeScreenRoute(),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .safeDrawingPadding()
    ) {

        composable<HomeScreenRoute> {
            HomeScreenRoute.Invoke(
                homeScreenRoute = it.toRoute(),
                onNavigateToMapScreen = {
                    navController.navigate(MapScreenRoute())
                },
                onNavigateToRouteScreen = { sourceId, destId, isRecent ->
                    navController.navigate(
                        RouteScreenRoute(
                            sourceId = sourceId,
                            destId = destId,
                            isRecent = isRecent
                        )
                    )
                },
                onNavigateToPremium = {
                    navController.navigate(
                        PayrollScreenRoute
                    )
                }
            )
        }

        composable<RouteScreenRoute> { it ->
            val adsController = MetroConfig.interstitialAdController
            RouteScreenRoute.Invoke(
                showInAppReview = showInAppReview,
                onNavigateUp = {
                    if (adsController != null) {
                        adsController.show {
                            navController.navigateUp()
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
                onNavigateToMap = { sourceId, destId ->
                    navController.navigate(
                        MapScreenRoute(
                            sourceId = sourceId,
                            destId = destId
                        )
                    )
                },
                routeScreenRoute = it.toRoute()
            )
        }

        composable<MapScreenRoute> { it ->
            MapScreenRoute.Invoke(
                mapScreenRoute = it.toRoute(),
                onNavigateUp = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { sourceId, destId ->
                    navController.navigate(
                        RouteScreenRoute(
                            sourceId = sourceId,
                            destId = destId
                        )
                    )
                }
            )
        }

        composable<PayrollScreenRoute> { it ->
            PayrollScreenRoute.Invoke(
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
    }
}