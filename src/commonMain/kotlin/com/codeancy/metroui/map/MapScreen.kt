package com.codeancy.metroui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.codeancy.metroui.common.utils.MapDrawableResource
import com.codeancy.metroui.domain.models.MapStationUi
import com.codeancy.metroui.map.components.LineFilterChips
import com.codeancy.metroui.map.components.MapControls
import com.codeancy.metroui.map.components.MapRouteHeader
import com.codeancy.metroui.map.components.StationDetailBottomSheet

@Composable
fun MapScreen(
    state: MapScreenState,
    onAction: (MapUiAction) -> Unit,
    onBack: () -> Unit,
    onNavigateToRoute: (Long, Long) -> Unit,
    onOpenExternalMap: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gestureState = rememberMapGestureState()

    // Handle navigation events triggered by the ViewModel
    LaunchedEffect(state.navigationEvent) {
        when (val event = state.navigationEvent) {
            is MapNavigationEvent.NavigateToRoute -> {
                onNavigateToRoute(event.sourceId, event.destId)
                onAction(MapUiAction.ConsumeNavigationEvent)
            }
            is MapNavigationEvent.OpenExternalMap -> {
                onOpenExternalMap(event.lat, event.lng, event.label)
                onAction(MapUiAction.ConsumeNavigationEvent)
            }
            null -> Unit
        }
    }

    // Handle camera auto-pan animation to focused station or line
    LaunchedEffect(state.focusTarget) {
        state.focusTarget?.let { target ->
            val targetZoom = if (state.selectedStation != null) 3.0f else 1.4f
            gestureState.centerOnCoordinates(target.x, target.y, targetZoom = targetZoom)
            onAction(MapUiAction.ResetFocusTarget)
        }
    }

    // When deselecting line filter back to All Lines, reset view
    LaunchedEffect(state.selectedLineId) {
        if (state.selectedLineId == null && state.mapData.stations.isNotEmpty()) {
            gestureState.resetView(
                state.mapData.minX,
                state.mapData.maxX,
                state.mapData.minY,
                state.mapData.maxY
            )
        }
    }

    // Handle auto-fit when route overlay changes
    LaunchedEffect(state.routeOverlay) {
        if (state.routeOverlay.isNotEmpty() && state.mapData.stations.isNotEmpty()) {
            val routeStations = state.mapData.stations.filter { state.routeOverlay.contains(it.id) }
            if (routeStations.isNotEmpty()) {
                val minX = routeStations.minOf { it.x }
                val maxX = routeStations.maxOf { it.x }
                val minY = routeStations.minOf { it.y }
                val maxY = routeStations.maxOf { it.y }
                gestureState.centerOnBoundingBox(minX, maxX, minY, maxY)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Interactive Vector Transit Map Canvas
        InteractiveMetroMap(
            mapData = state.mapData,
            gestureState = gestureState,
            selectedLineId = state.selectedLineId,
            selectedStation = state.selectedStation,
            routeOverlay = state.routeOverlay,
            userLocationStation = state.userLocationStation,
            onStationTapped = { station ->
                onAction(MapUiAction.SelectStation(station))
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Bar with Search & Line Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            MapRouteHeader(
                sourceStation = state.sourceStation,
                destinationStation = state.destinationStation,
                activeField = state.activeRouteField,
                searchQuery = state.searchQuery,
                suggestions = state.searchSuggestions,
                routeResult = state.routeResultUi,
                onFieldFocused = { onAction(MapUiAction.FocusRouteField(it)) },
                onQueryChanged = { onAction(MapUiAction.ChangeSearchQuery(it)) },
                onStationSelected = { onAction(MapUiAction.SelectSearchResult(it)) },
                onSwapStations = { onAction(MapUiAction.SwapSourceAndDestination) },
                onClearField = { onAction(MapUiAction.ClearRouteField(it)) },
                onClearRoute = { onAction(MapUiAction.ClearRouteOverlay) },
                onViewRouteDetails = { srcId, dstId -> onNavigateToRoute(srcId, dstId) },
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            if (state.mapData.lines.isNotEmpty() && state.routeOverlay.isEmpty()) {
                LineFilterChips(
                    lines = state.mapData.lines,
                    selectedLineId = state.selectedLineId,
                    onLineSelected = { onAction(MapUiAction.FilterLine(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Floating Map Controls (Fit All, Zoom In/Out)
        MapControls(
            onResetView = {
                gestureState.resetView(
                    state.mapData.minX,
                    state.mapData.maxX,
                    state.mapData.minY,
                    state.mapData.maxY
                )
            },
            onZoomIn = { gestureState.zoomIn() },
            onZoomOut = { gestureState.zoomOut() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
        )

        // 4. Loading Indicator Overlay
        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        // 5. Station Detail Modal Bottom Sheet
        state.selectedStation?.let { selected ->
            StationDetailBottomSheet(
                station = selected,
                firstLastTime = state.selectedStationFirstLastTime,
                onDismiss = { onAction(MapUiAction.DismissBottomSheet) },
                onSetAsSource = { onAction(MapUiAction.SetAsSource(it)) },
                onSetAsDestination = { onAction(MapUiAction.SetAsDestination(it)) },
                onOpenDirections = { onAction(MapUiAction.OpenDirections(it)) }
            )
        }
    }
}

@Composable
expect fun MapImage(
    modifier: Modifier,
    mapDrawableResource: MapDrawableResource,
    contentDescription: String?,
    contentScale: ContentScale
)