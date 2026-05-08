@file:Suppress("MatchingDeclarationName")

package com.cosmonaut.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

enum class CosmoButtonVariant {
    Primary,
    Secondary,
    Outline,
    Destructive,
    Ghost,
}

private const val DISABLED_FACE_ALPHA = 0.35f
private const val DISABLED_TEXT_ALPHA = 0.5f
private val DEPTH_SIZE = 5.dp
private val DEPTH_PRESSED = 2.5.dp
private val BUTTON_HEIGHT = 40.dp
private val BUTTON_CORNER = 8.dp
private val ICON_BUTTON_CORNER = 10.dp

private data class ButtonColors(
    val face: Color,
    val depth: Color,
    val content: Color,
    val ripple: Color,
    val border: Color?,
    val useShadowDepth: Boolean = false,
)

@Composable
private fun resolveColors(variant: CosmoButtonVariant): ButtonColors {
    val colors = CosmoTheme.colors
    return when (variant) {
        CosmoButtonVariant.Primary -> ButtonColors(
            face = colors.primary,
            depth = colors.primaryDepth,
            content = colors.primaryForeground,
            ripple = colors.primaryForeground,
            border = null,
        )
        CosmoButtonVariant.Secondary -> ButtonColors(
            face = colors.secondary,
            depth = colors.secondaryDepth,
            content = colors.secondaryForeground,
            ripple = colors.secondaryForeground,
            border = null,
        )
        CosmoButtonVariant.Outline -> ButtonColors(
            face = colors.outline,
            depth = colors.outlineDepth,
            content = colors.outlineForeground,
            ripple = colors.outlineForeground,
            border = colors.outlineBorder,
            useShadowDepth = true,
        )
        CosmoButtonVariant.Destructive -> ButtonColors(
            face = colors.destructive,
            depth = colors.destructiveDepth,
            content = colors.destructiveForeground,
            ripple = colors.destructiveForeground,
            border = null,
        )
        CosmoButtonVariant.Ghost -> ButtonColors(
            face = Color.Transparent,
            depth = Color.Transparent,
            content = colors.mutedForeground,
            ripple = colors.mutedForeground,
            border = null,
        )
    }
}

@Composable
fun CosmoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CosmoButtonVariant = CosmoButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val effectiveEnabled = enabled && !isLoading
    val buttonColors = resolveColors(variant)
    val bgColor = CosmoTheme.colors.background
    val isGhost = variant == CosmoButtonVariant.Ghost

    val translateY by animateDpAsState(
        targetValue = if (isPressed && effectiveEnabled) DEPTH_PRESSED else 0.dp,
        label = "depthTranslateY",
    )

    val currentFace = if (effectiveEnabled) {
        buttonColors.face
    } else {
        buttonColors.face.copy(alpha = DISABLED_FACE_ALPHA).compositeOver(bgColor)
    }
    val currentDepth = if (effectiveEnabled) {
        buttonColors.depth
    } else {
        buttonColors.depth.copy(alpha = DISABLED_FACE_ALPHA).compositeOver(bgColor)
    }
    val currentContent = if (effectiveEnabled) {
        buttonColors.content
    } else {
        buttonColors.content.copy(alpha = DISABLED_TEXT_ALPHA)
    }

    val shape = RoundedCornerShape(BUTTON_CORNER)
    val depthHeight = if (isGhost) 0.dp else DEPTH_SIZE
    val useShadow = buttonColors.useShadowDepth

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT + depthHeight)
            .then(
                if (!isGhost && useShadow) {
                    Modifier
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            val cornerPx = BUTTON_CORNER.toPx()
                            val depthSizePx = DEPTH_SIZE.toPx()
                            val buttonHeightPx = size.height - depthSizePx
                            val translateYPx = translateY.toPx()

                            drawRoundRect(
                                color = currentDepth,
                                topLeft = Offset(0f, depthSizePx),
                                size = Size(size.width, buttonHeightPx),
                                cornerRadius = CornerRadius(cornerPx, cornerPx),
                            )
                            drawRoundRect(
                                color = Color.Black,
                                topLeft = Offset(0f, translateYPx),
                                size = Size(size.width, buttonHeightPx),
                                cornerRadius = CornerRadius(cornerPx, cornerPx),
                                blendMode = BlendMode.DstOut,
                            )

                            drawContent()
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        if (!isGhost && !useShadow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUTTON_HEIGHT)
                    .align(Alignment.BottomCenter)
                    .clip(shape)
                    .background(currentDepth),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .offset { androidx.compose.ui.unit.IntOffset(0, translateY.roundToPx()) }
                .clip(shape)
                .then(if (!isGhost) Modifier.background(currentFace) else Modifier)
                .then(
                    if (buttonColors.border != null) {
                        Modifier.border(1.dp, buttonColors.border, shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = buttonColors.ripple),
                    enabled = effectiveEnabled,
                    role = Role.Button,
                    onClick = { if (!isLoading) onClick() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides currentContent) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = currentContent,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun CosmoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CosmoButtonVariant = CosmoButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    CosmoButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled,
        isLoading = isLoading,
    ) {
        val colors = resolveColors(variant)
        val contentColor = if (enabled && !isLoading) {
            colors.content
        } else {
            colors.content.copy(alpha = DISABLED_TEXT_ALPHA)
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun CosmoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CosmoButtonVariant = CosmoButtonVariant.Secondary,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonColors = resolveColors(variant)

    val translateY by animateDpAsState(
        targetValue = if (isPressed) DEPTH_PRESSED else 0.dp,
        label = "iconBtnTranslateY",
    )

    val shape = RoundedCornerShape(ICON_BUTTON_CORNER)

    Box(
        modifier = modifier.size(size, size + DEPTH_SIZE),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(buttonColors.depth),
        )

        Box(
            modifier = Modifier
                .size(size)
                .offset { androidx.compose.ui.unit.IntOffset(0, translateY.roundToPx()) }
                .clip(shape)
                .background(buttonColors.face)
                .then(
                    if (buttonColors.border != null) {
                        Modifier.border(1.dp, buttonColors.border, shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = buttonColors.ripple),
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = buttonColors.content,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
