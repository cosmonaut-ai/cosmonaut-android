package com.cosmonaut.app.ui.screens.storymap

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmonaut.app.ui.theme.CosmoTheme

private val NODE_SHAPE = RoundedCornerShape(8.dp)
private val NODE_MIN_WIDTH = 160.dp
private val NODE_MAX_WIDTH = 200.dp
private val NODE_MIN_HEIGHT = NODE_HEIGHT_DP.dp

/**
 * A single node in the story map graph.
 * Mirrors the web's FlowNode.svelte with start/end/current visual states.
 */
@Composable
fun StoryMapNode(node: GraphNode, onClick: (String) -> Unit, modifier: Modifier = Modifier,) {
    val colors = CosmoTheme.colors

    val borderColor = when {
        node.isCurrent -> colors.graphCurrent
        else -> colors.border
    }

    val ringColor: Color? = when {
        node.isRoot -> colors.graphStart
        node.isEnding -> colors.graphEnd
        else -> null
    }

    val currentPulse: Float = if (node.isCurrent) {
        val transition: InfiniteTransition = rememberInfiniteTransition(label = "currentPulse")
        val pulseAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2500),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
        pulseAlpha
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .widthIn(min = NODE_MIN_WIDTH, max = NODE_MAX_WIDTH)
            .heightIn(min = NODE_MIN_HEIGHT)
            .then(
                if (ringColor != null) {
                    Modifier.drawBehind {
                        val ringOffset = 4.dp.toPx()
                        drawRoundRect(
                            color = ringColor,
                            topLeft = Offset(-ringOffset, -ringOffset),
                            size = Size(size.width + ringOffset * 2, size.height + ringOffset * 2),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (node.isCurrent) {
                    Modifier.drawBehind {
                        val glowOffset = 3.dp.toPx()
                        drawRoundRect(
                            color = colors.graphCurrent.copy(alpha = currentPulse),
                            topLeft = Offset(-glowOffset, -glowOffset),
                            size = Size(size.width + glowOffset * 2, size.height + glowOffset * 2),
                            cornerRadius = CornerRadius(11.dp.toPx()),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .shadow(
                elevation = if (node.isCurrent) 8.dp else 4.dp,
                shape = NODE_SHAPE,
                ambientColor = if (node.isCurrent) {
                    colors.graphCurrent.copy(
                        alpha = 0.2f
                    )
                } else {
                    Color.Black.copy(alpha = 0.15f)
                },
                spotColor = if (node.isCurrent) {
                    colors.graphCurrent.copy(
                        alpha = 0.3f
                    )
                } else {
                    Color.Black.copy(alpha = 0.15f)
                },
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = NODE_SHAPE,
            )
            .clickable { onClick(node.id) },
        shape = NODE_SHAPE,
        color = colors.card,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                    ),
                    color = colors.cardForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (node.isRoot || node.isEnding) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (node.isRoot) "START" else "END",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        ),
                        color = if (node.isRoot) colors.graphStart else colors.graphEnd,
                    )
                }
            }

            if (node.choiceCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${node.choiceCount} choice${if (node.choiceCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}
