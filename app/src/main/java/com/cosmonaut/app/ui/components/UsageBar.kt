package com.cosmonaut.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * A progress bar showing resource usage (used/limit) with label.
 * Matches the web's UsageBar component behavior.
 */
@Composable
fun UsageBar(
    label: String,
    used: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "usage_progress",
    )

    val barColor = when {
        fraction >= 1f -> CosmoTheme.colors.destructive
        fraction >= NEAR_LIMIT_THRESHOLD -> amberWarningColor
        else -> CosmoTheme.colors.primary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "$used / $limit",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    fraction >= 1f -> CosmoTheme.colors.destructive
                    fraction >= NEAR_LIMIT_THRESHOLD -> amberWarningColor
                    else -> CosmoTheme.colors.mutedForeground
                },
                fontWeight = if (fraction >= NEAR_LIMIT_THRESHOLD) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CosmoTheme.colors.muted),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor),
            )
        }

        if (fraction >= 1f) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Limit reached",
                style = MaterialTheme.typography.labelSmall,
                color = CosmoTheme.colors.destructive,
            )
        } else if (fraction >= NEAR_LIMIT_THRESHOLD) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Approaching limit",
                style = MaterialTheme.typography.labelSmall,
                color = amberWarningColor,
            )
        }
    }
}

private const val NEAR_LIMIT_THRESHOLD = 0.8f

private val amberWarningColor = Color(0xFFF59E0B)
