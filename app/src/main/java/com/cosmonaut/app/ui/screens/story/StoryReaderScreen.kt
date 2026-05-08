package com.cosmonaut.app.ui.screens.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.data.remote.dto.ChoiceResponse
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import com.cosmonaut.app.ui.screens.audio.AudioNarrationViewModel
import com.cosmonaut.app.ui.screens.share.ShareBottomSheet
import com.cosmonaut.app.ui.screens.share.ShareBottomSheetViewModel
import com.cosmonaut.app.ui.screens.story.components.ChoiceList
import com.cosmonaut.app.ui.screens.story.components.EndingCard
import com.cosmonaut.app.ui.screens.story.components.NodeFailedCard
import com.cosmonaut.app.ui.screens.story.components.StoryText
import com.cosmonaut.app.ui.screens.story.components.TypewriterText
import com.cosmonaut.app.ui.screens.story.components.UpgradePromptDialog
import com.cosmonaut.app.ui.screens.story.components.WrongSessionCard
import com.cosmonaut.app.ui.theme.CosmoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    onNavigateToNode: (worldId: String, nodeId: String) -> Unit,
    onNavigateToParent: (worldId: String, nodeId: String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToMap: (worldId: String, currentNodeId: String?) -> Unit,
    audioViewModel: AudioNarrationViewModel? = null,
    viewModel: StoryReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasParent by viewModel.hasParent.collectAsState()
    val choiceInProgress by viewModel.isChoiceInProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuotaPrompt by remember { mutableStateOf(false) }
    var showAudioQuotaPrompt by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val shareViewModel: ShareBottomSheetViewModel = hiltViewModel()
    val worldForShare by viewModel.worldForShare.collectAsState()

    // Update audio ViewModel with current node context
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is StoryReaderUiState.Content && audioViewModel != null) {
            audioViewModel.setNodeContext(
                worldId = viewModel.worldId,
                nodeId = state.node.id,
                nodeTitle = state.node.title,
                nodeTextLength = state.node.text?.length ?: 0,
                isCompleted = state.node.isCompleted,
                audioEntries = state.node.audio,
            )
        }
    }

    // Collect audio events
    val audioEvent by audioViewModel?.events?.collectAsState() ?: remember { mutableStateOf(null) }
    LaunchedEffect(audioEvent) {
        when (val event = audioEvent) {
            is com.cosmonaut.app.ui.screens.audio.AudioNarrationEvent.Error -> {
                snackbarHostState.showSnackbar(event.message)
                audioViewModel?.consumeEvent()
            }
            is com.cosmonaut.app.ui.screens.audio.AudioNarrationEvent.QuotaExceeded -> {
                showAudioQuotaPrompt = true
                audioViewModel?.consumeEvent()
            }
            is com.cosmonaut.app.ui.screens.audio.AudioNarrationEvent.RateLimited -> {
                snackbarHostState.showSnackbar(event.message)
                audioViewModel?.consumeEvent()
            }
            null -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StoryReaderEvent.NavigateToNode -> {
                    onNavigateToNode(event.worldId, event.nodeId)
                }
                is StoryReaderEvent.NavigateToParent -> {
                    onNavigateToParent(event.worldId, event.nodeId)
                }
                is StoryReaderEvent.NavigateToDashboard -> onNavigateToDashboard()
                is StoryReaderEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is StoryReaderEvent.ShowQuotaPrompt -> {
                    showQuotaPrompt = true
                }
            }
        }
    }

    val usage by viewModel.usage.collectAsState()

    if (showQuotaPrompt) {
        UpgradePromptDialog(
            resource = "nodes",
            regionDetector = viewModel.regionDetector,
            usage = usage,
            onDismiss = { showQuotaPrompt = false },
        )
    }

    if (showAudioQuotaPrompt) {
        UpgradePromptDialog(
            resource = "audio",
            regionDetector = viewModel.regionDetector,
            usage = usage,
            onDismiss = { showAudioQuotaPrompt = false },
        )
    }

    val narrationAvailability by audioViewModel?.narrationAvailability?.collectAsState()
        ?: remember { mutableStateOf(com.cosmonaut.app.ui.screens.audio.NarrationAvailability()) }
    val isAudioEnabled = narrationAvailability.isEnabled
    val audioDisabledMessage = narrationAvailability.disabledMessage

    Scaffold(
        topBar = {
            StoryTopBar(
                onHome = { viewModel.navigateToDashboard() },
                onUndo = { viewModel.navigateToParent() },
                undoEnabled = hasParent,
                onMap = { onNavigateToMap(viewModel.worldId, viewModel.nodeId) },
                onAudio = { audioViewModel?.toggleNarration() },
                audioEnabled = isAudioEnabled,
                audioDisabledMessage = audioDisabledMessage,
                onShare = {
                    viewModel.loadWorldForShare()
                    showShareSheet = true
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmoTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        StoryContent(
            uiState = uiState,
            worldId = viewModel.worldId,
            isChoiceInProgress = choiceInProgress,
            onChoiceSelected = viewModel::onChoiceSelected,
            onCustomChoice = viewModel::onCustomChoice,
            onRetry = viewModel::retryGeneration,
            onGoBack = { viewModel.navigateToParent() },
            onDashboard = { viewModel.navigateToDashboard() },
            onViewMap = { onNavigateToMap(viewModel.worldId, viewModel.nodeId) },
            onStartOver = { viewModel.navigateToDashboard() },
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showShareSheet && worldForShare != null) {
        ShareBottomSheet(
            world = worldForShare!!,
            currentUserId = viewModel.currentUserId,
            viewModel = shareViewModel,
            onDismiss = { showShareSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryTopBar(
    onHome: () -> Unit,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
    onMap: () -> Unit,
    onAudio: () -> Unit,
    audioEnabled: Boolean,
    audioDisabledMessage: String?,
    onShare: () -> Unit = {},
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            Row {
                IconButton(onClick = onHome) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Back to dashboard",
                        tint = CosmoTheme.colors.foreground,
                    )
                }
                IconButton(onClick = onUndo, enabled = undoEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "Previous node",
                        tint = if (undoEnabled) {
                            CosmoTheme.colors.foreground
                        } else {
                            CosmoTheme.colors.mutedForeground.copy(alpha = 0.4f)
                        },
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onMap) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = "Story map",
                    tint = CosmoTheme.colors.mutedForeground,
                )
            }
            IconButton(
                onClick = onAudio,
                enabled = audioEnabled,
                modifier = Modifier.semantics {
                    if (!audioEnabled && audioDisabledMessage != null) {
                        stateDescription = audioDisabledMessage
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = if (audioEnabled) {
                        "Toggle audio narration"
                    } else {
                        "Audio narration unavailable: ${audioDisabledMessage ?: "disabled"}"
                    },
                    tint = if (audioEnabled) {
                        CosmoTheme.colors.foreground
                    } else {
                        CosmoTheme.colors.mutedForeground.copy(alpha = 0.4f)
                    },
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = CosmoTheme.colors.mutedForeground,
                )
            }
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CosmoTheme.colors.background,
        ),
    )
}

@Composable
private fun StoryContent(
    uiState: StoryReaderUiState,
    worldId: String,
    isChoiceInProgress: Boolean,
    onChoiceSelected: (String) -> Unit,
    onCustomChoice: (String) -> Unit,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onDashboard: () -> Unit,
    onViewMap: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        when (uiState) {
            is StoryReaderUiState.Loading -> {
                LoadingState()
            }

            is StoryReaderUiState.Streaming -> {
                StreamingContent(
                    text = uiState.text,
                    parentChoice = uiState.parentChoice,
                    isWaitingForFirstToken = uiState.isWaitingForFirstToken,
                )
            }

            is StoryReaderUiState.RemoteGenerating -> {
                RemoteGeneratingContent(parentChoice = uiState.parentChoice)
            }

            is StoryReaderUiState.Content -> {
                CompletedContent(
                    node = uiState.node,
                    isEnding = uiState.isEnding,
                    isChoiceInProgress = isChoiceInProgress,
                    onChoiceSelected = onChoiceSelected,
                    onCustomChoice = onCustomChoice,
                    onStartOver = onStartOver,
                )
            }

            is StoryReaderUiState.Failed -> {
                NodeFailedCard(
                    canRetry = uiState.canRetry,
                    onRetry = onRetry,
                    onGoBack = onGoBack,
                    onDashboard = onDashboard,
                )
            }

            is StoryReaderUiState.WrongSession -> {
                WrongSessionCard(
                    onViewMap = onViewMap,
                    onDashboard = onDashboard,
                )
            }

            is StoryReaderUiState.Error -> {
                NodeFailedCard(
                    canRetry = true,
                    onRetry = onRetry,
                    onGoBack = onGoBack,
                    onDashboard = onDashboard,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = CosmoTheme.colors.primary,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun StreamingContent(text: String, parentChoice: ChoiceResponse?, isWaitingForFirstToken: Boolean,) {
    Column {
        ParentChoiceBanner(parentChoice)

        if (isWaitingForFirstToken) {
            Spacer(modifier = Modifier.height(32.dp))
            TypewriterText()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            StoryText(text = text, showCursor = true)
        }
    }
}

@Composable
private fun RemoteGeneratingContent(parentChoice: ChoiceResponse?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ParentChoiceBanner(parentChoice)

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            color = CosmoTheme.colors.primary,
            modifier = Modifier.size(32.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Another session is generating this story...",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = CosmoTheme.colors.mutedForeground,
        )
    }
}

@Composable
private fun CompletedContent(
    node: StoryNodeResponse,
    isEnding: Boolean,
    isChoiceInProgress: Boolean,
    onChoiceSelected: (String) -> Unit,
    onCustomChoice: (String) -> Unit,
    onStartOver: () -> Unit,
) {
    Column {
        ParentChoiceBanner(node.parentChoice)

        Spacer(modifier = Modifier.height(16.dp))

        if (node.text != null) {
            StoryText(text = node.text)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isEnding) {
            EndingCard(onStartOver = onStartOver)
        } else {
            ChoiceList(
                choices = node.choices,
                isLoading = isChoiceInProgress,
                onChoiceSelected = onChoiceSelected,
                onCustomChoice = onCustomChoice,
            )
        }
    }
}

@Composable
private fun ParentChoiceBanner(parentChoice: ChoiceResponse?) {
    if (parentChoice == null) return

    val annotatedText = buildAnnotatedString {
        withStyle(SpanStyle(color = CosmoTheme.colors.mutedForeground)) {
            append("You chose: ")
        }
        withStyle(
            SpanStyle(
                color = CosmoTheme.colors.foreground,
                fontStyle = FontStyle.Italic,
            ),
        ) {
            append(parentChoice.label)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CosmoTheme.colors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = CosmoTheme.colors.primary,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.CenterVertically),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
