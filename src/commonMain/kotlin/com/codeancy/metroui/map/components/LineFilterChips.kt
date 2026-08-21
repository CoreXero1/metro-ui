package com.codeancy.metroui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.utils.hexToColor
import com.codeancy.metroui.domain.models.MapLineUi

@Composable
fun LineFilterChips(
    lines: List<MapLineUi>,
    selectedLineId: Int?,
    onLineSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All Lines" Chip
        item {
            val isAllSelected = selectedLineId == null
            LineChip(
                label = "All Lines",
                color = MaterialTheme.colorScheme.primary,
                isSelected = isAllSelected,
                onClick = { onLineSelected(null) }
            )
        }

        items(lines, key = { it.id }) { line ->
            val isSelected = selectedLineId == line.id
            val color = line.colorHex.hexToColor()
            LineChip(
                label = line.name,
                color = color,
                isSelected = isSelected,
                onClick = {
                    if (isSelected) onLineSelected(null) else onLineSelected(line.id)
                }
            )
        }
    }
}

@Composable
private fun LineChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        color.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }

    val borderColor = if (isSelected) {
        color
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}
