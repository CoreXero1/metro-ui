package com.codeancy.metroui.home

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeancy.metroui.app.HomeScreenRoute
import com.codeancy.metroui.domain.models.AppUpdateUi
import com.codeancy.metroui.domain.models.LocationUi
import com.codeancy.metroui.domain.models.StationUi
import com.codeancy.metroui.domain.repository.AppUpdateRepository
import com.codeancy.metroui.domain.repository.RouteRepository
import com.codeancy.metroui.domain.repository.StationRepository
import com.codeancy.metroui.domain.utils.runCatchingOrNull
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.AnalyticsParams
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import com.codeancy.metroui.home.utils.HomeScreenState
import com.codeancy.metroui.home.utils.HomeScreenUiAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.corexero.sutradhar.appConfig.AppConfigurationProvider
import org.corexero.sutradhar.datastore.DataStoreKey
import org.corexero.sutradhar.datastore.DataStoreManager
import org.corexero.sutradhar.location.LocationRepository
import org.corexero.sutradhar.network.repository.SutradharRepository

class HomeViewModel(
    private val stationRepository: StationRepository,
    private val routeRepository: RouteRepository,
    private val locationRepository: LocationRepository,
    private val sutradharRepository: SutradharRepository,
    private val dataStoreManager: DataStoreManager,
    private val appUpdateRepository: AppUpdateRepository,
    private val appConfigurationProvider: AppConfigurationProvider,
    homeScreenRoute: HomeScreenRoute
) : ViewModel() {

    private val appConfiguration by lazy {
        appConfigurationProvider.getAppConfiguration()
    }

    private val _homeScreenState = MutableStateFlow(HomeScreenState())
    val homeScreenState: StateFlow<HomeScreenState> = _homeScreenState

    private lateinit var allStationList: List<StationUi>

    // Job management for coroutines
    private var nearestMetroJob: Job? = null
    private var lastMetroJob: Job? = null

    init {
        initAppUpdate()
        initializeStationList()
        observeRecentSearches()
    }

    // Public action handler
    fun onAction(action: HomeScreenUiAction) = viewModelScope.launch(Dispatchers.IO) {
        when (action) {
            is HomeScreenUiAction.ChangeSource -> handleChangeSource(action)
            is HomeScreenUiAction.ChangeDestination -> handleChangeDestination(action)
            is HomeScreenUiAction.GetRoute -> handleGetRoute(action)
            HomeScreenUiAction.GetRouteError -> showGetRouteError()
            is HomeScreenUiAction.SwapSourceAndDestination -> handleSwapSourceAndDestination()
            is HomeScreenUiAction.DismissProgress -> handleDismissProgress()
            is HomeScreenUiAction.DismissError -> handleDismissError()
            is HomeScreenUiAction.ShareOnWhatsApp -> handleShareOnWhatsApp()
            is HomeScreenUiAction.OnMetroMapClick -> handleMetroMapClick()
            HomeScreenUiAction.OnNearestMetroClick -> handleNearestMetroClick()
            HomeScreenUiAction.OnDismissNeatestMetro -> handleDismissNearestMetro()
            HomeScreenUiAction.OnLocationPermissionDenied -> handleLocationPermissionDenied()
            HomeScreenUiAction.OnLocationPermissionGranted -> handleLocationPermissionGranted()
            is HomeScreenUiAction.OnSelectNearestMetroStationUi -> handleSelectNearestMetroStation(
                action
            )

            HomeScreenUiAction.OnLastMetroClick -> handleLastMetroClick()
            HomeScreenUiAction.OnDismissLastMetroTiming -> handleDismissLastMetroTiming()
            HomeScreenUiAction.OnGetLastMetroTiming -> handleGetLastMetroTiming()
            is HomeScreenUiAction.OnLastMetroChangeDestination -> handleLastMetroChangeDestination(
                action
            )

            is HomeScreenUiAction.OnLastMetroChangeSource -> handleLastMetroChangeSource(action)
            is HomeScreenUiAction.OnLastMetroStationSwap -> handleLastMetroStationSwap()
            is HomeScreenUiAction.ShowError -> {
                showErrorMessage(action.message)
            }

            is HomeScreenUiAction.DismissAppUpdate -> dismissAppUpdate(action.appUpdateUi)
            is HomeScreenUiAction.OnLastMetroSelectDestination -> handleLastMetroSelectDestination(
                action
            )

            is HomeScreenUiAction.OnLastMetroSelectSource -> handleLastMetroSelectSource(action)
            is HomeScreenUiAction.OnSelectDestination -> handleDestinationStationSelect(action)

            is HomeScreenUiAction.OnSelectSource -> handleSourceStationSelect(action)
            HomeScreenUiAction.OnPremiumClick -> handlePremiumClick()
        }
    }

    private fun handlePremiumClick() {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.PREMIUM,
            screenName = ScreenName.HOME_SCREEN,
        )
    }

    private fun initAppUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            appUpdateRepository.getAppUpdateInfo()?.let { appUpdate ->
                _homeScreenState.update {
                    if (appUpdate.isHardUpdate) {
                        it.copy(appUpdateUi = appUpdate)
                    } else {
                        val dismissedVersion =
                            dataStoreManager.getFirst(DataStoreKey.LastSoftUpdateDismissedVersion)
                        if (dismissedVersion == appUpdate.newVersion) {
                            it
                        } else {
                            it.copy(appUpdateUi = appUpdate)
                        }
                    }
                }
            }
        }
    }

    private fun dismissAppUpdate(appUpdate: AppUpdateUi) {
        viewModelScope.launch(Dispatchers.IO) {
            _homeScreenState.update { it.copy(appUpdateUi = null) }
            dataStoreManager.put(
                DataStoreKey.LastSoftUpdateDismissedVersion,
                appUpdate.newVersion
            )
        }
    }

    // Initialization methods
    private fun initializeStationList() {
        viewModelScope.launch(Dispatchers.IO) {
            allStationList = stationRepository.getAllStations()
            _homeScreenState.update { currentState ->
                currentState.copy(
                    allStation = allStationList
                )
            }
        }
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            routeRepository.getRecentSearches()
                .flowOn(Dispatchers.IO)
                .collect { routeResults ->
                    _homeScreenState.update { currentState ->
                        currentState.copy(
                            recentRouteResults = routeResults.map { it.routeResult }
                        )
                    }
                }
        }
    }

    // Source and Destination handlers
    private fun handleChangeSource(action: HomeScreenUiAction.ChangeSource) {
        _homeScreenState.update { currentState ->
            currentState.copy(sourceTextFieldValue = action.value)
        }
    }

    private fun handleSourceStationSelect(action: HomeScreenUiAction.OnSelectSource) {
        _homeScreenState.update {
            it.copy(
                source = action.stationUi,
                sourceTextFieldValue = TextFieldValue(
                    action.stationUi?.name?.value ?: it.sourceTextFieldValue.text
                )
            )
        }
        logSourceSelectionEvent(action.stationUi)
    }

    private fun handleChangeDestination(action: HomeScreenUiAction.ChangeDestination) {
        _homeScreenState.update { currentState ->
            currentState.copy(destinationTextFieldValue = action.value)
        }
    }

    private fun handleDestinationStationSelect(action: HomeScreenUiAction.OnSelectDestination) {
        _homeScreenState.update {
            it.copy(
                destination = action.stationUi,
                destinationTextFieldValue = TextFieldValue(
                    action.stationUi?.name?.value ?: it.destinationTextFieldValue.text
                )
            )
        }

        logDestinationSelectionEvent(action.stationUi)
    }

    private fun handleSwapSourceAndDestination() {
        _homeScreenState.update { currentState ->
            currentState.copy(
                sourceTextFieldValue = currentState.destinationTextFieldValue,
                destinationTextFieldValue = currentState.sourceTextFieldValue,
                source = currentState.destination,
                destination = currentState.source
            )
        }
    }

    // Route handling
    private fun handleGetRoute(action: HomeScreenUiAction.GetRoute) {
        val isValidRoute = validateRoute(action.sourceId, action.destId)
        logGetRouteEvent(action, isValidRoute)
    }

    private fun showGetRouteError() {
        validateRoute(
            _homeScreenState.value.source?.id,
            _homeScreenState.value.destination?.id
        )
    }

    // UI state handlers
    private fun handleDismissProgress() {
        _homeScreenState.update { currentState ->
            currentState.copy(showProgress = false)
        }
    }

    private fun handleDismissError() {
        _homeScreenState.update { currentState ->
            currentState.copy(showError = false)
        }
    }

    // Analytics handlers
    private fun handleShareOnWhatsApp() {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.BOOK_TICKET,
            screenName = ScreenName.HOME_SCREEN,
            eventParams = emptyMap()
        )
    }

    private fun handleMetroMapClick() {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.METRO_MAP,
            screenName = ScreenName.HOME_SCREEN
        )
    }

    // Nearest Metro handlers
    private fun handleNearestMetroClick() {
        if (locationRepository.hasLocationPermission()) {
            loadNearestMetroStations()
        } else {
            showLocationPermissionRationale()
        }
    }

    private fun handleDismissNearestMetro() {
        cancelNearestMetro()
    }

    private fun handleLocationPermissionDenied() {
        cancelNearestMetro()
    }

    private fun handleLocationPermissionGranted() {
        loadNearestMetroStations()
    }

    private fun handleSelectNearestMetroStation(action: HomeScreenUiAction.OnSelectNearestMetroStationUi) {
        if (!::allStationList.isInitialized) return

        val station = findStationById(action.metroStationUi.stationId)
        station?.let { selectedStation ->
            _homeScreenState.update { currentState ->
                currentState.copy(
                    source = selectedStation,
                    sourceTextFieldValue = TextFieldValue(selectedStation.name.value),
                    nearestMetroState = null
                )
            }
        }
    }

    // Last Metro handlers
    private fun handleLastMetroClick() {
        _homeScreenState.update { currentState ->
            currentState.copy(
                lastMetroTimingState = HomeScreenState.LastMetroTimingState.StationSelect(
                    stations = if (::allStationList.isInitialized) allStationList else emptyList()
                )
            )
        }
    }

    private fun handleDismissLastMetroTiming() {
        lastMetroJob?.cancel()
        _homeScreenState.update { currentState ->
            currentState.copy(lastMetroTimingState = null)
        }
    }

    private fun handleGetLastMetroTiming() {
        loadLastMetroTiming()
    }

    private fun handleLastMetroChangeDestination(
        action: HomeScreenUiAction.OnLastMetroChangeDestination
    ) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        destinationString = action.value
                    )
                )
            } else {
                currentState
            }
        }
    }

    private fun handleLastMetroSelectDestination(
        action: HomeScreenUiAction.OnLastMetroSelectDestination
    ) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        destination = action.stationUi,
                        destinationString = TextFieldValue(
                            action.stationUi?.name?.value ?: stationSelect.destinationString.text
                        )
                    )
                )
            } else {
                currentState
            }
        }
        logLastMetroDestinationSelectionEvent(action.stationUi)
    }

    private fun handleLastMetroChangeSource(action: HomeScreenUiAction.OnLastMetroChangeSource) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        sourceString = action.value
                    )
                )
            } else {
                currentState
            }
        }
    }

    private fun handleLastMetroSelectSource(action: HomeScreenUiAction.OnLastMetroSelectSource) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        source = action.stationUi,
                        sourceString = TextFieldValue(
                            action.stationUi?.name?.value ?: stationSelect.sourceString.text
                        )
                    )
                )
            } else {
                currentState
            }
        }
        logLastMetroSourceSelectionEvent(action.stationUi)
    }

    private fun handleLastMetroStationSwap() {
        _homeScreenState.update { currentState ->
            val metroState = currentState.lastMetroTimingState
            if (metroState is HomeScreenState.LastMetroTimingState.StationSelect) {
                currentState.copy(
                    lastMetroTimingState = metroState.copy(
                        source = metroState.destination,
                        sourceString = metroState.destinationString,
                        destination = metroState.source,
                        destinationString = metroState.sourceString
                    )
                )
            } else {
                currentState
            }
        }
    }

    // Helper methods for station operations
    private fun findStationById(stationId: Long): StationUi? {
        return if (::allStationList.isInitialized) {
            allStationList.find { it.id == stationId }
        } else null
    }

    private fun updateSourceStation(station: StationUi?) {
        _homeScreenState.update { currentState ->
            currentState.copy(source = station)
        }
    }

    private fun updateDestinationStation(station: StationUi?) {
        _homeScreenState.update { currentState ->
            currentState.copy(destination = station)
        }
    }

    private fun updateLastMetroDestinationStation(station: StationUi?) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        destination = station
                    )
                )
            } else {
                currentState
            }
        }
    }

    private fun updateLastMetroSourceStation(
        value: TextFieldValue,
        station: StationUi?
    ) {
        _homeScreenState.update { currentState ->
            val stationSelect =
                currentState.lastMetroTimingState as? HomeScreenState.LastMetroTimingState.StationSelect
            if (stationSelect != null) {
                currentState.copy(
                    lastMetroTimingState = stationSelect.copy(
                        sourceString = value,
                        source = station
                    )
                )
            } else {
                currentState
            }
        }
    }

    // Location and nearest metro operations
    private fun showLocationPermissionRationale() {
        _homeScreenState.update { currentState ->
            currentState.copy(nearestMetroState = HomeScreenState.NearestMetroState.ShowRationale)
        }
    }

    private fun loadNearestMetroStations() {
        _homeScreenState.update { currentState ->
            currentState.copy(nearestMetroState = HomeScreenState.NearestMetroState.Loading)
        }
        nearestMetroJob = viewModelScope.launch(Dispatchers.IO) {
            val location = runCatchingOrNull { locationRepository.getLocation() }

            if (location != null) {
                loadAndDisplayNearestStations(location.lat, location.long)
            } else {
                handleLocationError()
            }
        }
    }

    private suspend fun loadAndDisplayNearestStations(latitude: Double, longitude: Double) {
        val nearestMetroStations = stationRepository.getNearestMetroStations(
            locationUi = LocationUi(
                lat = latitude,
                long = longitude
            )
        )

        _homeScreenState.update { currentState ->
            currentState.copy(
                nearestMetroState = HomeScreenState.NearestMetroState.NearestMetroStations(
                    stations = nearestMetroStations
                )
            )
        }
    }

    private fun handleLocationError() {
        _homeScreenState.update { currentState ->
            currentState.copy(nearestMetroState = null)
        }
        showErrorMessage("Please turn on your location")
        locationRepository.openLocationSettings()
    }

    private fun cancelNearestMetro() {
        nearestMetroJob?.cancel()
        _homeScreenState.update { currentState ->
            currentState.copy(nearestMetroState = null)
        }
    }

    // Last metro timing operations
    private fun loadLastMetroTiming() {
        val metroState = _homeScreenState.value.lastMetroTimingState

        if (!isValidLastMetroState(metroState)) {
            showErrorMessage("Please select source and destination station")
            return
        }

        val stationSelect = metroState as HomeScreenState.LastMetroTimingState.StationSelect
        showLastMetroLoading(stationSelect)

        lastMetroJob = viewModelScope.launch(Dispatchers.IO) {
            fetchAndDisplayLastMetroTiming(stationSelect)
        }
    }

    private fun isValidLastMetroState(metroState: HomeScreenState.LastMetroTimingState?): Boolean {
        return metroState is HomeScreenState.LastMetroTimingState.StationSelect &&
                metroState.source != null &&
                metroState.destination != null
    }

    private fun showLastMetroLoading(stationSelect: HomeScreenState.LastMetroTimingState.StationSelect) {
        _homeScreenState.update { currentState ->
            currentState.copy(
                lastMetroTimingState = HomeScreenState.LastMetroTimingState.Loading(
                    source = stationSelect.source!!,
                    destination = stationSelect.destination!!
                )
            )
        }
    }

    private suspend fun fetchAndDisplayLastMetroTiming(stationSelect: HomeScreenState.LastMetroTimingState.StationSelect) {
        val source = stationSelect.source!!
        val destination = stationSelect.destination!!

        val metroTime = stationRepository.getFirstAndLastMetroTime(
            source,
            destination
        )

        if (metroTime != null) {
            _homeScreenState.update { currentState ->
                currentState.copy(
                    lastMetroTimingState = HomeScreenState.LastMetroTimingState.LastMetroTime(
                        source = source,
                        destination = destination,
                        firstMetroTime = metroTime.first,
                        lastMetroTime = metroTime.second
                    )
                )
            }
            logFirstLastMetroEvent(success = true)
        } else {
            handleLastMetroTimingError()
            logFirstLastMetroEvent(success = false)
        }
    }

    private fun handleLastMetroTimingError() {
        _homeScreenState.update { currentState ->
            currentState.copy(lastMetroTimingState = null)
        }
        showErrorMessage("Sorry, Something went wrong.")
    }

    // Validation and error handling
    private fun validateRoute(sourceId: Long?, destinationId: Long?): Boolean {
        if (sourceId == null || destinationId == null) {
            val errorMessage = when {
                sourceId == null -> "Please select a proper Source station"
                else -> "Please select a proper Destination station"
            }
            showErrorMessage(errorMessage)
            return false
        }
        return true
    }

    private fun showErrorMessage(errorMessage: String) {
        _homeScreenState.update { currentState ->
            currentState.copy(
                showError = true,
                errorMessage = errorMessage
            )
        }
    }

    // Analytics logging methods
    private fun logSourceSelectionEvent(station: StationUi?) {
        station?.let {
            FirebaseAnalyticsTracker.logEvent(
                eventName = AnalyticsEvents.SOURCE_SELECT,
                screenName = ScreenName.HOME_SCREEN,
                eventParams = mapOf(
                    AnalyticsParams.SOURCE_ID to station.id,
                    AnalyticsParams.SOURCE_NAME to station.name.value
                )
            )
        }
    }

    private fun logDestinationSelectionEvent(station: StationUi?) {
        station?.let {
            FirebaseAnalyticsTracker.logEvent(
                eventName = AnalyticsEvents.DEST_SELECT,
                screenName = ScreenName.HOME_SCREEN,
                eventParams = mapOf(
                    AnalyticsParams.DEST_ID to station.id,
                    AnalyticsParams.DEST_NAME to station.name.value
                )
            )
        }
    }

    private fun logGetRouteEvent(action: HomeScreenUiAction.GetRoute, isValidRoute: Boolean) {
        FirebaseAnalyticsTracker.logEvent(
            eventName = if (action.isRecent) {
                AnalyticsEvents.GET_RECENT_ROUTE
            } else {
                AnalyticsEvents.GET_ROUTE
            },
            screenName = ScreenName.HOME_SCREEN,
            eventParams = mapOf(
                AnalyticsParams.SOURCE_ID to action.sourceId,
                AnalyticsParams.DEST_ID to action.destId,
                AnalyticsParams.ERROR to !isValidRoute
            )
        )
    }

    private fun logLastMetroDestinationSelectionEvent(station: StationUi?) {
        station?.let {
            FirebaseAnalyticsTracker.logEvent(
                eventName = AnalyticsEvents.LAST_METRO_TIME_DEST_SELECT,
                screenName = ScreenName.HOME_SCREEN,
                eventParams = mapOf(
                    AnalyticsParams.SOURCE_ID to station.id,
                    AnalyticsParams.SOURCE_NAME to station.name.value
                )
            )
        }
    }

    private fun logLastMetroSourceSelectionEvent(station: StationUi?) {
        station?.let {
            FirebaseAnalyticsTracker.logEvent(
                eventName = AnalyticsEvents.LAST_METRO_TIME_SOURCE_SELECT,
                screenName = ScreenName.HOME_SCREEN,
                eventParams = mapOf(
                    AnalyticsParams.SOURCE_ID to station.id,
                    AnalyticsParams.SOURCE_NAME to station.name.value
                )
            )
        }
    }

    private fun logFirstLastMetroEvent(success: Boolean) {
        FirebaseAnalyticsTracker.logEvent(
            eventName = AnalyticsEvents.FIRST_LAST_METRO,
            screenName = ScreenName.HOME_SCREEN,
            eventParams = mapOf(AnalyticsParams.ERROR to !success)
        )
    }

}