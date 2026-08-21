package com.codeancy.metroui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.codeancy.metroui.common.components.SnackBar
import com.codeancy.metroui.common.components.StatusBarColor
import com.codeancy.metroui.common.utils.MetroUiColor
import com.codeancy.metroui.common.utils.MetroConfig
import com.codeancy.metroui.home.components.Disclaimer
import com.codeancy.metroui.home.components.HomeScreenHeader
import com.codeancy.metroui.home.components.LastMetroTiming
import com.codeancy.metroui.home.components.NearestMetro
import com.codeancy.metroui.home.components.QuickAccess
import com.codeancy.metroui.home.components.RecentRouteResults
import com.codeancy.metroui.home.components.StationSelectionCard
import com.codeancy.metroui.home.components.UpdateDialog
import com.codeancy.metroui.home.components.rememberHardUpdateModel
import com.codeancy.metroui.home.components.rememberSoftUpdateModel
import com.codeancy.metroui.home.utils.HomeScreenState
import com.codeancy.metroui.home.utils.HomeScreenUiAction
import com.corexero.dhan_tantra.sdk.presentation.PayrollScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeScreenState,
    onAction: (HomeScreenUiAction) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroUiColor.background)
            .verticalScroll(rememberScrollState()),
    ) {

        StatusBarColor(isLightStatusBar = false)

        HomeScreenHeader(
            title = MetroConfig.appTitle,
            selectedCity = MetroConfig.selectedCity,
            supportedCities = MetroConfig.supportedCities,
            onCitySelected = MetroConfig.onCitySelected,
            modifier = Modifier
                .fillMaxWidth(),
            onPremiumClicked = {
                onAction(HomeScreenUiAction.OnPremiumClick)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        StationSelectionCard(
            source = state.sourceTextFieldValue,
            destination = state.destinationTextFieldValue,
            allStations = state.allStation,
            onSourceChanged = {
                onAction(HomeScreenUiAction.ChangeSource(it))
            },
            onDestinationChanged = {
                onAction(HomeScreenUiAction.ChangeDestination(it))
            },
            onGetRouteClick = {
                state.source?.id?.let { source ->
                    state.destination?.id?.let { dest ->
                        onAction(
                            HomeScreenUiAction.GetRoute(
                                sourceId = source,
                                destId = dest,
                                isRecent = false
                            )
                        )
                    } ?: run {
                        onAction(HomeScreenUiAction.GetRouteError)
                    }
                } ?: run {
                    onAction(HomeScreenUiAction.GetRouteError)
                }
                keyboardController?.hide()
            },
            onSwap = {
                onAction(HomeScreenUiAction.SwapSourceAndDestination)
            },
            onSourceStationSelected = {
                onAction(HomeScreenUiAction.OnSelectSource(it))
            },
            onDestinationStationSelected = {
                onAction(HomeScreenUiAction.OnSelectDestination(it))
            },
            recentRouteResults = state.recentRouteResults,
            modifier = Modifier
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val uriHandler = LocalUriHandler.current

        QuickAccess(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            visibleItems = buildSet {
                add(QuickAccess.Map)
                if (MetroConfig.showBookTicket) add(QuickAccess.BOOK_TICKET)
                if (MetroConfig.showNearestMetro) add(QuickAccess.NearestMetro)
                if (MetroConfig.showTimings) add(QuickAccess.Timings)
            }
        ) { quickAccess ->
            when (quickAccess) {
                QuickAccess.Map -> {
                    onAction(HomeScreenUiAction.OnMetroMapClick)
                }

                QuickAccess.BOOK_TICKET -> {
                    try {
                        uriHandler.openUri("https://wa.me/+919650855800?text=Hi")
                    } catch (_: IllegalArgumentException) {
                        onAction(HomeScreenUiAction.ShowError("Please install Whatsapp"))
                    }
                    onAction(HomeScreenUiAction.ShareOnWhatsApp)
                }

                QuickAccess.NearestMetro -> {
                    onAction(HomeScreenUiAction.OnNearestMetroClick)
                }

                QuickAccess.Timings -> {
                    onAction(HomeScreenUiAction.OnLastMetroClick)
                }
            }
        }



        NearestMetro(
            state = state.nearestMetroState,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
        )

        LastMetroTiming(
            lastMetroTimingState = state.lastMetroTimingState,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.recentRouteResults.isNotEmpty()) {
            RecentRouteResults(
                routeResultsUi = state.recentRouteResults,
                onClick = {
                    onAction(
                        HomeScreenUiAction.GetRoute(
                            sourceId = it.sourceStation.id,
                            destId = it.destinationStation.id,
                            isRecent = true
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

        state.appUpdateUi?.let { appUpdate ->
            val updateUiModel =
                if (appUpdate.isHardUpdate) rememberHardUpdateModel() else rememberSoftUpdateModel()
            UpdateDialog(
                model = updateUiModel,
                onPrimary = {
                    uriHandler.openUri(appUpdate.appUrl)
                },
                onSecondary = {
                    onAction(HomeScreenUiAction.DismissAppUpdate(appUpdate))
                },
                onDismissRequest = {
                    if (!appUpdate.isHardUpdate) {
                        onAction(HomeScreenUiAction.DismissAppUpdate(appUpdate))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(8.dp))

        Disclaimer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

    }

    SnackBar(
        state.showError,
        message = state.errorMessage,
        onDismiss = {
            onAction(HomeScreenUiAction.DismissError)
        },
    )

}
