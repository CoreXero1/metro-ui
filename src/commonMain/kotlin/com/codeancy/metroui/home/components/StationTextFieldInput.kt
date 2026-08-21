package com.codeancy.metroui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.utils.MetroUiColor
import com.codeancy.metroui.domain.models.StationUi
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.cross
import org.jetbrains.compose.resources.vectorResource

private val indicatorWidth = 2.dp

@Composable
fun StationTextFieldInput(
    isSource: Boolean,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    stations: List<StationUi>,
    onSelectStation: (StationUi?) -> Unit,
    hintText: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onNextIme: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current

    var isFocused by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
    ) {
        val borderColor by animateColorAsState(
            if (isFocused)
                MaterialTheme.colorScheme.primary
            else MetroUiColor.stationCard.unfocusedBorderColor
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .height(IntrinsicSize.Max)
                .onFocusChanged {
                    isFocused = it.isFocused
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (isSource) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    onNextIme?.invoke() ?: focusManager.moveFocus(FocusDirection.Next)
                },
                onDone = {
                    focusManager.clearFocus(true)
                }
            ),
            cursorBrush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary
                )
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MetroUiColor.stationCard.inputBackgroundColor)
                        .border(
                            2.dp,
                            borderColor,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 16.dp)
                        .height(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationSelectionLeading(isSource)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Max)
                            .padding(
                                vertical = 14.dp,
                                horizontal = 12.dp
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                        if (value.text.isEmpty()) {
                            Text(
                                text = hintText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                    if (value.text.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable {
                                    onValueChange(TextFieldValue(""))
                                    onSelectStation(null)
                                    if (!isFocused) {
                                        focusRequester.requestFocus()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.cross),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }
        )

        val filteredStationUis = remember(value, stations) {
            fuzzyMatchSubstring(value.text, stations)
        }

        val shouldShowDropdown = isFocused && filteredStationUis.isNotEmpty() &&
            !(filteredStationUis.size == 1 && filteredStationUis.first().name.value.equals(value.text, ignoreCase = true))

        AnimatedVisibility(shouldShowDropdown) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                filteredStationUis.forEach { stationUi ->
                    DropdownMenuItem(
                        onClick = {
                            onSelectStation(stationUi)
                            if (isSource) {
                                onNextIme?.invoke() ?: focusManager.moveFocus(FocusDirection.Next)
                            } else {
                                focusManager.clearFocus(true)
                            }
                        },
                        text = {
                            Text(stationUi.name.asString())
                        }
                    )
                }
            }
        }
        AnimatedVisibility(isSource && (filteredStationUis.isEmpty() || !isFocused)) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .height(24.dp)
                    .width(indicatorWidth)
                    .background(MetroUiColor.stationCard.indicatorLineColor)
            )
        }
    }
}

@Composable
private fun Dot(
    isSource: Boolean,
    modifier: Modifier = Modifier
) {

    val stationSelectionCard = MetroUiColor.stationCard

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (isSource)
                    stationSelectionCard.sourceDotColor
                else
                    stationSelectionCard.destinationDotColor
            )
    )

}

@Composable
private fun StationSelectionLeading(isSource: Boolean) {
    var halfBoxHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .heightIn(min = 32.dp)
            .onGloballyPositioned {
                halfBoxHeight = with(density) {
                    (it.size.height / 2).toDp()
                }
            },
    ) {
        if (isSource) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(halfBoxHeight + 5.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Dot(true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(indicatorWidth)
                        .background(MetroUiColor.stationCard.indicatorLineColor)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(halfBoxHeight + 5.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(indicatorWidth)
                        .background(MetroUiColor.stationCard.indicatorLineColor)
                )
                Dot(
                    false
                )
            }
        }
    }
}

fun fuzzyMatchSubstring(
    input: String,
    stations: List<StationUi>,
    maxResult: Int = 6
): List<StationUi> {
    if (input.isEmpty()) return listOf()

    val results = LinkedHashSet<StationUi>()
    val inputLower = input.lowercase()

    for (inputLength in inputLower.length downTo 1) {
        val substrings = generateSubstrings(inputLower, inputLength)

        for (substring in substrings) {
            for (station in stations) {
                if (station.name.value.lowercase().contains(substring)) {
                    results.add(station)
                    if (results.size == maxResult) {
                        return results.toList()
                    }
                }
            }
        }
    }

    return results.toList()
}

fun generateSubstrings(input: String, length: Int): List<String> {
    val substrings = mutableListOf<String>()
    for (i in 0..(input.length - length)) {
        substrings.add(input.substring(i, i + length))
    }
    return substrings
}