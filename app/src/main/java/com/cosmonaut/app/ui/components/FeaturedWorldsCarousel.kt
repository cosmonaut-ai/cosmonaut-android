package com.cosmonaut.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.ui.theme.CosmoMotion
import com.cosmonaut.app.ui.theme.CosmoTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CARD_WIDTH_DP = 220
private const val IMAGE_HEIGHT_DP = 120
private const val ENTRANCE_DURATION_MS = 400
private const val STAGGER_DELAY_MS = 50L
private val EntranceEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
fun FeaturedWorldsCarousel(
    worlds: ImmutableList<WorldResponse>,
    onWorldClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (worlds.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Featured Stories",
            style = MaterialTheme.typography.titleMedium,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp),
        ) {
            itemsIndexed(
                items = worlds,
                key = { _, world -> "featured_${world.id}" },
                contentType = { _, _ -> "featured_card" },
            ) { index, world ->
                FeaturedWorldCard(
                    world = world,
                    onClick = { onWorldClick(world.id) },
                    entranceIndex = index,
                )
            }
        }
    }
}

@Composable
private fun FeaturedWorldCard(
    world: WorldResponse,
    onClick: () -> Unit,
    entranceIndex: Int,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = CosmoMotion.config.isReducedMotion
    val alpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val offsetX = remember { Animatable(if (isReducedMotion) 0f else 16f) }

    LaunchedEffect(world.id) {
        if (!isReducedMotion) {
            delay(entranceIndex * STAGGER_DELAY_MS)
            coroutineScope {
                launch {
                    alpha.animateTo(
                        1f,
                        animationSpec = tween(ENTRANCE_DURATION_MS, easing = EntranceEasing),
                    )
                }
                launch {
                    offsetX.animateTo(
                        0f,
                        animationSpec = tween(ENTRANCE_DURATION_MS, easing = EntranceEasing),
                    )
                }
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "featuredCardScale",
    )

    val cardDesc = "${world.title ?: "Untitled Story"}. ${world.genre ?: "Featured story"}."

    GlassCard(
        modifier = modifier
            .width(CARD_WIDTH_DP.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                translationX = offsetX.value * density
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = cardDesc
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        cornerRadius = 14.dp,
    ) {
        Column {
            FeaturedCardImage(world)
            FeaturedCardContent(world)
        }
    }
}

@Composable
private fun FeaturedCardImage(world: WorldResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IMAGE_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
    ) {
        if (world.worldImageUrl != null) {
            AsyncImage(
                model = world.worldImageUrl,
                contentDescription = world.worldImageAltText
                    ?: "Cover for ${world.title ?: "story"}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMAGE_HEIGHT_DP.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMAGE_HEIGHT_DP.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CosmoTheme.colors.primary.copy(alpha = 0.2f),
                                CosmoTheme.colors.accent.copy(alpha = 0.2f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun FeaturedCardContent(world: WorldResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Text(
            text = world.title ?: "Untitled Story",
            style = MaterialTheme.typography.titleSmall,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (world.genre != null) {
            Spacer(modifier = Modifier.height(4.dp))
            CosmoBadge(
                text = world.genre,
                variant = BadgeVariant.Secondary,
            )
        }

        if (world.description != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = world.description,
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}
