package com.codeancy.metroui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.components.ComponentCard
import com.codeancy.metroui.common.components.HomeScreenComponentHeader
import com.codeancy.metroui.common.utils.MetroUiColor
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.book_ticket
import indianmetro.metroui.generated.resources.map
import indianmetro.metroui.generated.resources.nearest_metro
import indianmetro.metroui.generated.resources.quick_access
import indianmetro.metroui.generated.resources.timings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private val mapColors = Color(0xFFFEE2E2) to Color(0xFFDC2626)
private val bookTicketColors = Color(0xFFDEFEE7) to Color(0xFF16A34A)

private val platformColors = Color(0xFFF3E8FF) to Color(0xFF9333EA)
private val timingsColors = Color(0xFFFEF9C3) to Color(0xFFCA8A04)

private val nearestMetroColors = Color(0xFFDBEAFE) to Color(0xFF3b82F6)

enum class QuickAccess {
    Map,
    BOOK_TICKET,
    NearestMetro,
    Timings
}

@Composable
fun QuickAccess(
    modifier: Modifier = Modifier,
    visibleItems: Set<QuickAccess> = QuickAccess.entries.toSet(),
    onClick: (QuickAccess) -> Unit
) {

    Column(
        modifier = modifier
    ) {
        HomeScreenComponentHeader(
            title = stringResource(Res.string.quick_access),
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            QuickAccessIcon(
                imageVector = vectorResource(Res.drawable.map),
                title = stringResource(Res.string.map),

                iconBackgroundColor = mapColors.first,
                iconTintColor = mapColors.second,
                onClick = {
                    onClick(QuickAccess.Map)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            if (QuickAccess.BOOK_TICKET in visibleItems) {
                QuickAccessIcon(
                    imageVector = vectorResource(Res.drawable.book_ticket),
                    title = stringResource(Res.string.book_ticket),
                    iconBackgroundColor = bookTicketColors.first,
                    iconTintColor = bookTicketColors.second,
                    onClick = {
                        onClick(QuickAccess.BOOK_TICKET)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            if (QuickAccess.NearestMetro in visibleItems) {
                QuickAccessIcon(
                    imageVector = vectorResource(Res.drawable.nearest_metro),
                    title = stringResource(Res.string.nearest_metro),
                    iconBackgroundColor = nearestMetroColors.first,
                    iconTintColor = nearestMetroColors.second,
                    onClick = {
                        onClick(QuickAccess.NearestMetro)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            if (QuickAccess.Timings in visibleItems) {
                QuickAccessIcon(
                    imageVector = vectorResource(Res.drawable.timings),
                    title = stringResource(Res.string.timings),
                    iconBackgroundColor = timingsColors.first,
                    iconTintColor = timingsColors.second,
                    onClick = {
                        onClick(QuickAccess.Timings)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun QuickAccessIcon(
    imageVector: ImageVector,
    title: String,
    iconTintColor: Color,
    iconBackgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    ComponentCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(iconBackgroundColor)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
            Text(
                text = title,
                style = TextStyle(
                    color = MetroUiColor.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

}
