package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Gold blinking cursor shown inline after streaming text.
 * Matches the web's `.story-cursor` animation.
 */
@Composable
fun BlinkingCursor(modifier: Modifier = Modifier) {
    val cursorColor = CosmoTheme.colors.primary
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "cursor")

    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )

    Canvas(modifier = modifier.size(width = 3.dp, height = 20.dp)) {
        drawRect(
            color = cursorColor.copy(alpha = alpha),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
        )
    }
}
