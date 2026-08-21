package com.codeancy.metroui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.codeancy.metroui.domain.models.MapStationUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Stable
class MapGestureState(
    val minScale: Float = 0.35f,
    val maxScale: Float = 10.0f,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var scale by mutableFloatStateOf(1.0f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    var viewportSize by mutableStateOf(Size.Zero)

    // Animation drivers
    private val scaleAnim = Animatable(1.0f)
    private val offsetAnim = Animatable(Offset.Zero, Offset.VectorConverter)

    fun onTransform(pan: Offset, zoom: Float, centroid: Offset) {
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        val scaleRatio = newScale / scale

        // Pan + Zoom around the centroid focal point
        val newOffsetX = centroid.x - (centroid.x - offset.x) * scaleRatio + pan.x
        val newOffsetY = centroid.y - (centroid.y - offset.y) * scaleRatio + pan.y

        scale = newScale
        offset = Offset(newOffsetX, newOffsetY)
    }

    fun onDoubleTap(tapOffset: Offset) {
        scope.launch {
            if (scale > 1.8f) {
                // Reset zoom
                animateTo(targetScale = 1.0f, targetOffset = calculateFitOffset(1.0f))
            } else {
                // Zoom in centered on tap
                val targetScale = 3.0f
                val scaleRatio = targetScale / scale
                val targetOffsetX = tapOffset.x - (tapOffset.x - offset.x) * scaleRatio
                val targetOffsetY = tapOffset.y - (tapOffset.y - offset.y) * scaleRatio
                animateTo(targetScale = targetScale, targetOffset = Offset(targetOffsetX, targetOffsetY))
            }
        }
    }

    fun zoomIn() {
        val targetScale = (scale * 1.5f).coerceAtMost(maxScale)
        val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val scaleRatio = targetScale / scale
        val targetOffset = center - (center - offset) * scaleRatio
        scope.launch {
            animateTo(targetScale, targetOffset)
        }
    }

    fun zoomOut() {
        val targetScale = (scale / 1.5f).coerceAtLeast(minScale)
        val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val scaleRatio = targetScale / scale
        val targetOffset = center - (center - offset) * scaleRatio
        scope.launch {
            animateTo(targetScale, targetOffset)
        }
    }

    fun resetView(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return

        // Focus on the core urban Delhi network (Rajiv Chowk, Kashmere Gate, Central Secretariat hub)
        val focusMinX = 350f
        val focusMaxX = 2150f
        val focusMinY = 200f
        val focusMaxY = 1650f

        val boxWidth = focusMaxX - focusMinX
        val boxHeight = focusMaxY - focusMinY

        val scaleX = viewportSize.width / boxWidth * 0.95f
        val scaleY = viewportSize.height / boxHeight * 0.90f
        val targetScale = minOf(scaleX, scaleY).coerceIn(0.65f, maxScale)

        val mapCenterX = (focusMinX + focusMaxX) / 2f
        val mapCenterY = (focusMinY + focusMaxY) / 2f

        val targetOffsetX = viewportSize.width / 2f - mapCenterX * targetScale
        val targetOffsetY = viewportSize.height / 2f - mapCenterY * targetScale

        scope.launch {
            animateTo(targetScale, Offset(targetOffsetX, targetOffsetY))
        }
    }

    fun centerOnCoordinates(mapX: Float, mapY: Float, targetZoom: Float = 3.5f) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return
        val clampedScale = targetZoom.coerceIn(minScale, maxScale)
        val targetOffsetX = viewportSize.width / 2f - mapX * clampedScale
        val targetOffsetY = viewportSize.height / 2f - mapY * clampedScale

        scope.launch {
            animateTo(clampedScale, Offset(targetOffsetX, targetOffsetY))
        }
    }

    fun centerOnBoundingBox(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return
        val boxWidth = (maxX - minX).coerceAtLeast(80f)
        val boxHeight = (maxY - minY).coerceAtLeast(80f)

        val scaleX = viewportSize.width / boxWidth * 0.75f
        val scaleY = viewportSize.height / boxHeight * 0.75f
        val targetScale = minOf(scaleX, scaleY).coerceIn(minScale, 4.0f)

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        val targetOffsetX = viewportSize.width / 2f - centerX * targetScale
        val targetOffsetY = viewportSize.height / 2f - centerY * targetScale

        scope.launch {
            animateTo(targetScale, Offset(targetOffsetX, targetOffsetY))
        }
    }

    private suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        scaleAnim.snapTo(scale)
        offsetAnim.snapTo(offset)

        val spec = tween<Float>(durationMillis = 350)
        val offsetSpec = tween<Offset>(durationMillis = 350)

        // Run animations concurrently
        scope.launch {
            scaleAnim.animateTo(targetScale, spec) {
                scale = value
            }
        }
        scope.launch {
            offsetAnim.animateTo(targetOffset, offsetSpec) {
                offset = value
            }
        }
    }

    private fun calculateFitOffset(targetScale: Float): Offset {
        return Offset(
            (viewportSize.width - viewportSize.width * targetScale) / 2f,
            (viewportSize.height - viewportSize.height * targetScale) / 2f
        )
    }

    fun findStationAt(tapOffset: Offset, stations: List<MapStationUi>): MapStationUi? {
        if (stations.isEmpty()) return null

        // Convert screen pixel tap to map coordinate
        val mapX = (tapOffset.x - offset.x) / scale
        val mapY = (tapOffset.y - offset.y) / scale

        // Touch radius in screen dp converted to map space (minimum 28dp radius)
        val screenTouchRadius = 32f
        val mapTouchRadius = screenTouchRadius / scale

        var closestStation: MapStationUi? = null
        var minDistance = Float.MAX_VALUE

        for (st in stations) {
            val dx = st.x - mapX
            val dy = st.y - mapY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= mapTouchRadius && dist < minDistance) {
                minDistance = dist
                closestStation = st
            }
        }
        return closestStation
    }
}

@Composable
fun rememberMapGestureState(
    minScale: Float = 0.35f,
    maxScale: Float = 10.0f
): MapGestureState {
    val scope = rememberCoroutineScope()
    return remember {
        MapGestureState(minScale = minScale, maxScale = maxScale, scope = scope)
    }
}
