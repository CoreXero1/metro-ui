package com.codeancy.metroui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.codeancy.metroui.common.utils.hexToColor
import com.codeancy.metroui.domain.models.MapEdgeUi
import com.codeancy.metroui.domain.models.MapStationUi
import com.codeancy.metroui.domain.models.MetroMapDataUi

// Key interchange hubs that are prioritized on the map overview when NO route is active
private val ANCHOR_STATION_NAMES = setOf(
    "Rajiv Chowk",
    "Kashmere Gate",
    "Central Secretariat",
    "Hauz Khas",
    "Botanical Garden",
    "Inderlok",
    "Welcome",
    "Janakpuri West",
    "Lajpat Nagar",
    "IGI Airport",
    "Airport T3 - IGI",
    "Noida Sector 52",
    "Samaypur Badli",
    "Rithala",
    "Millennium (Huda) City Centre Gurugram",
    "Vaishali",
    "Noida Electronic City",
    "New Delhi",
    "Mandi House",
    "Dilli Haat - INA",
    "Netaji Subhash Place",
    "Kirti Nagar"
)

@Composable
fun InteractiveMetroMap(
    mapData: MetroMapDataUi,
    gestureState: MapGestureState,
    selectedLineId: Int?,
    selectedStation: MapStationUi?,
    routeOverlay: List<Long>,
    userLocationStation: MapStationUi?,
    onStationTapped: (MapStationUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    // Pulse animation for selected station and user location
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Fit map on initial layout
    var hasInitialFit by remember { mutableStateOf(false) }
    LaunchedEffect(mapData.stations, gestureState.viewportSize) {
        if (!hasInitialFit && mapData.stations.isNotEmpty() && gestureState.viewportSize.width > 0) {
            gestureState.resetView(mapData.minX, mapData.maxX, mapData.minY, mapData.maxY)
            hasInitialFit = true
        }
    }

    // Lookup index for fast station coordinate access
    val stationMap = remember(mapData.stations) {
        mapData.stations.associateBy { it.id }
    }

    // Edge lookup for fast segment color mapping: (fromId, toId) -> MapEdgeUi
    val edgeMap = remember(mapData.edges) {
        val map = mutableMapOf<Pair<Long, Long>, MapEdgeUi>()
        for (e in mapData.edges) {
            map[Pair(e.fromStationId, e.toStationId)] = e
            map[Pair(e.toStationId, e.fromStationId)] = e
        }
        map
    }

    // Identify stations that are along horizontal track segments (to only rotate horizontal lines)
    val horizontalStationIds = remember(mapData) {
        val neighborMap = mutableMapOf<Long, MutableList<MapStationUi>>()
        val stationById = mapData.stations.associateBy { it.id }
        for (edge in mapData.edges) {
            val u = stationById[edge.fromStationId]
            val v = stationById[edge.toStationId]
            if (u != null && v != null && u.lineId == v.lineId) {
                neighborMap.getOrPut(u.id) { mutableListOf() }.add(v)
                neighborMap.getOrPut(v.id) { mutableListOf() }.add(u)
            }
        }
        val hSet = mutableSetOf<Long>()
        for (st in mapData.stations) {
            val nbrs = neighborMap[st.id]
            if (!nbrs.isNullOrEmpty()) {
                var totalDx = 0f
                var totalDy = 0f
                for (nbr in nbrs) {
                    totalDx += kotlin.math.abs(st.x - nbr.x)
                    totalDy += kotlin.math.abs(st.y - nbr.y)
                }
                val avgDx = totalDx / nbrs.size
                val avgDy = totalDy / nbrs.size
                if (avgDx > avgDy * 1.2f) {
                    hSet.add(st.id)
                }
            }
        }
        hSet
    }

    // Set of station IDs on the active route
    val routeStationSet = remember(routeOverlay) {
        routeOverlay.toSet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { size ->
                gestureState.viewportSize = size.toSize()
            }
            .pointerInput(mapData.stations) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val tapped = gestureState.findStationAt(tapOffset, mapData.stations)
                        if (tapped != null) {
                            onStationTapped(tapped)
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        gestureState.onDoubleTap(tapOffset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    gestureState.onTransform(pan = pan, zoom = zoom, centroid = centroid)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = gestureState.scale
            val offset = gestureState.offset

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // 1. Draw Network Track Edges
                drawNetworkEdges(
                    edges = mapData.edges,
                    stationMap = stationMap,
                    selectedLineId = selectedLineId,
                    routeStationSet = routeStationSet,
                    isDarkTheme = isDarkTheme
                )

                // 3. Draw Active Route with ORIGINAL LINE COLORS for each segment
                if (routeOverlay.size >= 2) {
                    drawMultiColorRouteOverlay(
                        routeStationIds = routeOverlay,
                        stationMap = stationMap,
                        edgeMap = edgeMap
                    )
                }

                // 4. Draw Station Nodes (Dim non-route nodes when route is active)
                drawStationNodes(
                    stations = mapData.stations,
                    selectedLineId = selectedLineId,
                    routeStationSet = routeStationSet,
                    isDarkTheme = isDarkTheme
                )

                // 5. Draw Selected Station Pulsing Focus Ring
                if (selectedStation != null) {
                    drawSelectedStationPulse(
                        station = selectedStation,
                        pulseRadius = pulseRadius,
                        pulseAlpha = pulseAlpha
                    )
                }

                // 6. Draw User GPS Location Marker
                if (userLocationStation != null) {
                    drawUserLocationMarker(
                        station = userLocationStation,
                        pulseRadius = pulseRadius,
                        pulseAlpha = pulseAlpha
                    )
                }
            }

            // 7. Draw Station Text Labels in Screen Space (ONLY route stations when route is active)
            drawScreenSpaceLabels(
                stations = mapData.stations,
                selectedLineId = selectedLineId,
                selectedStationId = selectedStation?.id,
                routeStationSet = routeStationSet,
                horizontalStationIds = horizontalStationIds,
                scale = scale,
                offset = offset,
                viewportWidth = size.width,
                viewportHeight = size.height,
                isDarkTheme = isDarkTheme,
                textMeasurer = textMeasurer
            )

            // 8. Draw Route Pins in Screen Space (Clean fixed badges for Origin [A] and Destination [B])
            if (routeOverlay.size >= 2) {
                val originStation = stationMap[routeOverlay.first()]
                val destStation = stationMap[routeOverlay.last()]
                if (originStation != null) {
                    drawScreenSpaceRoutePin(
                        station = originStation,
                        label = "A",
                        pinColor = Color(0xFF16A34A),
                        scale = scale,
                        offset = offset,
                        textMeasurer = textMeasurer
                    )
                }
                if (destStation != null) {
                    drawScreenSpaceRoutePin(
                        station = destStation,
                        label = "B",
                        pinColor = Color(0xFFDC2626),
                        scale = scale,
                        offset = offset,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}


private fun DrawScope.drawNetworkEdges(
    edges: List<MapEdgeUi>,
    stationMap: Map<Long, MapStationUi>,
    selectedLineId: Int?,
    routeStationSet: Set<Long>,
    isDarkTheme: Boolean
) {
    val casingColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val isRouteActive = routeStationSet.isNotEmpty()

    // 1. Draw Underline Casings (only when not heavily dimmed)
    if (!isRouteActive && selectedLineId == null) {
        for (edge in edges) {
            val fromSt = stationMap[edge.fromStationId] ?: continue
            val toSt = stationMap[edge.toStationId] ?: continue

            drawLine(
                color = casingColor,
                start = Offset(fromSt.x, fromSt.y),
                end = Offset(toSt.x, toSt.y),
                strokeWidth = 10.5f,
                cap = StrokeCap.Round
            )
        }
    }

    // 2. Draw Main Colored Line Tracks
    for (edge in edges) {
        val fromSt = stationMap[edge.fromStationId] ?: continue
        val toSt = stationMap[edge.toStationId] ?: continue

        val baseColor = edge.colorHex.hexToColor(Color.Gray)
        val isLineSelected = selectedLineId == null || edge.lineId == selectedLineId
        val isEdgeOnRoute = routeStationSet.contains(edge.fromStationId) && routeStationSet.contains(edge.toStationId)

        if (isRouteActive) {
            // When route is active, non-route edges are dimmed to background context
            if (!isEdgeOnRoute) {
                drawLine(
                    color = baseColor.copy(alpha = 0.08f),
                    start = Offset(fromSt.x, fromSt.y),
                    end = Offset(toSt.x, toSt.y),
                    strokeWidth = 3.0f,
                    cap = StrokeCap.Round
                )
            }
        } else if (selectedLineId != null) {
            if (isLineSelected) {
                // Outer glow for active filtered line
                drawLine(
                    color = baseColor.copy(alpha = 0.38f),
                    start = Offset(fromSt.x, fromSt.y),
                    end = Offset(toSt.x, toSt.y),
                    strokeWidth = 20f,
                    cap = StrokeCap.Round
                )
                // Vibrant main track
                drawLine(
                    color = baseColor,
                    start = Offset(fromSt.x, fromSt.y),
                    end = Offset(toSt.x, toSt.y),
                    strokeWidth = 9.5f,
                    cap = StrokeCap.Round
                )
            } else {
                // Dimmed inactive track
                drawLine(
                    color = baseColor.copy(alpha = 0.10f),
                    start = Offset(fromSt.x, fromSt.y),
                    end = Offset(toSt.x, toSt.y),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            // Standard "All Lines" track
            drawLine(
                color = baseColor,
                start = Offset(fromSt.x, fromSt.y),
                end = Offset(toSt.x, toSt.y),
                strokeWidth = 7.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawMultiColorRouteOverlay(
    routeStationIds: List<Long>,
    stationMap: Map<Long, MapStationUi>,
    edgeMap: Map<Pair<Long, Long>, MapEdgeUi>
) {
    // 1. Draw segment glows using the ORIGINAL line color of each segment
    for (i in 0 until routeStationIds.size - 1) {
        val stA = stationMap[routeStationIds[i]] ?: continue
        val stB = stationMap[routeStationIds[i + 1]] ?: continue

        val edge = edgeMap[Pair(routeStationIds[i], routeStationIds[i + 1])]
        val segmentColor = edge?.colorHex?.hexToColor(stA.colorHex.hexToColor(Color(0xFF2563EB)))
            ?: stA.colorHex.hexToColor(Color(0xFF2563EB))

        // Segment Outer Glow
        drawLine(
            color = segmentColor.copy(alpha = 0.40f),
            start = Offset(stA.x, stA.y),
            end = Offset(stB.x, stB.y),
            strokeWidth = 22f,
            cap = StrokeCap.Round
        )

        // Segment White Outline Casing
        drawLine(
            color = Color.White,
            start = Offset(stA.x, stA.y),
            end = Offset(stB.x, stB.y),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )

        // Segment Vibrant Original Color Core Line
        drawLine(
            color = segmentColor,
            start = Offset(stA.x, stA.y),
            end = Offset(stB.x, stB.y),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
    }

    // 2. Draw route station nodes along the path
    for (i in 0 until routeStationIds.size) {
        val st = stationMap[routeStationIds[i]] ?: continue
        val isOrigin = i == 0
        val isDestination = i == routeStationIds.size - 1

        val stColor = st.colorHex.hexToColor(Color(0xFF2563EB))

        if (isOrigin || isDestination) {
            // Origin/Destination station ring
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = if (isOrigin) Color(0xFF16A34A) else Color(0xFFDC2626),
                radius = 7f,
                center = Offset(st.x, st.y)
            )
        } else if (st.isJunction) {
            // Interchange Station on Route: Dual Ring
            drawCircle(
                color = Color.White,
                radius = 8.5f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = stColor,
                radius = 6.5f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(st.x, st.y)
            )
        } else {
            // Regular Route Node
            drawCircle(
                color = Color.White,
                radius = 6.5f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = stColor,
                radius = 4.5f,
                center = Offset(st.x, st.y)
            )
        }
    }
}

private fun DrawScope.drawStationNodes(
    stations: List<MapStationUi>,
    selectedLineId: Int?,
    routeStationSet: Set<Long>,
    isDarkTheme: Boolean
) {
    val isRouteActive = routeStationSet.isNotEmpty()
    val junctionFillColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White

    for (st in stations) {
        val isOnRoute = routeStationSet.contains(st.id)

        // When route is active, nodes on route are drawn by drawMultiColorRouteOverlay
        if (isRouteActive) {
            if (!isOnRoute) {
                // Dim non-route station node
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.08f),
                    radius = 3.0f,
                    center = Offset(st.x, st.y)
                )
            }
            continue
        }

        val baseColor = st.colorHex.hexToColor(Color.Gray)
        val isLineSelected = selectedLineId == null || st.lineId == selectedLineId || st.connectedLines.contains(st.lineName)

        val alpha = when {
            selectedLineId != null && !isLineSelected -> 0.12f
            else -> 1.0f
        }

        val stationColor = baseColor.copy(alpha = alpha)

        if (st.isJunction) {
            // Interchange Hub: Outer Ring + Inner White Fill + Center Dot
            val outerRadius = if (selectedLineId != null && isLineSelected) 11f else 9f
            drawCircle(
                color = stationColor,
                radius = outerRadius,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = junctionFillColor.copy(alpha = alpha),
                radius = outerRadius - 2.5f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = stationColor,
                radius = outerRadius - 5.5f,
                center = Offset(st.x, st.y)
            )
        } else {
            // Regular Station Dot
            drawCircle(
                color = junctionFillColor.copy(alpha = alpha),
                radius = 5.5f,
                center = Offset(st.x, st.y)
            )
            drawCircle(
                color = stationColor,
                radius = 4.0f,
                center = Offset(st.x, st.y)
            )
        }
    }
}

private fun DrawScope.drawSelectedStationPulse(
    station: MapStationUi,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    val pulseColor = station.colorHex.hexToColor(Color(0xFF2563EB))
    val center = Offset(station.x, station.y)

    // Expanding outer radar pulse ring
    drawCircle(
        color = pulseColor.copy(alpha = pulseAlpha),
        radius = 12f + pulseRadius,
        center = center,
        style = Stroke(width = 3.0f)
    )

    // Highlight border ring
    drawCircle(
        color = pulseColor,
        radius = 14f,
        center = center,
        style = Stroke(width = 3.5f)
    )

    // Center dot
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = center
    )
    drawCircle(
        color = pulseColor,
        radius = 4f,
        center = center
    )
}

private fun DrawScope.drawUserLocationMarker(
    station: MapStationUi,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    val gpsColor = Color(0xFF2563EB)
    val center = Offset(station.x, station.y)

    // Radar pulse ring
    drawCircle(
        color = gpsColor.copy(alpha = pulseAlpha * 0.7f),
        radius = 16f + pulseRadius * 1.5f,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Outer white border
    drawCircle(
        color = Color.White,
        radius = 9f,
        center = center
    )

    // Inner bright blue dot
    drawCircle(
        color = gpsColor,
        radius = 6.5f,
        center = center
    )
}

private fun DrawScope.drawScreenSpaceLabels(
    stations: List<MapStationUi>,
    selectedLineId: Int?,
    selectedStationId: Long?,
    routeStationSet: Set<Long>,
    horizontalStationIds: Set<Long>,
    scale: Float,
    offset: Offset,
    viewportWidth: Float,
    viewportHeight: Float,
    isDarkTheme: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val isRouteActive = routeStationSet.isNotEmpty()
    val labelColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val pillBackground = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val pillBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    val fontSize = 11.sp

    // Priority ordering:
    // When route active: strictly route stations
    // When line filtered: strictly selected line stations
    // When route inactive: Selected > Anchor Hubs > Filtered Line > Interchanges > Regular
    val candidateStations = if (isRouteActive) {
        stations.filter { routeStationSet.contains(it.id) }
    } else if (selectedLineId != null) {
        stations.filter { it.lineId == selectedLineId }
    } else {
        stations
    }

    val sortedStations = candidateStations.sortedWith(
        compareByDescending<MapStationUi> { it.id == selectedStationId }
            .thenByDescending { routeStationSet.contains(it.id) }
            .thenByDescending { ANCHOR_STATION_NAMES.contains(it.name) }
            .thenByDescending { selectedLineId != null && it.lineId == selectedLineId }
            .thenByDescending { it.isJunction }
    )

    for (st in sortedStations) {
        val isSelected = st.id == selectedStationId
        val isOnRoute = routeStationSet.contains(st.id)
        val isAnchorHub = !isRouteActive && selectedLineId == null && ANCHOR_STATION_NAMES.contains(st.name)
        val isLineSelected = !isRouteActive && selectedLineId != null && st.lineId == selectedLineId

        // Convert station coordinate to screen pixel coordinate
        val screenX = st.x * scale + offset.x
        val screenY = st.y * scale + offset.y

        // Frustum culling: Skip if outside visible screen
        if (screenX < -150f || screenX > viewportWidth + 150f || screenY < -100f || screenY > viewportHeight + 100f) {
            continue
        }

        // Level-of-Detail threshold:
        // - When route is active or line is selected: ALL stations on that path are shown
        // - In general overview: anchor stations always show, junctions at scale >= 0.75, regular at scale >= 1.2
        val isHighPriority = isSelected || isOnRoute || isAnchorHub || isLineSelected
        val shouldAttemptShow = isHighPriority || (scale >= 1.2f) || (scale >= 0.75f && st.isJunction)

        if (!shouldAttemptShow) continue

        val textLayout = textMeasurer.measure(
            text = st.name,
            style = TextStyle(
                color = labelColor,
                fontSize = fontSize,
                fontWeight = if (st.isJunction || isHighPriority) FontWeight.Bold else FontWeight.Medium
            )
        )

        val textWidth = textLayout.size.width.toFloat()
        val textHeight = textLayout.size.height.toFloat()

        val stColor = st.colorHex.hexToColor(Color(0xFF2563EB))
        val isStationHorizontal = horizontalStationIds.contains(st.id)

        if (isStationHorizontal) {
            // ONLY stations along horizontal lines are slanted at -35 deg to prevent horizontal track overlaps
            withTransform({
                rotate(degrees = -35f, pivot = Offset(screenX, screenY))
            }) {
                val pillLeft = screenX + 9f
                val pillTop = screenY - textHeight / 2f - 2.5f
                val pillWidth = textWidth + 10f
                val pillHeight = textHeight + 5f

                drawRoundRect(
                    color = pillBackground,
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                drawRoundRect(
                    color = if (isOnRoute || isLineSelected) stColor else pillBorderColor,
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = if (isOnRoute || isLineSelected) 1.5f else 1f)
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(pillLeft + 5f, screenY - textHeight / 2f)
                )
            }
        } else {
            // All vertical lines, diagonal lines, and general stations remain standard horizontal (0 deg)
            val pillLeft = if (screenX + textWidth + 24f > viewportWidth) {
                screenX - textWidth - 14f
            } else {
                screenX + 10f
            }
            val pillTop = screenY - textHeight / 2f - 2.5f
            val pillWidth = textWidth + 10f
            val pillHeight = textHeight + 5f

            drawRoundRect(
                color = pillBackground,
                topLeft = Offset(pillLeft, pillTop),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            drawRoundRect(
                color = if (isOnRoute || isLineSelected) stColor else pillBorderColor,
                topLeft = Offset(pillLeft, pillTop),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = if (isOnRoute || isLineSelected) 1.5f else 1f)
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(pillLeft + 5f, screenY - textHeight / 2f)
            )
        }
    }
}

private fun DrawScope.drawScreenSpaceRoutePin(
    station: MapStationUi,
    label: String,
    pinColor: Color,
    scale: Float,
    offset: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val screenX = station.x * scale + offset.x
    val screenY = station.y * scale + offset.y - 20f

    val center = Offset(screenX, screenY)

    // Pin bubble
    drawCircle(
        color = pinColor,
        radius = 13f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 13f,
        center = center,
        style = Stroke(width = 2f)
    )

    // Text label "A" or "B"
    val textResult = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    )

    // Use font baseline for exact vertical glyph centering inside circle
    val textX = center.x - textResult.size.width / 2f
    val textY = center.y - textResult.firstBaseline / 2f

    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(textX, textY)
    )
}
