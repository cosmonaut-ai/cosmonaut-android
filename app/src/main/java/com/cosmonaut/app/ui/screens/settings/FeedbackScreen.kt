package com.cosmonaut.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MapsUgc
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoTopAppBar
import com.cosmonaut.app.ui.theme.CosmoTheme

private val categoryIcons: Map<FeedbackCategory, ImageVector> = mapOf(
    FeedbackCategory.BUG to Icons.Outlined.BugReport,
    FeedbackCategory.FEATURE to Icons.Outlined.Lightbulb,
    FeedbackCategory.FEEDBACK to Icons.AutoMirrored.Outlined.Message,
    FeedbackCategory.OTHER to Icons.AutoMirrored.Outlined.HelpOutline,
)

@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val category by viewModel.category.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isSubmitted by viewModel.isSubmitted.collectAsState()
    val isRateLimited by viewModel.isRateLimited.collectAsState()
    val messageTooShort = message.trim().length < FeedbackViewModel.MIN_MESSAGE_LENGTH
    val canSubmit = !messageTooShort && !isSubmitting

    Column(modifier = modifier.fillMaxSize()) {
        CosmoTopAppBar(
            title = "Feedback",
            showBackButton = true,
            onBackClick = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "We genuinely read all of this. Say whatever\u2019s on your mind.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CosmoTheme.colors.card,
                border = BorderStroke(1.dp, CosmoTheme.colors.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MapsUgc,
                            contentDescription = null,
                            tint = CosmoTheme.colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmoTheme.colors.foreground,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "Found a bug? Have an idea? Just want to tell us something feels off? All of it is useful.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.mutedForeground,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSubmitted) {
                        SuccessMessage()
                    } else {
                        FeedbackForm(
                            category = category,
                            message = message,
                            isSubmitting = isSubmitting,
                            isRateLimited = isRateLimited,
                            messageTooShort = messageTooShort,
                            canSubmit = canSubmit,
                            onCategoryChange = viewModel::setCategory,
                            onMessageChange = viewModel::setMessage,
                            onSubmit = viewModel::submit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessMessage() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CosmoTheme.colors.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, CosmoTheme.colors.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Got it \u2014 thanks for taking the time.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FeedbackForm(
    category: FeedbackCategory,
    message: String,
    isSubmitting: Boolean,
    isRateLimited: Boolean,
    messageTooShort: Boolean,
    canSubmit: Boolean,
    onCategoryChange: (FeedbackCategory) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Category selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )
            CategoryDropdown(
                selected = category,
                onSelect = onCategoryChange,
                enabled = !isSubmitting,
            )
        }

        // Message input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )

            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = CosmoTheme.colors.foreground,
                ),
                cursorBrush = SolidColor(CosmoTheme.colors.primary),
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmoTheme.colors.background)
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (message.isEmpty()) {
                            Text(
                                text = "What\u2019s on your mind?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (messageTooShort && message.isNotEmpty()) {
                        "A little more detail would help (at least ${FeedbackViewModel.MIN_MESSAGE_LENGTH} characters)"
                    } else {
                        "Tell us as much or as little as you\u2019d like"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val overLimit = message.length > FeedbackViewModel.MAX_MESSAGE_LENGTH
                Text(
                    text = "${message.length}/${FeedbackViewModel.MAX_MESSAGE_LENGTH}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overLimit) CosmoTheme.colors.destructive else CosmoTheme.colors.mutedForeground,
                    fontWeight = if (overLimit) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }

        // Rate limit warning
        AnimatedVisibility(
            visible = isRateLimited,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = amberColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, amberColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Whoa there cowboy! We can only read so much feedback at a time \u2014 take a breather!",
                    style = MaterialTheme.typography.bodySmall,
                    color = amberColor,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Submit button
        CosmoButton(
            onClick = onSubmit,
            enabled = canSubmit,
            isLoading = isSubmitting,
        ) {
            if (isSubmitting) {
                Text("Submitting...")
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Feedback")
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    selected: FeedbackCategory,
    onSelect: (FeedbackCategory) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CosmoTheme.colors.background,
            border = BorderStroke(1.dp, CosmoTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = categoryIcons[selected] ?: Icons.AutoMirrored.Outlined.Message,
                    contentDescription = null,
                    tint = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmoTheme.colors.foreground,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CosmoTheme.colors.card),
        ) {
            FeedbackCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categoryIcons[cat] ?: Icons.AutoMirrored.Outlined.Message,
                                contentDescription = null,
                                tint = CosmoTheme.colors.mutedForeground,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.label,
                                color = CosmoTheme.colors.foreground,
                            )
                        }
                    },
                    onClick = {
                        onSelect(cat)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val amberColor = androidx.compose.ui.graphics.Color(0xFFF59E0B)
