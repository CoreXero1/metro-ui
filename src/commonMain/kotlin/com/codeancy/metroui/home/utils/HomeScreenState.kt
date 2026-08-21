package com.codeancy.metroui.home.utils

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.codeancy.metroui.domain.models.AppUpdateUi
import com.codeancy.metroui.domain.models.NearestMetroStationUi
import com.codeancy.metroui.domain.models.RouteResultUi
import com.codeancy.metroui.domain.models.StationUi

@Stable
data class HomeScreenState(
    val sourceTextFieldValue: TextFieldValue = TextFieldValue(),
    val destinationTextFieldValue: TextFieldValue = TextFieldValue(),
    val source: StationUi? = null,
    val destination: StationUi? = null,
    val allStation: List<StationUi> = emptyList(),
    val showProgress: Boolean = false,
    val showError: Boolean = false,
    val errorMessage: String = "",
    val recentRouteResults: List<RouteResultUi> = emptyList(),
    val nearestMetroState: NearestMetroState? = null,
    val lastMetroTimingState: LastMetroTimingState? = null,
    val appUpdateUi: AppUpdateUi? = null
) {

    sealed interface NearestMetroState {
        data object Loading : NearestMetroState
        data object ShowRationale : NearestMetroState
        data class NearestMetroStations(
            val stations: List<NearestMetroStationUi>
        ) : NearestMetroState
    }

    sealed interface LastMetroTimingState {

        data class StationSelect(
            val destination: StationUi? = null,
            val source: StationUi? = null,
            val destinationString: TextFieldValue = TextFieldValue(),
            val sourceString: TextFieldValue = TextFieldValue(),
            val stations: List<StationUi>
        ) : LastMetroTimingState

        data class Loading(
            val source: StationUi,
            val destination: StationUi,
        ) : LastMetroTimingState

        data class LastMetroTime(
            val source: StationUi,
            val destination: StationUi,
            val firstMetroTime: String,
            val lastMetroTime: String
        ) : LastMetroTimingState

    }

}
