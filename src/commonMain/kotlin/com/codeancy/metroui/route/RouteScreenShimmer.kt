package com.codeancy.metroui.route

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codeancy.metroui.common.components.ComponentCard

private val shimmerBase = Color(0xFFE0E0E0)
private val shimmerHighlight = Color(0xFFF5F5F5)

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = listOf(shimmerBase, shimmerHighlight, shimmerBase),
        start = Offset(progress - 300f, progress - 300f),
        end = Offset(progress, progress)
    )
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun RouteScreenShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sub-info card skeleton
        ComponentCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 4 info item placeholders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            ShimmerBox(modifier = Modifier.size(20.dp), shape = RoundedCornerShape(4.dp))
                            ShimmerBox(modifier = Modifier.width(40.dp).height(14.dp))
                            ShimmerBox(modifier = Modifier.width(32.dp).height(10.dp))
                        }
                    }
                }
                // Source → destination row skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(14.dp))
                    ShimmerBox(
                        modifier = Modifier.width(46.dp).height(24.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Station rows skeleton
        ComponentCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                repeat(6) { index ->
                    ShimmerStationRow()
                    if (index < 5) {
                        // connector line between rows
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(shimmerBase)
                                )
                            }
                            Spacer(modifier = Modifier.padding(start = 32.dp, end = 24.dp).width(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerStationRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically)
                .clip(CircleShape)
                .background(shimmerBrush())
        )

        // Vertical divider
        Box(
            modifier = Modifier
                .padding(start = 32.dp, end = 24.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(shimmerBase)
        )

        // Text column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp))
        }

        // Time placeholder
        ShimmerBox(modifier = Modifier.width(36.dp).height(10.dp).align(Alignment.CenterVertically))
    }
}
