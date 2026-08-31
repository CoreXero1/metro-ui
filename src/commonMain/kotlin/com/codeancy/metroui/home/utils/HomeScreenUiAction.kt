package com.codeancy.metroui.home.utils

import androidx.compose.ui.text.input.TextFieldValue
import com.codeancy.metroui.domain.models.AppUpdateUi
import com.codeancy.metroui.domain.models.NearestMetroStationUi
import com.codeancy.metroui.domain.models.StationUi

sealed interface HomeScreenUiAction {
    data class ChangeSource(val value: TextFieldValue) : HomeScreenUiAction
    data class ChangeDestination(val value: TextFieldValue) : HomeScreenUiAction
    data class OnSelectSource(val stationUi: StationUi?) : HomeScreenUiAction
    data class OnSelectDestination(val stationUi: StationUi?) : HomeScreenUiAction

    data object GetRouteError : HomeScreenUiAction

    data class GetRoute(
        val sourceId: Long,
        val destId: Long,
        val isRecent: Boolean
    ) : HomeScreenUiAction

    data object SwapSourceAndDestination : HomeScreenUiAction
    data object DismissProgress : HomeScreenUiAction
    data object DismissError : HomeScreenUiAction
    data object ShareOnWhatsApp : HomeScreenUiAction
    data object OnMetroMapClick : HomeScreenUiAction
    data object OnNearestMetroClick : HomeScreenUiAction
    data object OnDismissNeatestMetro : HomeScreenUiAction

    data object OnLocationPermissionGranted : HomeScreenUiAction

    data object OnLocationPermissionDenied : HomeScreenUiAction

    data class OnSelectNearestMetroStationUi(
        val metroStationUi: NearestMetroStationUi
    ) : HomeScreenUiAction

    data object OnLastMetroClick : HomeScreenUiAction

    data class OnLastMetroChangeSource(
        val value: TextFieldValue
    ) : HomeScreenUiAction

    data class OnLastMetroChangeDestination(
        val value: TextFieldValue
    ) : HomeScreenUiAction

    data class OnLastMetroSelectSource(
        val stationUi: StationUi?
    ) : HomeScreenUiAction

    data class OnLastMetroSelectDestination(
        val stationUi: StationUi?
    ) : HomeScreenUiAction

    data object OnLastMetroStationSwap : HomeScreenUiAction

    data object OnGetLastMetroTiming : HomeScreenUiAction

    data object OnDismissLastMetroTiming : HomeScreenUiAction

    data class ShowError(
        val message: String
    ) : HomeScreenUiAction

    data class DismissAppUpdate(
        val appUpdateUi: AppUpdateUi
    ) : HomeScreenUiAction

    data class OnPremiumClick(val entrySource: String = "home_header") : HomeScreenUiAction
    data object DismissPaywall : HomeScreenUiAction

}
