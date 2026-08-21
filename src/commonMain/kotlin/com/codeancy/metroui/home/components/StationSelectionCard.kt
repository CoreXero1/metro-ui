package com.codeancy.metroui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.components.ComponentCard
import com.codeancy.metroui.common.utils.MetroUiColor
import com.codeancy.metroui.common.utils.hexToColor
import com.codeancy.metroui.domain.models.RouteResultUi
import com.codeancy.metroui.domain.models.StationUi
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.AnalyticsParams
import com.codeancy.metroui.firebase.MetroConfigKey
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.find_route
import indianmetro.metroui.generated.resources.plan_your_journey
import indianmetro.metroui.generated.resources.time
import kotlinx.serialization.json.Json
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.corexero.sutradhar.remoteConfig.FirebaseRemoteConfig
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private val defaultPopularJunctions = listOf(
    "Rajiv Chowk",
    "Kashmere Gate",
    "Hauz Khas",
    "Botanical Garden",
    "Central Secretariat",
    "IGI Airport",
    "New Delhi",
    "Noida Sector 52",
    "Chandni Chowk",
    "Anand Vihar"
)

fun parsePopularStations(configValue: String): List<String> {
    val trimmed = configValue.trim()
    if (trimmed.isEmpty()) return defaultPopularJunctions

    return try {
        if (trimmed.startsWith("[")) {
            Json.decodeFromString<List<String>>(trimmed)
        } else {
            trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

private data class StationChip(
    val station: StationUi,
    val isRecent: Boolean
)

@Composable
fun StationSelectionCard(
    source: TextFieldValue,
    destination: TextFieldValue,
    allStations: List<StationUi>,
    onSourceChanged: (TextFieldValue) -> Unit,
    onDestinationChanged: (TextFieldValue) -> Unit,
    onSourceStationSelected: (StationUi?) -> Unit,
    onDestinationStationSelected: (StationUi?) -> Unit,
    onGetRouteClick: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
    recentRouteResults: List<RouteResultUi> = emptyList(),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val destinationFocusRequester = remember { FocusRequester() }

    val popularStationNames = remember {
        val configStr = FirebaseRemoteConfig.getString(MetroConfigKey.PopularStations)
        parsePopularStations(configStr)
    }

    val displayChips = remember(recentRouteResults, allStations, popularStationNames) {
        val chips = mutableListOf<StationChip>()
        val addedIds = mutableSetOf<Long>()

        // 1. Add recent search stations first (up to 4 unique stations)
        recentRouteResults.flatMap { listOf(it.sourceStation, it.destinationStation) }
            .distinctBy { it.id }
            .take(4)
            .forEach { station ->
                chips.add(StationChip(station = station, isRecent = true))
                addedIds.add(station.id)
            }

        // 2. Add major transit junctions from Firebase Remote Config
        popularStationNames.forEach { targetName ->
            val foundStation = allStations.find { station ->
                station.name.value.equals(targetName, ignoreCase = true) ||
                station.name.value.contains(targetName, ignoreCase = true)
            }
            if (foundStation != null && addedIds.add(foundStation.id)) {
                chips.add(StationChip(station = foundStation, isRecent = false))
            }
        }

        chips
    }

    ComponentCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 16.dp,
                    horizontal = 16.dp
                )
        ) {
            Text(
                text = stringResource(Res.string.plan_your_journey),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            StationSelectContent(
                source = source,
                destination = destination,
                allStations = allStations,
                onSourceChanged = onSourceChanged,
                onDestinationChanged = onDestinationChanged,
                onSourceStationSelected = onSourceStationSelected,
                onDestinationStationSelected = onDestinationStationSelected,
                onSwap = onSwap,
                destinationFocusRequester = destinationFocusRequester,
                modifier = Modifier.fillMaxWidth()
            )

            if (displayChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    displayChips.forEach { chip ->
                        val station = chip.station
                        val isSourceSelected = source.text.equals(station.name.value, ignoreCase = true)
                        val isDestSelected = destination.text.equals(station.name.value, ignoreCase = true)
                        val isSelected = isSourceSelected || isDestSelected

                        val chipBorderColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MetroUiColor.stationCard.unfocusedBorderColor
                        }

                        val chipBgColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MetroUiColor.stationCard.inputBackgroundColor
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBgColor)
                                .border(
                                    width = 1.dp,
                                    color = chipBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    val selectionTarget = if (source.text.isEmpty()) {
                                        onSourceStationSelected(station)
                                        onSourceChanged(TextFieldValue(station.name.value))
                                        destinationFocusRequester.requestFocus()
                                        "source"
                                    } else if (destination.text.isEmpty() || !isSourceSelected) {
                                        onDestinationStationSelected(station)
                                        onDestinationChanged(TextFieldValue(station.name.value))
                                        focusManager.clearFocus(true)
                                        keyboardController?.hide()
                                        "destination"
                                    } else {
                                        onSourceStationSelected(station)
                                        onSourceChanged(TextFieldValue(station.name.value))
                                        destinationFocusRequester.requestFocus()
                                        "source"
                                    }

                                    FirebaseAnalyticsTracker.logEvent(
                                        eventName = AnalyticsEvents.STATION_CHIP_CLICK,
                                        screenName = ScreenName.HOME_SCREEN,
                                        eventParams = mapOf(
                                            AnalyticsParams.STATION_ID to station.id,
                                            AnalyticsParams.STATION_NAME to station.name.value,
                                            AnalyticsParams.CHIP_TYPE to if (chip.isRecent) "recent" else "popular",
                                            AnalyticsParams.SELECTION_TARGET to selectionTarget
                                        )
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (chip.isRecent) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.time),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MetroUiColor.subHeading,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                val dotColor = when {
                                    isSourceSelected -> MetroUiColor.stationCard.sourceDotColor
                                    isDestSelected -> MetroUiColor.stationCard.destinationDotColor
                                    station.colorHex.isNotEmpty() -> station.colorHex.hexToColor()
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }

                            Text(
                                text = station.name.value,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MetroUiColor.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    onGetRouteClick()
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.find_route),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
