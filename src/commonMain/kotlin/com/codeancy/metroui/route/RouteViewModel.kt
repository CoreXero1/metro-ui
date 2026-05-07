package com.codeancy.metroui.route

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeancy.metroui.domain.models.LiveLocationUi
import com.codeancy.metroui.domain.models.RouteResultUi
import com.codeancy.metroui.domain.repository.RouteRepository
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.AnalyticsParams
import com.codeancy.metroui.firebase.MetroConfigKey
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import com.codeancy.metroui.route.utils.RouteScreenRouteUi
import com.codeancy.metroui.route.utils.toLiveLocationUi
import com.codeancy.metroui.route.utils.toLiveLocationWithId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.corexero.sutradhar.datastore.DataStoreKey
import org.corexero.sutradhar.datastore.DataStoreManager
import org.corexero.sutradhar.location.LiveLocationRepository
import org.corexero.sutradhar.location.LocationRepository
import org.corexero.sutradhar.location.models.LocationProviderStatus
import org.corexero.sutradhar.remoteConfig.FirebaseRemoteConfig
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private data object RouteLiveLocationConfigKey : org.corexero.sutradhar.remoteConfig.ConfigKey<Boolean>(
    key = "LIVE_LOCATION",
    defaultValue = false
)

@Stable
data class RoutScreenState(
    val showProgress: Boolean = true,
    val routeResultUi: RouteResultUi? = null,
    val showInAppReview: Boolean = false,
    val showBookTicketTooltip: Boolean = false,
    val hasInitialized: Boolean = false,
    val isLiveLocationEnabled: Boolean = false,
    val showError: Boolean = false,
    val errorMessage: String = "",
    val isInterChangeFormatOpen: Boolean = true,
)

sealed interface RouteScreenUiAction {
    data object Init : RouteScreenUiAction
    data object GoBack : RouteScreenUiAction
    data object ShareScreenShot : RouteScreenUiAction

    data object DismissError : RouteScreenUiAction

    data object ShowNotInsideMetroError : RouteScreenUiAction

    data class ToggleLiveLocation(
        val isEnabled: Boolean
    ) : RouteScreenUiAction

    data object ToggleInterChangeFormat : RouteScreenUiAction
}

@OptIn(ExperimentalTime::class)
class RouteViewModel(
    private val routeRepository: RouteRepository,
    private val dataStoreManager: DataStoreManager,
    private val locationRepository: LocationRepository,
    private val liveLocationRepository: LiveLocationRepository,
    private val routeScreenRoute: RouteScreenRouteUi,
) : ViewModel() {


    private val _state: MutableStateFlow<RoutScreenState> =
        MutableStateFlow(RoutScreenState())

    val state: StateFlow<RoutScreenState>
        get() = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val providerState: StateFlow<LocationProviderStatus?> =
        state
            .map {
                if (isLiveLocationFeatureEnabled() && it.isLiveLocationEnabled && it.isInterChangeFormatOpen) {
                    it.routeResultUi
                } else {
                    null
                }
            }
            .flatMapLatest<RouteResultUi?, LocationProviderStatus?> { routeResultUi ->
                routeResultUi?.let {
                    locationRepository.getLocationAccess()
                } ?: flow { emit(null) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveLocationState: StateFlow<LiveLocationUi?> =
        providerState.combine(state) { providerState, state ->
            when (providerState) {
                LocationProviderStatus.ProviderEnabled -> {
                    if (isLiveLocationFeatureEnabled() && state.isLiveLocationEnabled && state.isInterChangeFormatOpen) {
                        state.routeResultUi
                    } else {
                        null
                    }
                }

                else -> null
            }
        }
            .flatMapLatest<RouteResultUi?, LiveLocationUi?> { routeResultUi ->
                routeResultUi?.let {
                    liveLocationRepository
                        .getLiveLocation(routeResultUi.toLiveLocationWithId())
                        .map { liveLocation ->
                            liveLocation.toLiveLocationUi()
                        }
                        .onStart {
                            emit(LiveLocationUi.Initializing)
                        }
                } ?: flow { emit(null) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    private fun isLiveLocationFeatureEnabled(): Boolean =
        FirebaseRemoteConfig.getBoolean(RouteLiveLocationConfigKey)

    private fun showNotInsideMetroError() {
        _state.update { currentState ->
            if (currentState.isLiveLocationEnabled) {
                currentState.copy(
                    showError = true,
                    errorMessage = NOT_INSIDE_METRO_ERROR,
                    isLiveLocationEnabled = false
                )
            } else currentState
        }
    }


    private fun init() {
        if (_state.value.hasInitialized) {
            return
        }
        _state.update {
            it.copy(hasInitialized = true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = Clock.System.now()
            val recentRouteResult = if (routeScreenRoute.isRecent) {
                routeRepository.getRecentSearch(
                    sourceId = routeScreenRoute.sourceId,
                    destinationId = routeScreenRoute.destId
                )
            } else {
                null
            }
            if (recentRouteResult != null) {
                _state.update {
                    it.copy(
                        routeResultUi = recentRouteResult.routeResult,
                        showProgress = false,
                    )
                }
                if (!recentRouteResult.hasPlatformUpdated) {
                    updatePlatFormDetails(recentRouteResult.routeResult)
                }
            } else {
                val metroResult = routeRepository.getRoute(
                    sourceId = routeScreenRoute.sourceId,
                    destinationId = routeScreenRoute.destId
                )
                _state.update {
                    it.copy(
                        routeResultUi = metroResult,
                        showProgress = false,
                    )
                }
                updatePlatFormDetails(metroResult)
            }
            FirebaseAnalyticsTracker.logEvent(
                eventName = AnalyticsEvents.ROUTE_LOAD_TIME,
                screenName = ScreenName.ROUTE_SCREEN,
                eventParams = routeAnalyticsParams(
                    viewType = if (_state.value.isInterChangeFormatOpen) {
                        ROUTE_VIEW_INTERCHANGE
                    } else {
                        ROUTE_VIEW_STATION_LIST
                    }
                ) + mapOf(AnalyticsParams.TIME to (Clock.System.now() - startTime) / 1000)
            )

            val tooltipCount = dataStoreManager.getFirst(DataStoreKey.BookTicketTooltipShowCount)
            if (tooltipCount < 3) {
                _state.update { it.copy(showBookTicketTooltip = true) }
                dataStoreManager.put(DataStoreKey.BookTicketTooltipShowCount, tooltipCount + 1)
            }

            //If app review is not show, show the app review
            if (FirebaseRemoteConfig.getBoolean(MetroConfigKey.EnableInAppReview) &&
                dataStoreManager.getFirst(DataStoreKey.ShowInAppReview)
            ) {
                delay(5000)
                _state.update {
                    it.copy(
                        showInAppReview = true
                    )
                }
                FirebaseAnalyticsTracker.logEvent(
                    eventName = AnalyticsEvents.REVIEW_POP_UP_SHOW,
                    screenName = ScreenName.ROUTE_SCREEN
                )
                dataStoreManager.put(DataStoreKey.ShowInAppReview, false)
            }
        }
    }

    private fun updatePlatFormDetails(
        routeResult: RouteResultUi
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    routeResultUi = routeRepository.updatePlatForms(routeResult)
                )
            }
        }
    }

    private fun showErrorMessage(errorMessage: String) {
        _state.update { currentState ->
            currentState.copy(
                showError = true,
                errorMessage = errorMessage
            )
        }
    }

    private fun routeAnalyticsParams(
        viewType: String? = null,
    ): Map<String, Any> {
        val routeResult = _state.value.routeResultUi
        return mutableMapOf<String, Any>(
            AnalyticsParams.SOURCE_ID to routeScreenRoute.sourceId,
            AnalyticsParams.DEST_ID to routeScreenRoute.destId,
        ).apply {
            routeResult?.let {
                put(AnalyticsParams.SOURCE_NAME, it.sourceStation.name.value)
                put(AnalyticsParams.DEST_NAME, it.destinationStation.name.value)
                put(AnalyticsParams.STATIONS, it.stations)
                put(AnalyticsParams.INTERCHANGES, it.interchanges)
                put(AnalyticsParams.FARE, it.fare)
            }
            viewType?.let {
                put(AnalyticsParams.VIEW_TYPE, it)
            }
        }
    }

    private fun logRouteEvent(
        eventName: String,
        viewType: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            FirebaseAnalyticsTracker.logEvent(
                eventName = eventName,
                screenName = ScreenName.ROUTE_SCREEN,
                eventParams = routeAnalyticsParams(viewType)
            )
        }
    }

    fun onAction(action: RouteScreenUiAction) {
        when (action) {

            RouteScreenUiAction.Init -> {
                init()
            }

            RouteScreenUiAction.ShareScreenShot -> {
                logRouteEvent(
                    eventName = AnalyticsEvents.ROUTE_SHARE_CLICKED,
                    viewType = if (_state.value.isInterChangeFormatOpen) {
                        ROUTE_VIEW_INTERCHANGE
                    } else {
                        ROUTE_VIEW_STATION_LIST
                    }
                )
            }

            RouteScreenUiAction.GoBack -> {
                _state.update {
                    it.copy(
                        showProgress = false
                    )
                }
            }

            is RouteScreenUiAction.ToggleLiveLocation -> {
                if (!isLiveLocationFeatureEnabled()) {
                    _state.update {
                        it.copy(isLiveLocationEnabled = false)
                    }
                    return
                }
                _state.update {
                    it.copy(
                        isLiveLocationEnabled = action.isEnabled
                    )
                }
                viewModelScope.launch(Dispatchers.Default) {
                    FirebaseAnalyticsTracker.logEvent(
                        eventName = if (action.isEnabled) AnalyticsEvents.LIVE_LOCATION_ENABLED else AnalyticsEvents.LIVE_LOCATION_DISABLED,
                        screenName = ScreenName.ROUTE_SCREEN,
                        eventParams = routeAnalyticsParams(
                            viewType = if (_state.value.isInterChangeFormatOpen) {
                                ROUTE_VIEW_INTERCHANGE
                            } else {
                                ROUTE_VIEW_STATION_LIST
                            }
                        )
                    )
                }
            }


            RouteScreenUiAction.DismissError -> {
                _state.update { currentState ->
                    currentState.copy(showError = false)
                }
            }

            RouteScreenUiAction.ShowNotInsideMetroError -> {
                viewModelScope.launch(Dispatchers.Default) {
                    FirebaseAnalyticsTracker.logEvent(
                        eventName = AnalyticsEvents.NOT_INSIDE_METRO_ERROR,
                        screenName = ScreenName.ROUTE_SCREEN,
                        eventParams = routeAnalyticsParams(
                            viewType = if (_state.value.isInterChangeFormatOpen) {
                                ROUTE_VIEW_INTERCHANGE
                            } else {
                                ROUTE_VIEW_STATION_LIST
                            }
                        )
                    )
                }
                showNotInsideMetroError()
            }

            RouteScreenUiAction.ToggleInterChangeFormat -> {
                val nextIsInterchangeView = !_state.value.isInterChangeFormatOpen
                _state.update { currentState ->
                    currentState.copy(
                        isInterChangeFormatOpen = nextIsInterchangeView
                    )
                }
                val viewType = if (nextIsInterchangeView) {
                    ROUTE_VIEW_INTERCHANGE
                } else {
                    ROUTE_VIEW_STATION_LIST
                }
                logRouteEvent(
                    eventName = if (nextIsInterchangeView) {
                        AnalyticsEvents.ROUTE_VIEW_INTERCHANGE
                    } else {
                        AnalyticsEvents.ROUTE_VIEW_STATION_LIST
                    },
                    viewType = viewType
                )
            }
        }
    }

    companion object {
        private const val NOT_INSIDE_METRO_ERROR =
            "Live location starts only when you’re inside the metro"
        private const val ROUTE_VIEW_INTERCHANGE = "interchange"
        private const val ROUTE_VIEW_STATION_LIST = "station_list"
    }
}
