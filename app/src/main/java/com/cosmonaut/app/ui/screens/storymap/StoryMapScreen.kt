package com.cosmonaut.app.ui.screens.storymap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme

internal const val NODE_WIDTH_DP = 200f
internal const val NODE_HEIGHT_DP = 58f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryMapScreen(
    onNavigateToNode: (worldId: String, nodeId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: StoryMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmoTheme.colors.foreground,
                        )
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmoTheme.colors.background,
                ),
            )
        },
        containerColor = CosmoTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is StoryMapUiState.Loading -> LoadingContent()

                is StoryMapUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::loadNodes,
                )

                is StoryMapUiState.Empty -> EmptyContent(
                    rootNodeId = state.rootNodeId,
                    onEnterStory = { nodeId ->
                        onNavigateToNode(viewModel.worldId, nodeId)
                    },
                )

                is StoryMapUiState.Success -> {
                    StoryMapGraph(
                        graphData = state.graphData,
                        currentNodeId = state.graphData.nodes.find { it.isCurrent }?.id,
                        onNodeClick = { nodeId ->
                            onNavigateToNode(viewModel.worldId, nodeId)
                        },
                    )

                    MapLegend(
                        hasCurrentNode = state.hasCurrentNode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLegend(hasCurrentNode: Boolean, modifier: Modifier = Modifier,) {
    val colors = CosmoTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendBadge(
            label = "Start",
            color = colors.graphStart,
        )
        LegendBadge(
            label = "End",
            color = colors.graphEnd,
        )
        if (hasCurrentNode) {
            LegendBadge(
                label = "Current",
                color = colors.graphCurrent,
            )
        }
    }
}

@Composable
private fun LegendBadge(label: String, color: androidx.compose.ui.graphics.Color,) {
    val bgColor = color.copy(alpha = 0.15f)

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        ),
        color = color,
        modifier = Modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(50),
            )
            .border(
                width = 2.dp,
                color = color,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                color = CosmoTheme.colors.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Loading story map...",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit,) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.destructive,
                textAlign = TextAlign.Center,
            )
            CosmoButton(
                onClick = onRetry,
                variant = CosmoButtonVariant.Outline,
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent(rootNodeId: String?, onEnterStory: (String) -> Unit,) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.4f),
                        shape = CircleShape,
                    )
                    .background(CosmoTheme.colors.muted.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Rocket,
                    contentDescription = null,
                    tint = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "No story nodes yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CosmoTheme.colors.mutedForeground,
                )
                Text(
                    text = "Start your adventure to build out the story map",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.7f),
                )
            }

            if (rootNodeId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                CosmoButton(
                    onClick = { onEnterStory(rootNodeId) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter the Story")
                }
            }
        }
    }
}
