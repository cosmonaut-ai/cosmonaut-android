@file:Suppress("MatchingDeclarationName", "TooManyFunctions")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmonaut.app.ui.screens.world

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.cosmonaut.app.data.remote.dto.CharacterResponse
import com.cosmonaut.app.data.remote.dto.LocationResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.ui.components.BadgeVariant
import com.cosmonaut.app.ui.components.CosmoBadge
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.CosmoErrorState
import com.cosmonaut.app.ui.components.GlassCard
import com.cosmonaut.app.ui.screens.share.ShareBottomSheet
import com.cosmonaut.app.ui.screens.share.ShareBottomSheetViewModel
import com.cosmonaut.app.ui.theme.CosmoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val BACK_BUTTON_BG_ALPHA = 0.5f
private const val CHAR_INITIAL_BG_ALPHA = 0.2f
private const val DIVIDER_ALPHA = 0.15f
private const val SPOILER_HIDDEN_ALPHA = 0.04f
private const val GRADIENT_MIDPOINT = 0.4f
private const val GRADIENT_FADE_START = 0.85f
private const val GRADIENT_FADE_ALPHA = 0.9f
private const val COPIED_FEEDBACK_MS = 2000L
private const val DATE_FALLBACK_LENGTH = 10
private const val CAROUSEL_CARD_WIDTH = 220
private const val SPOILER_CARD_WIDTH = 240

@Composable
fun WorldHomeScreen(
    onNavigateToStoryNode: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToMap: (worldId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WorldHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showShareSheet by remember { mutableStateOf(false) }
    val shareViewModel: ShareBottomSheetViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorldHomeEvent.NavigateToStoryNode ->
                    onNavigateToStoryNode(event.worldId, event.nodeId)
                is WorldHomeEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short,
                )
                is WorldHomeEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = state) {
            is WorldHomeUiState.Loading -> LoadingState()
            is WorldHomeUiState.Error -> {
                CosmoErrorState(
                    message = currentState.message,
                    onRetry = viewModel::loadWorld,
                )
            }
            is WorldHomeUiState.Generating -> GeneratingState(currentState.world)
            is WorldHomeUiState.Failed -> FailedState(
                world = currentState.world,
                onRetry = viewModel::retryGeneration,
                onDelete = viewModel::deleteWorld,
            )
            is WorldHomeUiState.Ready -> ReadyState(
                world = currentState.world,
                currentNodeId = currentState.currentNodeId,
                onContinue = viewModel::onContinueStory,
                onBack = onNavigateBack,
                onViewMap = { onNavigateToMap(currentState.world.id) },
                onShare = { showShareSheet = true },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showShareSheet) {
        val world = (state as? WorldHomeUiState.Ready)?.world
        if (world != null) {
            ShareBottomSheet(
                world = world,
                currentUserId = viewModel.currentUserId,
                viewModel = shareViewModel,
                onDismiss = { showShareSheet = false },
                onWorldUpdate = viewModel::onWorldUpdated,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = CosmoTheme.colors.primary)
    }
}

@Composable
private fun GeneratingState(world: WorldResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = CosmoTheme.colors.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Generating Your Story",
            style = MaterialTheme.typography.headlineSmall,
            color = CosmoTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = world.generationStatusDisplay,
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your story is being crafted in the " +
                "background. Feel free to leave and come " +
                "back \u2014 it\u2019ll be ready when you return.",
            style = MaterialTheme.typography.bodySmall,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FailedState(world: WorldResponse, onRetry: () -> Unit, onDelete: () -> Unit,) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Generation Failed",
            style = MaterialTheme.typography.headlineSmall,
            color = CosmoTheme.colors.destructive,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Something went wrong while generating " +
                "\"${world.title ?: "your story"}\". " +
                "You can try again or delete this world.",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        CosmoButton(
            text = "Retry",
            onClick = onRetry,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CosmoButton(
            text = "Delete World",
            onClick = onDelete,
            variant = CosmoButtonVariant.Destructive,
        )
    }
}

@Composable
private fun ReadyState(
    world: WorldResponse,
    currentNodeId: String?,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onViewMap: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HeroSection(world)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                StatsStrip(world)
                ActionRow(
                    hasProgress = currentNodeId != null,
                    onContinue = onContinue,
                    onViewMap = onViewMap,
                    onShare = onShare,
                )
                if (world.worldPrompt != null) {
                    OriginPromptCard(prompt = world.worldPrompt)
                }
                if (!world.characters.isNullOrEmpty()) {
                    CharactersCarousel(world.characters)
                }
                if (!world.locations.isNullOrEmpty()) {
                    LocationsCarousel(world.locations)
                }
                if (!world.potentialEndings.isNullOrEmpty()) {
                    EndingsCarousel(world.potentialEndings)
                }
                WorldFooter(world)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        FloatingBackButton(onClick = onBack)
    }
}

@Composable
private fun FloatingBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp, top = 8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(
                CosmoTheme.colors.background.copy(alpha = BACK_BUTTON_BG_ALPHA),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CosmoTheme.colors.foreground,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Hero ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroSection(world: WorldResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val imageUrl = world.worldImageUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = world.worldImageAltText ?: world.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CosmoTheme.colors.secondary,
                                CosmoTheme.colors.accent,
                            ),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            GRADIENT_MIDPOINT to Color.Transparent,
                            GRADIENT_FADE_START to CosmoTheme.colors.background
                                .copy(alpha = GRADIENT_FADE_ALPHA),
                            1f to CosmoTheme.colors.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                world.genre?.let {
                    CosmoBadge(text = it, variant = BadgeVariant.Default)
                }
                world.visibility?.let { vis ->
                    val (icon, label) = when (vis) {
                        "public" -> Icons.Outlined.Public to "Public"
                        "unlisted" -> Icons.Outlined.Share to "Unlisted"
                        else -> Icons.Outlined.Lock to "Private"
                    }
                    CosmoBadge(
                        text = label,
                        variant = BadgeVariant.Secondary,
                        icon = icon,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = world.title ?: "Untitled Story",
                style = MaterialTheme.typography.headlineMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Bold,
            )

            if (world.description != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = world.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmoTheme.colors.mutedForeground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Stats ───────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(world: WorldResponse) {
    val items = buildList {
        world.worldLength?.let {
            add(it.replaceFirstChar { c -> c.uppercase() } to "length")
        }
        add(world.vocabLevel.replaceFirstChar { it.uppercase() } to "vocab")
        if (world.contentFilter != "none") {
            add(world.contentFilter.replaceFirstChar { it.uppercase() } to "filter")
        }
        world.score?.let { add(it to "score") }
        world.storyMaxNodes?.let { add("$it chapters" to "depth") }
        world.nodeTextLength?.let { add("~$it words/page" to "pace") }
    }

    if (items.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (value, _) ->
            if (index > 0) {
                Text(
                    text = "\u00B7",
                    color = CosmoTheme.colors.mutedForeground,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                text = value,
                color = CosmoTheme.colors.mutedForeground,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Actions ─────────────────────────────────────────────────────────────

@Composable
private fun ActionRow(hasProgress: Boolean, onContinue: () -> Unit, onViewMap: () -> Unit = {}, onShare: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        CosmoButton(
            onClick = onContinue,
            variant = CosmoButtonVariant.Primary,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = CosmoTheme.colors.primaryForeground,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (hasProgress) "Continue" else "Start Story",
                color = CosmoTheme.colors.primaryForeground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        CosmoButton(
            onClick = onViewMap,
            variant = CosmoButtonVariant.Outline,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                tint = CosmoTheme.colors.outlineForeground,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "View Map",
                color = CosmoTheme.colors.outlineForeground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        CosmoButton(
            onClick = onShare,
            variant = CosmoButtonVariant.Ghost,
            modifier = Modifier.weight(0.6f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                tint = CosmoTheme.colors.mutedForeground,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Share",
                color = CosmoTheme.colors.mutedForeground,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ── Origin Prompt ───────────────────────────────────────────────────────

@Composable
private fun OriginPromptCard(prompt: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }

    SectionHeader(icon = Icons.Outlined.AutoStories, title = "Origin Prompt")

    GlassCard(cornerRadius = 12.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val copyIcon = if (copied) {
                    Icons.Outlined.Check
                } else {
                    Icons.Outlined.ContentCopy
                }
                val copyTint = if (copied) {
                    Color(0xFF22C55E)
                } else {
                    CosmoTheme.colors.mutedForeground
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val cm = context.getSystemService(
                                Context.CLIPBOARD_SERVICE,
                            ) as ClipboardManager
                            cm.setPrimaryClip(
                                ClipData.newPlainText("prompt", prompt),
                            )
                            copied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = copyIcon,
                        contentDescription = "Copy prompt",
                        tint = copyTint,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = copyTint,
                    )
                }
            }
        }
    }
}

// ── Characters Carousel ─────────────────────────────────────────────────

@Composable
private fun CharactersCarousel(characters: List<CharacterResponse>) {
    SectionHeader(icon = Icons.Outlined.Person, title = "Characters")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 16.dp),
    ) {
        itemsIndexed(characters) { _, character ->
            val name = character.name ?: return@itemsIndexed
            GlassCard(cornerRadius = 12.dp) {
                Column(
                    modifier = Modifier
                        .width(CAROUSEL_CARD_WIDTH.dp)
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    CosmoTheme.colors.primary.copy(
                                        alpha = CHAR_INITIAL_BG_ALPHA,
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name.first().uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = CosmoTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmoTheme.colors.foreground,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (character.description != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = character.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmoTheme.colors.mutedForeground,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ── Locations Carousel ──────────────────────────────────────────────────

@Composable
private fun LocationsCarousel(locations: List<LocationResponse>) {
    SectionHeader(icon = Icons.Outlined.Place, title = "Locations")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 16.dp),
    ) {
        itemsIndexed(locations) { _, location ->
            val name = location.name ?: return@itemsIndexed
            GlassCard(cornerRadius = 12.dp) {
                Column(
                    modifier = Modifier
                        .width(CAROUSEL_CARD_WIDTH.dp)
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = CosmoTheme.colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmoTheme.colors.foreground,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (location.description != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = location.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmoTheme.colors.mutedForeground,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ── Endings Carousel ────────────────────────────────────────────────────

@Composable
private fun EndingsCarousel(endings: List<String>) {
    SectionHeader(icon = Icons.Outlined.Visibility, title = "Possible Endings")

    Text(
        text = "Contains spoilers \u2014 tap to reveal",
        style = MaterialTheme.typography.labelSmall,
        color = CosmoTheme.colors.mutedForeground,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 16.dp),
    ) {
        itemsIndexed(endings) { index, ending ->
            SpoilerCard(ending = ending, index = index + 1)
        }
    }
}

@Composable
private fun SpoilerCard(ending: String, index: Int) {
    var revealed by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else SPOILER_HIDDEN_ALPHA,
        label = "spoilerAlpha",
    )

    GlassCard(cornerRadius = 12.dp) {
        Box(
            modifier = Modifier
                .width(SPOILER_CARD_WIDTH.dp)
                .clickable { revealed = !revealed }
                .padding(14.dp),
        ) {
            Column {
                Text(
                    text = "Ending $index",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ending,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha
                    },
                )
            }

            AnimatedVisibility(
                visible = !revealed,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = CosmoTheme.colors.mutedForeground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Tap to reveal",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmoTheme.colors.mutedForeground,
                    )
                }
            }
        }
    }
}

// ── Footer ──────────────────────────────────────────────────────────────

@Composable
private fun WorldFooter(world: WorldResponse) {
    if (world.createdAt.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CosmoTheme.colors.border.copy(alpha = DIVIDER_ALPHA)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        FooterItem(
            icon = Icons.Outlined.CalendarToday,
            text = "Created ${formatDate(world.createdAt)}",
        )
    }
}

@Composable
private fun FooterItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = CosmoTheme.colors.mutedForeground,
        )
    }
}

// ── Shared helpers ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmoTheme.colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    CosmoTheme.colors.border.copy(alpha = DIVIDER_ALPHA),
                ),
        )
    }
}

private fun formatDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        isoDate.take(DATE_FALLBACK_LENGTH)
    }
}
