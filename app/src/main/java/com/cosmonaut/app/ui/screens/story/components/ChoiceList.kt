package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmonaut.app.data.remote.dto.ChoiceResponse
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme
import kotlinx.coroutines.delay

private const val STAGGER_DELAY_MS = 70L
private const val CUSTOM_CHOICE_MAX_LENGTH = 200

@Composable
fun ChoiceList(
    choices: List<ChoiceResponse>,
    isAtQuotaLimit: Boolean,
    isLoading: Boolean,
    onChoiceSelected: (targetId: String) -> Unit,
    onCustomChoice: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(color = CosmoTheme.colors.border)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "WHAT DO YOU DO?",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(4.dp))

        choices.forEachIndexed { index, choice ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * STAGGER_DELAY_MS)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it / 4 },
                ),
            ) {
                ChoiceCard(
                    choice = choice,
                    index = index,
                    isAtQuotaLimit = isAtQuotaLimit,
                    isLoading = isLoading,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        choice.target?.let { onChoiceSelected(it) }
                    },
                )
            }
        }

        if (!isAtQuotaLimit) {
            Spacer(modifier = Modifier.height(8.dp))
            CustomChoiceInput(
                isLoading = isLoading,
                onSubmit = { text ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCustomChoice(text)
                },
            )
        } else {
            QuotaLimitBanner()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceCard(
    choice: ChoiceResponse,
    index: Int,
    isAtQuotaLimit: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val isExplored = choice.isExplored == true
    val isPreGenerated = choice.isCreated == true && !isExplored
    val isCustom = choice.isCustom == true
    val isDisabled = isLoading || (isAtQuotaLimit && choice.isCreated != true)

    val borderColor = when {
        isExplored -> CosmoTheme.colors.muted
        else -> CosmoTheme.colors.border
    }
    val bgColor = when {
        isExplored -> CosmoTheme.colors.muted.copy(alpha = 0.3f)
        else -> CosmoTheme.colors.background.copy(alpha = 0.5f)
    }
    val contentAlpha = when {
        isDisabled -> 0.5f
        isExplored -> 0.7f
        else -> 1f
    }

    val choiceDesc = buildString {
        append("Choice ${index + 1}: ${choice.label}")
        if (isExplored) append(", already explored")
        if (isPreGenerated) append(", quick choice")
        if (isCustom) append(", custom choice")
        if (isDisabled && isAtQuotaLimit) append(", quota limit reached")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .semantics(mergeDescendants = true) {
                contentDescription = choiceDesc
                if (isExplored) stateDescription = "Already explored"
            }
            .clickable(enabled = !isDisabled) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceIndicator(
            index = index,
            isExplored = isExplored,
        )

        Spacer(modifier = Modifier.width(16.dp))

        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = choice.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isExplored) {
                    CosmoTheme.colors.mutedForeground
                } else {
                    CosmoTheme.colors.foreground
                },
            )

            if (isPreGenerated) {
                InlineBadge(
                    text = "Quick",
                    borderColor = CosmoTheme.colors.mutedForeground.copy(alpha = 0.3f),
                    bgColor = CosmoTheme.colors.muted,
                    textColor = CosmoTheme.colors.mutedForeground,
                )
            }

            if (isCustom) {
                InlineBadge(
                    text = "Custom",
                    borderColor = CosmoTheme.colors.primary.copy(alpha = 0.3f),
                    bgColor = CosmoTheme.colors.primary.copy(alpha = 0.1f),
                    textColor = CosmoTheme.colors.primary,
                )
            }

            if (isCustom && choice.creatorDisplayName != null) {
                Text(
                    text = "by @${choice.creatorDisplayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isExplored) {
                CosmoTheme.colors.mutedForeground.copy(alpha = 0.5f)
            } else {
                CosmoTheme.colors.mutedForeground
            },
        )
    }
}

@Composable
private fun ChoiceIndicator(index: Int, isExplored: Boolean) {
    val borderColor = if (isExplored) {
        CosmoTheme.colors.mutedForeground.copy(alpha = 0.3f)
    } else {
        CosmoTheme.colors.primary.copy(alpha = 0.3f)
    }
    val bgColor = if (isExplored) {
        CosmoTheme.colors.muted
    } else {
        CosmoTheme.colors.background
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, borderColor, CircleShape)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isExplored) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Already explored",
                modifier = Modifier.size(16.dp),
                tint = CosmoTheme.colors.mutedForeground,
            )
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = CosmoTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun InlineBadge(
    text: String,
    borderColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
    }
}

@Composable
private fun QuotaLimitBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                CosmoTheme.colors.primary.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp),
            )
            .background(CosmoTheme.colors.primary.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "You've reached your generation limit for this period.",
            style = MaterialTheme.typography.bodySmall,
            color = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CustomChoiceInput(isLoading: Boolean, onSubmit: (String) -> Unit,) {
    var text by remember { mutableStateOf("") }

    Column {
        HorizontalDivider(color = CosmoTheme.colors.border)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Or write your own action...",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = CosmoTheme.colors.mutedForeground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= CUSTOM_CHOICE_MAX_LENGTH) text = it },
            placeholder = { Text("Describe what you want to do...") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            maxLines = 3,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmoTheme.colors.primary,
                unfocusedBorderColor = CosmoTheme.colors.border,
                focusedContainerColor = CosmoTheme.colors.card,
                unfocusedContainerColor = CosmoTheme.colors.card,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onSubmit(text.trim())
                        text = ""
                    }
                },
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${text.length}/$CUSTOM_CHOICE_MAX_LENGTH",
                style = MaterialTheme.typography.labelSmall,
                color = CosmoTheme.colors.mutedForeground,
            )

            if (text.isNotBlank()) {
                CosmoButton(
                    text = "Take Action",
                    onClick = {
                        onSubmit(text.trim())
                        text = ""
                    },
                    variant = CosmoButtonVariant.Primary,
                    enabled = !isLoading,
                    modifier = Modifier.width(140.dp),
                )
            }
        }
    }
}
