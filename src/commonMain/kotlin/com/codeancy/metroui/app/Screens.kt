package com.codeancy.metroui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeancy.metroui.common.utils.IntentUtils
import com.codeancy.metroui.common.utils.MetroConfig
import com.codeancy.metroui.domain.models.LiveLocationUi
import com.codeancy.metroui.home.HomeScreen
import com.codeancy.metroui.home.HomeViewModel
import com.codeancy.metroui.home.utils.HomeScreenUiAction
import com.codeancy.metroui.route.RouteScreen
import com.codeancy.metroui.route.RouteScreenUiAction
import com.codeancy.metroui.route.RouteViewModel
import com.codeancy.metroui.route.utils.RouteScreenRouteUi
import com.corexero.dhan_tantra.sdk.presentation.PayrollScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class HomeScreenRoute(
    val feedback: Boolean = false,
    val openPaywallOnLaunch: Boolean = false,
) {

    companion object {
        @Composable
        fun Invoke(
            homeScreenRoute: HomeScreenRoute,
            onNavigateToRouteScreen: (Long, Long, Boolean) -> Unit,
            onNavigateToMapScreen: () -> Unit,
            onNavigateToPremium: () -> Unit,
        ) {
            val homeViewModel: HomeViewModel = koinViewModel(
                parameters = {
                    parametersOf(
                        homeScreenRoute
                    )
                }
            )

            LaunchedEffect(homeScreenRoute.openPaywallOnLaunch) {
                if (homeScreenRoute.openPaywallOnLaunch) {
                    homeViewModel.onAction(HomeScreenUiAction.OnPremiumClick)
                }
            }

            HomeScreen(
                state = homeViewModel.homeScreenState.collectAsStateWithLifecycle().value,
                onAction = {
                    homeViewModel.onAction(it)
                    when (it) {
                        is HomeScreenUiAction.GetRoute -> onNavigateToRouteScreen(
                            it.sourceId,
                            it.destId,
                            it.isRecent
                        )

                        is HomeScreenUiAction.OnMetroMapClick -> onNavigateToMapScreen()
                        else -> Unit
                    }
                }
            )
        }
    }

}

@Serializable
data class RouteScreenRoute(
    val sourceId: Long,
    val destId: Long,
    val isRecent: Boolean = false
) {
    companion object {

        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        fun Invoke(
            routeScreenRoute: RouteScreenRoute,
            showInAppReview: () -> Unit,
            onNavigateUp: () -> Unit,
            onNavigateToMap: ((Long, Long) -> Unit)? = null
        ) {

            val intentUtils = koinInject<IntentUtils>()
            val adsController = MetroConfig.interstitialAdController

            val routeViewModel: RouteViewModel = koinViewModel<RouteViewModel>(
                parameters = {
                    parametersOf(routeScreenRoute.toRouteScreenRoute())
                }
            )

            val state by routeViewModel.state.collectAsState()
            val providerState by routeViewModel.providerState.collectAsStateWithLifecycle()
            val liveLocationUi by routeViewModel.liveLocationState.collectAsStateWithLifecycle()


            LaunchedEffect(liveLocationUi) {
                when (liveLocationUi) {
                    LiveLocationUi.NotInMetro -> {
                        routeViewModel.onAction(RouteScreenUiAction.ShowNotInsideMetroError)
                    }

                    else -> Unit
                }
            }

            LaunchedEffect(Unit){
                adsController?.preload()
            }

            BackHandler{
                onNavigateUp()
            }

            RouteScreen(
                state = state,
                liveLocationUi = liveLocationUi,
                locationAccess = providerState,
                onAction = {
                    routeViewModel.onAction(it)
                    if (it is RouteScreenUiAction.GoBack) {
                        onNavigateUp()
                    }
                },
                onBack = onNavigateUp,
                onViewOnMap = {
                    state.routeResultUi?.let { route ->
                        onNavigateToMap?.invoke(route.sourceStation.id, route.destinationStation.id)
                    }
                },
                showInAppReview = {
                    showInAppReview()
                },
                onShare = {
                    intentUtils.onShareMetroRoute(it)
                },
            )
        }

    }


    fun toRouteScreenRoute(): RouteScreenRouteUi {
        return RouteScreenRouteUi(
            sourceId = sourceId,
            destId = destId,
            isRecent = isRecent
        )
    }

}

@Serializable
data class MapScreenRoute(
    val initialStationId: Long? = null,
    val sourceId: Long? = null,
    val destId: Long? = null
) {
    companion object {
        @Composable
        fun Invoke(
            mapScreenRoute: MapScreenRoute = MapScreenRoute(),
            onNavigateUp: () -> Unit,
            onNavigateToRoute: (Long, Long) -> Unit
        ) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val mapViewModel: com.codeancy.metroui.map.MapViewModel = koinViewModel(
                parameters = {
                    parametersOf(mapScreenRoute)
                }
            )
            val state by mapViewModel.state.collectAsStateWithLifecycle()

            com.codeancy.metroui.map.MapScreen(
                state = state,
                onAction = mapViewModel::onAction,
                onBack = onNavigateUp,
                onNavigateToRoute = onNavigateToRoute,
                onOpenExternalMap = { lat, lng, _ ->
                    try {
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                    } catch (_: Exception) {
                    }
                }
            )
        }
    }
}


@Serializable
data object PayrollScreenRoute {
    @Composable
    fun Invoke(
        onNavigateUp: () -> Unit,
    ) {
        PayrollScreen(
            onClose = { onNavigateUp() }
        )
    }

}
