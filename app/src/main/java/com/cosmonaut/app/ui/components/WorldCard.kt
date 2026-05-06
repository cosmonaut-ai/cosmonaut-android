package com.cosmonaut.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.ui.theme.CosmoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import timber.log.Timber

private const val CARD_ENTRANCE_DURATION = 400
private const val GRADIENT_PLACEHOLDER_HEIGHT = 140
private const val IMAGE_HEIGHT = 140
private const val ISO_DATE_PREFIX_LENGTH = 10

@Composable
fun WorldCard(
    world: WorldResponse,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    entranceDelay: Int = 0,
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(world.id) {
        kotlinx.coroutines.delay(entranceDelay.toLong())
        alpha.animateTo(1f, animationSpec = tween(CARD_ENTRANCE_DURATION))
    }

    GlassCard(
        modifier = modifier
            .alpha(alpha.value)
            .clickable(onClick = onCardClick),
        cornerRadius = 16.dp,
    ) {
        Column {
            WorldCardImage(world)
            WorldCardContent(
                world = world,
                onPlayClick = onPlayClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@Composable
private fun WorldCardImage(world: WorldResponse) {
    val imageUrl = world.worldImageUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IMAGE_HEIGHT.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = world.worldImageAltText ?: world.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMAGE_HEIGHT.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GRADIENT_PLACEHOLDER_HEIGHT.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CosmoTheme.colors.primary.copy(alpha = 0.3f),
                                CosmoTheme.colors.accent.copy(alpha = 0.3f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun WorldCardContent(world: WorldResponse, onPlayClick: () -> Unit, onDeleteClick: () -> Unit,) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = world.title ?: "Untitled Story",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            WorldStatusBadge(world)
        }

        if (world.genre != null) {
            Spacer(modifier = Modifier.height(4.dp))
            CosmoBadge(
                text = world.genre,
                variant = BadgeVariant.Secondary,
            )
        }

        if (world.description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = world.description,
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorldCardMetadata(world)
            WorldCardActions(
                world = world,
                onPlayClick = onPlayClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@Composable
private fun WorldStatusBadge(world: WorldResponse) {
    when {
        world.isCompleted -> {}
        world.isGenerating -> CosmoBadge(text = "Generating", variant = BadgeVariant.Warning)
        world.isFailed -> CosmoBadge(text = "Failed", variant = BadgeVariant.Destructive)
    }
}

@Composable
private fun WorldCardMetadata(world: WorldResponse) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = CosmoTheme.colors.mutedForeground,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatDate(world.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = CosmoTheme.colors.mutedForeground,
            )
        }

        if (world.contentFilter != "none") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Content filter: ${world.contentFilter}",
                    tint = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = world.contentFilter.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun WorldCardActions(world: WorldResponse, onPlayClick: () -> Unit, onDeleteClick: () -> Unit,) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        CosmoIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = "Delete world",
            onClick = onDeleteClick,
        )
        if (world.isCompleted) {
            CosmoIconButton(
                icon = Icons.Outlined.PlayArrow,
                contentDescription = "Play story",
                onClick = onPlayClick,
            )
        }
    }
}

private fun formatDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Timber.w(e, "Failed to parse date: %s", isoDate)
        isoDate.take(ISO_DATE_PREFIX_LENGTH)
    }
}
