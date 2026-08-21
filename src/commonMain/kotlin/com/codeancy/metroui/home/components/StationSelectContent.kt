package com.codeancy.metroui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.codeancy.metroui.common.components.ComponentCard
import com.codeancy.metroui.domain.models.StationUi
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.from_station
import indianmetro.metroui.generated.resources.swap
import indianmetro.metroui.generated.resources.to_station
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun StationSelectContent(
    source: TextFieldValue,
    destination: TextFieldValue,
    allStations: List<StationUi>,
    onSourceChanged: (TextFieldValue) -> Unit,
    onDestinationChanged: (TextFieldValue) -> Unit,
    onSourceStationSelected: (StationUi?) -> Unit,
    onDestinationStationSelected: (StationUi?) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
    sourceFocusRequester: FocusRequester = remember { FocusRequester() },
    destinationFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            StationTextFieldInput(
                value = source,
                stations = allStations,
                onValueChange = onSourceChanged,
                onSelectStation = { station ->
                    onSourceStationSelected(station)
                    if (station != null) {
                        destinationFocusRequester.requestFocus()
                    }
                },
                focusRequester = sourceFocusRequester,
                onNextIme = {
                    destinationFocusRequester.requestFocus()
                },
                isSource = true,
                hintText = stringResource(Res.string.from_station),
                modifier = Modifier
                    .fillMaxWidth()
            )

            StationTextFieldInput(
                value = destination,
                stations = allStations,
                onValueChange = onDestinationChanged,
                onSelectStation = onDestinationStationSelected,
                focusRequester = destinationFocusRequester,
                isSource = false,
                hintText = stringResource(Res.string.to_station),
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        ComponentCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 32.dp, top = 38.dp)
                .padding(vertical = 6.dp),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier
                    .clickable { onSwap() }
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.swap),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp),
                    tint = Color.Black
                )
            }
        }
    }
}