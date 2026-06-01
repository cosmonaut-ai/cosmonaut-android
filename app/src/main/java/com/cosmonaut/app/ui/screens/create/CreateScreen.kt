package com.cosmonaut.app.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.CosmoHaptics
import com.cosmonaut.app.ui.components.CosmoIconButton
import com.cosmonaut.app.ui.components.CosmoSegmentedControl
import com.cosmonaut.app.ui.components.GlassCard
import com.cosmonaut.app.ui.components.SegmentOption
import com.cosmonaut.app.ui.components.VisibilitySelector
import com.cosmonaut.app.ui.screens.story.components.UpgradePromptDialog
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val MAX_PROMPT_LENGTH = 2000
private const val PROMPT_MIN_LINES = 4

@Composable
fun CreateScreen(
    onNavigateToSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    var showQuotaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateEvent.NavigateToSession -> {
                    CosmoHaptics.onConfirm(view)
                    onNavigateToSession(event.sessionId)
                }
                is CreateEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short,
                )
                is CreateEvent.ShowQuotaDialog -> {
                    showQuotaDialog = true
                }
            }
        }
    }

    if (showQuotaDialog) {
        UpgradePromptDialog(
            resource = "worlds",
            regionDetector = viewModel.regionDetector,
            usage = state.usage,
            onDismiss = { showQuotaDialog = false },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Create a New Story",
                style = MaterialTheme.typography.titleLarge,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )

            GlassCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PromptSection(
                        prompt = state.prompt,
                        promptError = state.promptError,
                        onPromptChange = viewModel::updatePrompt,
                        onRandomPrompt = viewModel::loadRandomPrompt,
                    )

                    StorySettingsSection(
                        isExpanded = state.showMoreSettings,
                        visibility = state.visibility,
                        storyLength = state.storyLength,
                        vocabLevel = state.vocabLevel,
                        contentFilter = state.contentFilter,
                        onToggle = viewModel::toggleMoreSettings,
                        onVisibilityChange = viewModel::updateVisibility,
                        onStoryLengthChange = viewModel::updateStoryLength,
                        onVocabLevelChange = viewModel::updateVocabLevel,
                        onContentFilterChange = viewModel::updateContentFilter,
                    )

                    CosmoButton(
                        text = "Create Story",
                        onClick = viewModel::createWorld,
                        isLoading = state.isSubmitting,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
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
private fun PromptSection(
    prompt: String,
    promptError: String?,
    onPromptChange: (String) -> Unit,
    onRandomPrompt: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Story Prompt",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )
            CosmoIconButton(
                icon = Icons.Outlined.Casino,
                contentDescription = "Random prompt",
                onClick = onRandomPrompt,
                variant = CosmoButtonVariant.Primary,
                size = 36.dp,
                iconSize = 18.dp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Describe the world for your story...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = CosmoTheme.colors.mutedForeground,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = CosmoTheme.colors.foreground,
            ),
            minLines = PROMPT_MIN_LINES,
            maxLines = 8,
            isError = promptError != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = promptError ?: "",
                        color = if (promptError != null) {
                            CosmoTheme.colors.destructive
                        } else {
                            CosmoTheme.colors.mutedForeground
                        },
                    )
                    Text(
                        text = "${prompt.length}/$MAX_PROMPT_LENGTH",
                        color = CosmoTheme.colors.mutedForeground,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmoTheme.colors.outline,
                unfocusedContainerColor = CosmoTheme.colors.outline,
                focusedBorderColor = CosmoTheme.colors.primary,
                unfocusedBorderColor = CosmoTheme.colors.outline,
                cursorColor = CosmoTheme.colors.primary,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

private val storyLengthOptions = listOf(
    SegmentOption("short", "Short", "~5 choices"),
    SegmentOption("medium", "Medium", "~10 choices"),
    SegmentOption("long", "Long", "~20 choices"),
)

private val vocabLevelOptions = listOf(
    SegmentOption("child", "Child", "Ages 6-12"),
    SegmentOption("teen", "Teen", "Ages 13-17"),
    SegmentOption("adult", "Adult", "Ages 18+"),
)

private val contentFilterOptions = listOf(
    SegmentOption("none", "None", null),
    SegmentOption("moderate", "Moderate", null),
    SegmentOption("strict", "Strict", null),
)

@Suppress("LongParameterList")
@Composable
private fun StorySettingsSection(
    isExpanded: Boolean,
    visibility: String,
    storyLength: String,
    vocabLevel: String,
    contentFilter: String,
    onToggle: () -> Unit,
    onVisibilityChange: (String) -> Unit,
    onStoryLengthChange: (String) -> Unit,
    onVocabLevelChange: (String) -> Unit,
    onContentFilterChange: (String) -> Unit,
) {
    Column {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Story Settings",
                    color = CosmoTheme.colors.mutedForeground,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Filled.ExpandLess
                    } else {
                        Icons.Filled.ExpandMore
                    },
                    contentDescription = null,
                    tint = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                VisibilitySelector(
                    selectedVisibility = visibility,
                    onVisibilityChange = onVisibilityChange,
                )

                CosmoSegmentedControl(
                    options = storyLengthOptions,
                    selectedValue = storyLength,
                    onValueChange = onStoryLengthChange,
                    label = "Story Length",
                )

                CosmoSegmentedControl(
                    options = vocabLevelOptions,
                    selectedValue = vocabLevel,
                    onValueChange = onVocabLevelChange,
                    label = "Vocabulary Level",
                )

                CosmoSegmentedControl(
                    options = contentFilterOptions,
                    selectedValue = contentFilter,
                    onValueChange = onContentFilterChange,
                    label = "Content Filter",
                )
            }
        }
    }
}
