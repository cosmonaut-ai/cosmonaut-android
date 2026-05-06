@file:Suppress("MatchingDeclarationName")

package com.cosmonaut.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val ANIMATION_DURATION_MS = 200
private const val UNSELECTED_BG_ALPHA = 0f
private const val BORDER_ALPHA = 0.5f

data class SegmentOption<T>(val value: T, val label: String, val description: String? = null,)

@Composable
fun <T> CosmoSegmentedControl(
    options: List<SegmentOption<T>>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmoTheme.colors.outline)
                .border(
                    width = 1.dp,
                    color = CosmoTheme.colors.outline,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        CosmoTheme.colors.card
                    } else {
                        CosmoTheme.colors.muted.copy(alpha = UNSELECTED_BG_ALPHA)
                    },
                    animationSpec = tween(ANIMATION_DURATION_MS),
                    label = "segmentBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        CosmoTheme.colors.foreground
                    } else {
                        CosmoTheme.colors.mutedForeground
                    },
                    animationSpec = tween(ANIMATION_DURATION_MS),
                    label = "segmentText",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onValueChange(option.value) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                        if (option.description != null) {
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
