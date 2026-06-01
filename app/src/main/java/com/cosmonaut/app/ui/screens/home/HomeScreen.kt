package com.cosmonaut.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoEmptyState
import com.cosmonaut.app.ui.components.CosmoErrorState
import com.cosmonaut.app.ui.components.DeleteConfirmationDialog
import com.cosmonaut.app.ui.components.FeaturedWorldsCarousel
import com.cosmonaut.app.ui.components.FeaturedWorldsCarouselSkeleton
import com.cosmonaut.app.ui.components.SubscriptionStatusBanner
import com.cosmonaut.app.ui.components.WorldCard
import com.cosmonaut.app.ui.components.WorldCardSkeleton
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val STAGGER_DELAY_MS = 60
private const val PREFETCH_THRESHOLD = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWorld: (String) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToStoryNode: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCreate: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToWorld -> onNavigateToWorld(event.worldId)
                is HomeEvent.NavigateToSession -> onNavigateToSession(event.sessionId)
                is HomeEvent.NavigateToStoryNode ->
                    onNavigateToStoryNode(event.sessionId, event.nodeId)
                is HomeEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshUsage()
        }
    }

    state.sessionToDelete?.let { session ->
        DeleteConfirmationDialog(
            title = "Remove Story",
            message = "Are you sure you want to delete " +
                "\"${session.world.title ?: "this story"}\"? " +
                "This action cannot be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.sessions.isEmpty() -> {
                CosmoErrorState(
                    message = state.error!!,
                    onRetry = viewModel::loadSessions,
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    WorldList(
                        state = state,
                        onSessionClick = viewModel::onSessionClick,
                        onPlayClick = viewModel::onPlayClick,
                        onDeleteClick = viewModel::requestDelete,
                        onLoadMore = viewModel::loadMore,
                        onNavigateToWorld = onNavigateToWorld,
                        onNavigateToCreate = onNavigateToCreate,
                        regionDetector = viewModel.regionDetector,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LoadingState() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "featured_skeleton", contentType = "featured_skeleton") {
            FeaturedWorldsCarouselSkeleton(
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "section_header", contentType = "header") {
            SectionHeader()
        }
        items(3) {
            WorldCardSkeleton()
        }
    }
}

@Composable
private fun SectionHeader(modifier: Modifier = Modifier) {
    Text(
        text = "Your Stories",
        style = MaterialTheme.typography.titleLarge,
        color = CosmoTheme.colors.foreground,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun WorldList(
    state: HomeUiState,
    onSessionClick: (com.cosmonaut.app.data.remote.dto.WorldSessionSummaryResponse) -> Unit,
    onPlayClick: (com.cosmonaut.app.data.remote.dto.WorldSessionSummaryResponse) -> Unit,
    onDeleteClick: (com.cosmonaut.app.data.remote.dto.WorldSessionSummaryResponse) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToWorld: (String) -> Unit,
    onNavigateToCreate: (() -> Unit)? = null,
    regionDetector: com.cosmonaut.app.data.billing.RegionDetector? = null,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - PREFETCH_THRESHOLD && state.hasMore && !state.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.usage != null && regionDetector != null) {
            item(key = "subscription_banner", contentType = "banner") {
                SubscriptionStatusBanner(
                    usage = state.usage,
                    regionDetector = regionDetector,
                )
            }
        }

        if (state.isLoadingFeatured) {
            item(key = "featured_skeleton", contentType = "featured_skeleton") {
                FeaturedWorldsCarouselSkeleton(
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        } else if (state.featuredWorlds.isNotEmpty()) {
            item(key = "featured_carousel", contentType = "featured") {
                FeaturedWorldsCarousel(
                    worlds = state.featuredWorlds,
                    onWorldClick = onNavigateToWorld,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        item(key = "section_header", contentType = "header") {
            SectionHeader()
        }

        if (state.sessions.isEmpty()) {
            item(key = "empty_worlds", contentType = "empty") {
                CosmoEmptyState(
                    title = "No Stories Yet",
                    subtitle = "Create your first interactive story\nand start exploring!",
                    action = if (onNavigateToCreate != null) {
                        {
                            CosmoButton(
                                text = "Create a Story",
                                onClick = onNavigateToCreate,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        } else {
            itemsIndexed(
                items = state.sessions,
                key = { _, session -> session.id },
                contentType = { _, _ -> "world_card" },
            ) { index, session ->
                WorldCard(
                    world = session.world,
                    onCardClick = { onSessionClick(session) },
                    onPlayClick = { onPlayClick(session) },
                    onDeleteClick = { onDeleteClick(session) },
                    entranceDelay = index * STAGGER_DELAY_MS,
                )
            }

            if (state.isLoadingMore) {
                item(key = "loading_more", contentType = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = CosmoTheme.colors.primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
