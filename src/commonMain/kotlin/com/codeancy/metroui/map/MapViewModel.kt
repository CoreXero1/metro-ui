package com.codeancy.metroui.map

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeancy.metroui.app.MapScreenRoute
import com.codeancy.metroui.domain.models.LocationUi
import com.codeancy.metroui.domain.models.MapStationUi
import com.codeancy.metroui.domain.models.MetroMapDataUi
import com.codeancy.metroui.domain.models.RouteResultUi
import com.codeancy.metroui.domain.models.StationUi
import com.codeancy.metroui.domain.repository.RouteRepository
import com.codeancy.metroui.domain.repository.StationRepository
import com.codeancy.metroui.domain.utils.runCatchingOrNull
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.AnalyticsParams
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.corexero.sutradhar.location.LocationRepository

enum class RouteField {
    SOURCE,
    DESTINATION
}

data class MapScreenState(
    val mapData: MetroMapDataUi = MetroMapDataUi(),
    val isLoading: Boolean = true,
    val selectedLineId: Int? = null,
    val selectedStation: MapStationUi? = null,
    val selectedStationFirstLastTime: Pair<String, String>? = null,
    val sourceStation: MapStationUi? = null,
    val destinationStation: MapStationUi? = null,
    val activeRouteField: RouteField? = null,
    val searchQuery: String = "",
    val searchSuggestions: List<MapStationUi> = emptyList(),
    val routeOverlay: List<Long> = emptyList(),
    val routeResultUi: RouteResultUi? = null,
    val userLocationStation: MapStationUi? = null,
    val focusTarget: Offset? = null,
    val pendingSourceStation: MapStationUi? = null,
    val navigationEvent: MapNavigationEvent? = null
)

sealed interface MapNavigationEvent {
    data class NavigateToRoute(val sourceId: Long, val destId: Long) : MapNavigationEvent
    data class OpenExternalMap(val lat: Double, val lng: Double, val label: String) : MapNavigationEvent
}

sealed interface MapUiAction {
    data class SelectStation(val station: MapStationUi?) : MapUiAction
    data class FilterLine(val lineId: Int?) : MapUiAction
    data class FocusRouteField(val field: RouteField?) : MapUiAction
    data class ChangeSearchQuery(val query: String) : MapUiAction
    data class SelectSearchResult(val station: MapStationUi) : MapUiAction
    data object SwapSourceAndDestination : MapUiAction
    data class ClearRouteField(val field: RouteField) : MapUiAction
    data object ClearRouteOverlay : MapUiAction
    data object LocateUser : MapUiAction
    data object DismissBottomSheet : MapUiAction
    data class SetAsSource(val station: MapStationUi) : MapUiAction
    data class SetAsDestination(val station: MapStationUi) : MapUiAction
    data class OpenDirections(val station: MapStationUi) : MapUiAction
    data class ViewRouteDetails(val sourceId: Long, val destId: Long) : MapUiAction
    data object ResetFocusTarget : MapUiAction
    data object ConsumeNavigationEvent : MapUiAction
}

class MapViewModel(
    private val stationRepository: StationRepository,
    private val routeRepository: RouteRepository,
    private val locationRepository: LocationRepository,
    private val mapScreenRoute: MapScreenRoute = MapScreenRoute()
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state: StateFlow<MapScreenState> = _state.asStateFlow()

    init {
        loadMapData()
        logMapScreenVisit()
    }

    fun onAction(action: MapUiAction) {
        when (action) {
            is MapUiAction.SelectStation -> handleSelectStation(action.station)
            is MapUiAction.FilterLine -> handleFilterLine(action.lineId)
            is MapUiAction.FocusRouteField -> handleFocusRouteField(action.field)
            is MapUiAction.ChangeSearchQuery -> handleSearchQueryChanged(action.query)
            is MapUiAction.SelectSearchResult -> handleSelectSearchResult(action.station)
            MapUiAction.SwapSourceAndDestination -> handleSwapSourceAndDestination()
            is MapUiAction.ClearRouteField -> handleClearRouteField(action.field)
            MapUiAction.ClearRouteOverlay -> handleClearRoute()
            MapUiAction.LocateUser -> handleLocateUser()
            MapUiAction.DismissBottomSheet -> handleDismissBottomSheet()
            is MapUiAction.SetAsSource -> handleSetAsSource(action.station)
            is MapUiAction.SetAsDestination -> handleSetAsDestination(action.station)
            is MapUiAction.OpenDirections -> handleOpenDirections(action.station)
            is MapUiAction.ViewRouteDetails -> handleViewRouteDetails(action.sourceId, action.destId)
            MapUiAction.ResetFocusTarget -> _state.update { it.copy(focusTarget = null) }
            MapUiAction.ConsumeNavigationEvent -> _state.update { it.copy(navigationEvent = null) }
        }
    }

    private fun loadMapData() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            try {
                val mapData = stationRepository.getMetroMapData()
                _state.update {
                    it.copy(
                        mapData = mapData,
                        isLoading = false
                    )
                }

                // Check if initial source and destination were passed
                val initialSourceId = mapScreenRoute.sourceId
                val initialDestId = mapScreenRoute.destId

                if (initialSourceId != null && initialDestId != null) {
                    val src = mapData.stations.find { it.id == initialSourceId }
                    val dst = mapData.stations.find { it.id == initialDestId }
                    _state.update {
                        it.copy(
                            sourceStation = src,
                            destinationStation = dst
                        )
                    }
                    loadRoute(initialSourceId, initialDestId)
                } else if (mapScreenRoute.initialStationId != null) {
                    val station = mapData.stations.find { it.id == mapScreenRoute.initialStationId }
                    if (station != null) {
                        _state.update { it.copy(sourceStation = station) }
                        handleSelectStation(station)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleSelectStation(station: MapStationUi?) {
        val currentActiveField = _state.value.activeRouteField

        if (station != null) {
            if (currentActiveField == RouteField.SOURCE) {
                logSourceSelected(station)
                _state.update {
                    it.copy(
                        sourceStation = station,
                        activeRouteField = null,
                        searchQuery = "",
                        searchSuggestions = emptyList(),
                        focusTarget = Offset(station.x, station.y)
                    )
                }
                checkAndCalculateRoute()
                return
            } else if (currentActiveField == RouteField.DESTINATION) {
                logDestSelected(station)
                _state.update {
                    it.copy(
                        destinationStation = station,
                        activeRouteField = null,
                        searchQuery = "",
                        searchSuggestions = emptyList(),
                        focusTarget = Offset(station.x, station.y)
                    )
                }
                checkAndCalculateRoute()
                return
            }

            // Quick map click assignment
            val currentSource = _state.value.sourceStation
            val currentDest = _state.value.destinationStation

            if (currentSource == null) {
                logSourceSelected(station)
                _state.update {
                    it.copy(
                        sourceStation = station,
                        selectedStation = station,
                        focusTarget = Offset(station.x, station.y)
                    )
                }
                loadStationTimings(station)
            } else if (currentDest == null && currentSource.id != station.id) {
                logDestSelected(station)
                _state.update {
                    it.copy(
                        destinationStation = station,
                        selectedStation = null,
                        focusTarget = Offset(station.x, station.y)
                    )
                }
                loadRoute(currentSource.id, station.id)
            } else {
                _state.update {
                    it.copy(
                        selectedStation = station,
                        focusTarget = Offset(station.x, station.y)
                    )
                }
                loadStationTimings(station)
            }
        } else {
            _state.update {
                it.copy(
                    selectedStation = null,
                    selectedStationFirstLastTime = null
                )
            }
        }
    }

    private fun loadStationTimings(station: MapStationUi) {
        viewModelScope.launch(Dispatchers.IO) {
            val stationUi = StationUi(
                id = station.id,
                name = com.codeancy.metroui.common.utils.UiText.DynamicString(station.name),
                description = null,
                platform = null,
                time = 0L,
                colorHex = station.colorHex,
                lineName = station.lineName,
                code = station.code,
                locationUi = LocationUi(lat = station.latitude, long = station.longitude)
            )

            val timings = stationRepository.getFirstAndLastMetroTime(stationUi, stationUi)
            _state.update { it.copy(selectedStationFirstLastTime = timings) }
        }
    }

    private fun handleFilterLine(lineId: Int?) {
        _state.update { it.copy(selectedLineId = lineId) }
        if (lineId != null) {
            val lineStations = _state.value.mapData.stations.filter { it.lineId == lineId }
            if (lineStations.isNotEmpty()) {
                val midX = lineStations.map { it.x }.average().toFloat()
                val midY = lineStations.map { it.y }.average().toFloat()
                _state.update { it.copy(focusTarget = Offset(midX, midY)) }
            }
        }
    }

    private fun handleFocusRouteField(field: RouteField?) {
        _state.update {
            it.copy(
                activeRouteField = field,
                searchQuery = "",
                searchSuggestions = emptyList()
            )
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        val suggestions = if (trimmed.length >= 2) {
            _state.value.mapData.stations.filter { st ->
                st.name.contains(trimmed, ignoreCase = true) ||
                    st.hindiName.contains(trimmed, ignoreCase = true) ||
                    st.code?.contains(trimmed, ignoreCase = true) == true ||
                    st.lineName.contains(trimmed, ignoreCase = true)
            }.take(8)
        } else {
            emptyList()
        }

        _state.update {
            it.copy(
                searchQuery = query,
                searchSuggestions = suggestions
            )
        }
    }

    private fun handleSelectSearchResult(station: MapStationUi) {
        val field = _state.value.activeRouteField
        if (field == RouteField.SOURCE) {
            logSourceSelected(station)
            _state.update {
                it.copy(
                    sourceStation = station,
                    activeRouteField = null,
                    searchQuery = "",
                    searchSuggestions = emptyList(),
                    focusTarget = Offset(station.x, station.y)
                )
            }
            checkAndCalculateRoute()
        } else if (field == RouteField.DESTINATION) {
            logDestSelected(station)
            _state.update {
                it.copy(
                    destinationStation = station,
                    activeRouteField = null,
                    searchQuery = "",
                    searchSuggestions = emptyList(),
                    focusTarget = Offset(station.x, station.y)
                )
            }
            checkAndCalculateRoute()
        } else {
            handleSelectStation(station)
        }
    }

    private fun handleSwapSourceAndDestination() {
        val oldSource = _state.value.sourceStation
        val oldDest = _state.value.destinationStation

        _state.update {
            it.copy(
                sourceStation = oldDest,
                destinationStation = oldSource
            )
        }

        if (oldDest != null && oldSource != null) {
            loadRoute(oldDest.id, oldSource.id)
        }
    }

    private fun handleClearRouteField(field: RouteField) {
        when (field) {
            RouteField.SOURCE -> {
                _state.update {
                    it.copy(
                        sourceStation = null,
                        routeOverlay = emptyList(),
                        routeResultUi = null
                    )
                }
            }
            RouteField.DESTINATION -> {
                _state.update {
                    it.copy(
                        destinationStation = null,
                        routeOverlay = emptyList(),
                        routeResultUi = null
                    )
                }
            }
        }
    }

    private fun handleClearRoute() {
        _state.update {
            it.copy(
                sourceStation = null,
                destinationStation = null,
                routeOverlay = emptyList(),
                routeResultUi = null,
                selectedStation = null
            )
        }
    }

    private fun checkAndCalculateRoute() {
        val src = _state.value.sourceStation
        val dst = _state.value.destinationStation
        if (src != null && dst != null && src.id != dst.id) {
            loadRoute(src.id, dst.id)
        }
    }

    private fun loadRoute(sourceId: Long, destinationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val routeResult = routeRepository.getRoute(sourceId, destinationId)
                val allRouteStationIds = mutableListOf<Long>()
                for (ic in routeResult.interchange) {
                    if (allRouteStationIds.isEmpty()) {
                        allRouteStationIds.add(ic.sourceStation.id)
                    }
                    for (st in ic.inBetweenStations) {
                        allRouteStationIds.add(st.id)
                    }
                    allRouteStationIds.add(ic.destinationStation.id)
                }

                FirebaseAnalyticsTracker.logEvent(
                    eventName = AnalyticsEvents.MAP_GET_ROUTE,
                    screenName = ScreenName.MAP_SCREEN,
                    eventParams = mapOf(
                        AnalyticsParams.SOURCE_ID to sourceId,
                        AnalyticsParams.DEST_ID to destinationId,
                        AnalyticsParams.STATIONS to routeResult.stations,
                        AnalyticsParams.INTERCHANGES to routeResult.interchanges,
                        AnalyticsParams.FARE to routeResult.fare
                    )
                )

                _state.update {
                    it.copy(
                        routeOverlay = allRouteStationIds,
                        routeResultUi = routeResult
                    )
                }
            } catch (e: Exception) {
                // If route lookup fails, fall back to endpoints
                _state.update {
                    it.copy(
                        routeOverlay = listOf(sourceId, destinationId),
                        routeResultUi = null
                    )
                }
            }
        }
    }

    private fun handleLocateUser() {
        viewModelScope.launch(Dispatchers.IO) {
            if (locationRepository.hasLocationPermission()) {
                val loc = runCatchingOrNull { locationRepository.getLocation() }
                if (loc != null) {
                    val nearestList = stationRepository.getNearestMetroStations(
                        LocationUi(lat = loc.lat, long = loc.long)
                    )
                    val nearestId = nearestList.firstOrNull()?.stationId
                    if (nearestId != null) {
                        val nearestStation = _state.value.mapData.stations.find { it.id == nearestId }
                        if (nearestStation != null) {
                            _state.update {
                                it.copy(
                                    userLocationStation = nearestStation,
                                    selectedStation = nearestStation,
                                    focusTarget = Offset(nearestStation.x, nearestStation.y)
                                )
                            }
                        }
                    }
                }
            } else {
                locationRepository.openLocationSettings()
            }
        }
    }

    private fun handleDismissBottomSheet() {
        _state.update { it.copy(selectedStation = null) }
    }

    private fun handleSetAsSource(station: MapStationUi) {
        logSourceSelected(station)
        _state.update {
            it.copy(
                sourceStation = station,
                selectedStation = null
            )
        }
        checkAndCalculateRoute()
    }

    private fun handleSetAsDestination(station: MapStationUi) {
        logDestSelected(station)
        _state.update {
            it.copy(
                destinationStation = station,
                selectedStation = null
            )
        }
        checkAndCalculateRoute()
    }

    private fun handleOpenDirections(station: MapStationUi) {
        if (station.latitude > 0.0 && station.longitude > 0.0) {
            _state.update {
                it.copy(
                    navigationEvent = MapNavigationEvent.OpenExternalMap(
                        lat = station.latitude,
                        lng = station.longitude,
                        label = station.name
                    )
                )
            }
        }
    }

    private fun handleViewRouteDetails(sourceId: Long, destId: Long) {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.MAP_VIEW_ROUTE_DETAILS,
            screenName = ScreenName.MAP_SCREEN,
            eventParams = mapOf(
                AnalyticsParams.SOURCE_ID to sourceId,
                AnalyticsParams.DEST_ID to destId
            )
        )
        _state.update {
            it.copy(
                navigationEvent = MapNavigationEvent.NavigateToRoute(sourceId, destId)
            )
        }
    }

    private fun logSourceSelected(station: MapStationUi) {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.MAP_SOURCE_SELECT,
            screenName = ScreenName.MAP_SCREEN,
            eventParams = mapOf(
                AnalyticsParams.SOURCE_ID to station.id,
                AnalyticsParams.SOURCE_NAME to station.name
            )
        )
    }

    private fun logDestSelected(station: MapStationUi) {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.MAP_DEST_SELECT,
            screenName = ScreenName.MAP_SCREEN,
            eventParams = mapOf(
                AnalyticsParams.DEST_ID to station.id,
                AnalyticsParams.DEST_NAME to station.name
            )
        )
    }

    private fun logMapScreenVisit() {
        val entrySource = if (mapScreenRoute.sourceId != null && mapScreenRoute.destId != null) {
            "route_nudge"
        } else if (mapScreenRoute.initialStationId != null) {
            "station_action"
        } else {
            "home_quick_action"
        }

        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.METRO_MAP,
            screenName = ScreenName.MAP_SCREEN,
            eventParams = mapOf(
                AnalyticsParams.ENTRY_SOURCE to entrySource
            )
        )
    }
}
