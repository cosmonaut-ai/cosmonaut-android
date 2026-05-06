package com.cosmonaut.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private const val SHIMMER_DURATION_MS = 1200
private const val SHIMMER_START = -0.5f
private const val SHIMMER_END = 1.5f
private const val SHIMMER_WIDTH_FRACTION = 0.4f
private const val SHIMMER_HIGHLIGHT_ALPHA = 0.15f

fun Modifier.shimmer(baseColor: Color = Color.Transparent, highlightColor: Color = Color.White,): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = SHIMMER_START,
        targetValue = SHIMMER_END,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    drawWithContent {
        drawContent()
        val width = size.width
        val start = progress * width
        val end = start + width * SHIMMER_WIDTH_FRACTION

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    highlightColor.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA),
                    baseColor,
                ),
                start = Offset(start, 0f),
                end = Offset(end, 0f),
            ),
        )
    }
}
