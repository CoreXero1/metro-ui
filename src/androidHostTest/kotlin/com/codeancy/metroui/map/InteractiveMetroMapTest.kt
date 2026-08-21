package com.codeancy.metroui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.codeancy.metroui.common.utils.hexToColor
import com.codeancy.metroui.domain.models.MapLineUi
import com.codeancy.metroui.domain.models.MapStationUi
import com.codeancy.metroui.domain.models.MetroMapDataUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InteractiveMetroMapTest {

    private val sampleStations = listOf(
        MapStationUi(
            id = 1L,
            name = "Kashmere Gate",
            hindiName = "कश्मीरी गेट",
            code = "KG",
            lineId = 1,
            lineName = "Red Line",
            colorHex = "#c94534",
            isJunction = true,
            isAirport = false,
            parking = 100,
            nearestParking = "Near Gate 1",
            stationType = 0,
            x = 1000f,
            y = 500f,
            latitude = 28.6678,
            longitude = 77.2280,
            connectedLines = listOf("Yellow Line", "Violet Line")
        ),
        MapStationUi(
            id = 2L,
            name = "Rajiv Chowk",
            hindiName = "राजीव चौक",
            code = "RC",
            lineId = 3,
            lineName = "Yellow Line",
            colorHex = "#ffc900",
            isJunction = true,
            isAirport = false,
            parking = 0,
            nearestParking = null,
            stationType = 0,
            x = 1000f,
            y = 800f,
            latitude = 28.6328,
            longitude = 77.2195,
            connectedLines = listOf("Blue Line")
        ),
        MapStationUi(
            id = 3L,
            name = "IGI Airport",
            hindiName = "आईजीआई एयरपोर्ट",
            code = "AP",
            lineId = 7,
            lineName = "Orange Line",
            colorHex = "#F6921E",
            isJunction = false,
            isAirport = true,
            parking = 500,
            nearestParking = "T3 Multi-level",
            stationType = 0,
            x = 500f,
            y = 1200f,
            latitude = 28.5548,
            longitude = 77.0879
        )
    )

    @Test
    fun testFindStationAtDirectHit() {
        val gestureState = MapGestureState()
        gestureState.viewportSize = Size(1080f, 1920f)

        // Exact center tap on Kashmere Gate
        val hit = gestureState.findStationAt(
            tapOffset = Offset(1000f, 500f),
            stations = sampleStations
        )

        assertNotNull(hit)
        assertEquals(1L, hit.id)
        assertEquals("Kashmere Gate", hit.name)
    }

    @Test
    fun testFindStationAtWithinTouchRadius() {
        val gestureState = MapGestureState()
        gestureState.viewportSize = Size(1080f, 1920f)

        // Tap slightly off Rajiv Chowk (within 32 screen px)
        val hit = gestureState.findStationAt(
            tapOffset = Offset(1015f, 810f),
            stations = sampleStations
        )

        assertNotNull(hit)
        assertEquals(2L, hit.id)
        assertEquals("Rajiv Chowk", hit.name)
    }

    @Test
    fun testFindStationAtFarMiss() {
        val gestureState = MapGestureState()
        gestureState.viewportSize = Size(1080f, 1920f)

        // Tap far away from any station
        val hit = gestureState.findStationAt(
            tapOffset = Offset(100f, 100f),
            stations = sampleStations
        )

        assertNull(hit)
    }

    @Test
    fun testHexToColorSafeParsing() {
        val red = "#c94534".hexToColor()
        assertEquals(Color(0xFFc94534), red)

        val yellow = "ffc900".hexToColor()
        assertEquals(Color(0xFFffc900), yellow)

        val fallback = "invalid_hex".hexToColor(Color.Blue)
        assertEquals(Color.Blue, fallback)
    }

    @Test
    fun testMetroMapDataBoundsCalculation() {
        val minX = sampleStations.minOf { it.x }
        val maxX = sampleStations.maxOf { it.x }
        val minY = sampleStations.minOf { it.y }
        val maxY = sampleStations.maxOf { it.y }

        val mapData = MetroMapDataUi(
            stations = sampleStations,
            edges = emptyList(),
            lines = listOf(
                MapLineUi(id = 1, name = "Red Line", colorHex = "#c94534", stationCount = 1),
                MapLineUi(id = 3, name = "Yellow Line", colorHex = "#ffc900", stationCount = 1),
                MapLineUi(id = 7, name = "Orange Line", colorHex = "#F6921E", isAirport = true, stationCount = 1)
            ),
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY
        )

        assertEquals(500f, mapData.minX)
        assertEquals(1000f, mapData.maxX)
        assertEquals(500f, mapData.minY)
        assertEquals(1200f, mapData.maxY)
        assertEquals(3, mapData.stations.size)
        assertEquals(3, mapData.lines.size)
    }

    @Test
    fun testRouteFieldEnumValues() {
        assertEquals(2, RouteField.entries.size)
        assertEquals(RouteField.SOURCE, RouteField.valueOf("SOURCE"))
        assertEquals(RouteField.DESTINATION, RouteField.valueOf("DESTINATION"))
    }

    @Test
    fun testHorizontalStationOrientationCalculation() {
        val st1 = MapStationUi(
            id = 10L, name = "Inderlok", hindiName = "", code = "IL", lineId = 2,
            lineName = "Green Line", colorHex = "#009933", isJunction = true, isAirport = false,
            parking = 0, nearestParking = null, stationType = 0, x = 100f, y = 500f,
            latitude = 0.0, longitude = 0.0, connectedLines = emptyList()
        )
        val st2 = MapStationUi(
            id = 11L, name = "Ashok Park Main", hindiName = "", code = "APM", lineId = 2,
            lineName = "Green Line", colorHex = "#009933", isJunction = false, isAirport = false,
            parking = 0, nearestParking = null, stationType = 0, x = 150f, y = 502f,
            latitude = 0.0, longitude = 0.0, connectedLines = emptyList()
        )

        val dx = kotlin.math.abs(st1.x - st2.x)
        val dy = kotlin.math.abs(st1.y - st2.y)
        val isHorizontal = dx > dy * 1.2f

        assertEquals(true, isHorizontal)
        assertEquals(50f, dx)
        assertEquals(2f, dy)
    }
}
