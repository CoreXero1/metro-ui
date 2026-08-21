package com.codeancy.metroui.route.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.components.ComponentCard
import com.codeancy.metroui.common.utils.MetroUiColor
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.arrow
import indianmetro.metroui.generated.resources.fare
import indianmetro.metroui.generated.resources.interchanges
import indianmetro.metroui.generated.resources.metro
import indianmetro.metroui.generated.resources.route_view_all_stations
import indianmetro.metroui.generated.resources.route_view_interchanges
import indianmetro.metroui.generated.resources.runtime
import indianmetro.metroui.generated.resources.stations
import indianmetro.metroui.generated.resources.swap
import indianmetro.metroui.generated.resources.time
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MetroRouteSubInfo(
    time: Long,
    fare: Int,
    stations: Int,
    interchanges: Int,
    startStationName: String,
    destinationStationName: String,
    isInterChangeRouteOpen: Boolean,
    onToggleRoute: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ComponentCard(
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            SourceAndDestinationHeader(
                sourceStationName = startStationName,
                destinationStationName = destinationStationName,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (time > 0) {
                    MetroRouteSubInfoItem(
                        title = stringResource(Res.string.runtime),
                        imageVector = vectorResource(Res.drawable.time),
                        value = "$time Mins"
                    )
                }

                if (fare > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MetroRouteSubInfoItem(
                        title = stringResource(Res.string.fare),
                        imageVector = vectorResource(Res.drawable.fare),
                        value = "₹ $fare"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                MetroRouteSubInfoItem(
                    title = stringResource(Res.string.stations),
                    imageVector = vectorResource(Res.drawable.metro),
                    value = stations.toString()
                )

                Spacer(modifier = Modifier.width(8.dp))
                MetroRouteSubInfoItem(
                    title = stringResource(Res.string.interchanges),
                    imageVector = vectorResource(Res.drawable.swap),
                    value = interchanges.toString()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RouteViewSegmentedControl(
                isInterchangeView = isInterChangeRouteOpen,
                onViewSelected = onToggleRoute,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SourceAndDestinationHeader(
    sourceStationName: String,
    destinationStationName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = sourceStationName,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        Icon(
            imageVector = vectorResource(Res.drawable.arrow),
            contentDescription = null,
            tint = MetroUiColor.subHeading,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = destinationStationName,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun RouteViewSegmentedControl(
    isInterchangeView: Boolean,
    onViewSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentedTabItem(
            title = stringResource(Res.string.route_view_interchanges),
            imageVector = vectorResource(Res.drawable.swap),
            isSelected = isInterchangeView,
            onClick = {
                if (!isInterchangeView) {
                    onViewSelected(true)
                }
            },
            modifier = Modifier.weight(1f)
        )

        SegmentedTabItem(
            title = stringResource(Res.string.route_view_all_stations),
            imageVector = vectorResource(Res.drawable.metro),
            isSelected = !isInterchangeView,
            onClick = {
                if (isInterchangeView) {
                    onViewSelected(false)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedTabItem(
    title: String,
    imageVector: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color.White else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MetroUiColor.subHeading
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = fontWeight
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetroRouteSubInfoItem(
    imageVector: ImageVector,
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.border(
            1.dp,
            MetroUiColor.subHeading,
            RoundedCornerShape(12.dp)
        )
            .padding(12.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MetroUiColor.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MetroUiColor.subHeading,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}