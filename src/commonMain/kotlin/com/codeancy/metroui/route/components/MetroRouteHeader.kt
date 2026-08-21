package com.codeancy.metroui.route.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.utils.MetroUiColor
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.metro_route
import org.jetbrains.compose.resources.stringResource

@Composable
fun MetroRouteHeader(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onViewOnMap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary)
            .padding(
                vertical = 8.dp,
                horizontal = 12.dp
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MetroUiColor.onPrimaryColor,
            modifier = Modifier
                .clickable { onBack() }
                .size(28.dp)
                .align(Alignment.CenterStart)
        )

        Text(
            text = stringResource(Res.string.metro_route),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                color = MetroUiColor.onPrimaryColor,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .align(Alignment.Center)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onViewOnMap != null) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "View on Map",
                    tint = MetroUiColor.onPrimaryColor,
                    modifier = Modifier
                        .clickable { onViewOnMap() }
                        .size(24.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = MetroUiColor.onPrimaryColor,
                modifier = Modifier
                    .clickable { onShare() }
                    .size(24.dp)
            )
        }
    }
}