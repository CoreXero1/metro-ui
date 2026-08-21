package com.codeancy.metroui.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class MapStationUi(
    val id: Long,
    val name: String,
    val hindiName: String,
    val code: String?,
    val lineId: Int,
    val lineName: String,
    val colorHex: String,
    val isJunction: Boolean,
    val isAirport: Boolean,
    val parking: Int,
    val nearestParking: String?,
    val stationType: Int,
    val x: Float,
    val y: Float,
    val latitude: Double,
    val longitude: Double,
    val connectedLines: List<String> = emptyList(),
    val gates: String? = null,
    val platforms: String? = null
)

@Immutable
data class MapEdgeUi(
    val fromStationId: Long,
    val toStationId: Long,
    val lineId: Int,
    val colorHex: String,
    val timeSec: Int = 0,
    val distanceMeter: Int = 0
)

@Immutable
data class MapLineUi(
    val id: Int,
    val name: String,
    val colorHex: String,
    val isAirport: Boolean = false,
    val stationCount: Int = 0
)

@Immutable
data class MetroMapDataUi(
    val stations: List<MapStationUi> = emptyList(),
    val edges: List<MapEdgeUi> = emptyList(),
    val lines: List<MapLineUi> = emptyList(),
    val minX: Float = 0f,
    val maxX: Float = 3000f,
    val minY: Float = 0f,
    val maxY: Float = 3000f
)
