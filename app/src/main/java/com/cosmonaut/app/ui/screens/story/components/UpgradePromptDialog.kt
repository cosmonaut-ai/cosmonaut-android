package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.SubscriptionCta
import com.cosmonaut.app.ui.components.SubscriptionCtaAction
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.util.formatDate

/**
 * Dialog shown when the user hits a quota limit (worlds, nodes, or audio).
 * Renders a region-aware CTA: clickable link for US users, plain text for non-US.
 *
 * Matches the web UpgradePrompt dialog with:
 * - Title and description
 * - Usage snippet with counts and reset date
 * - Footer message about upgrading or waiting
 * - Manage Subscription CTA for paid users
 * - View Plans / Upgrade CTA (region-aware)
 * - Dismiss button
 */
@Composable
fun UpgradePromptDialog(
    resource: String,
    regionDetector: RegionDetector,
    usage: UsageResponse?,
    analytics: CosmoAnalytics? = null,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        analytics?.trackEvent(AnalyticsEvent.UpgradePromptShown(resource = resource))
    }
    val isFreeOrExplorer = usage?.tier?.uppercase().let { it == "FREE" || it == "EXPLORER" }
    val isFree = usage?.tier?.uppercase() == "FREE"
    val isPaid = usage?.tier?.uppercase().let { it == "EXPLORER" || it == "COSMONAUT" }

    val title = when (resource) {
        "worlds" -> "Story Creation Limit Reached"
        "nodes" -> "Generation Limit Reached"
        "audio" -> "Audio Narration Limit Reached"
        else -> "Usage Limit Reached"
    }

    val description = when (resource) {
        "worlds" -> "You\u2019ve reached your story creation limit for this period."
        "nodes" -> "You\u2019ve reached your story generation limit for this period."
        "audio" -> if (isFreeOrExplorer) {
            "You\u2019ve used all your free audio narrations. Upgrade to generate more."
        } else {
            "You\u2019ve reached your audio narration limit for this period."
        }
        else -> "You\u2019ve reached your usage limit."
    }

    val footerMessage = when {
        resource == "audio" && isFreeOrExplorer ->
            "Upgrade your plan to unlock more audio narrations."
        else ->
            "Upgrade your plan for higher limits, or wait for your usage period to reset."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CosmoTheme.colors.card,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmoTheme.colors.foreground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmoTheme.colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )

                if (usage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    UsageSnippet(resource = resource, usage = usage)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = footerMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isPaid) {
                    SubscriptionCta(
                        action = SubscriptionCtaAction.MANAGE,
                        regionDetector = regionDetector,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                SubscriptionCta(
                    action = SubscriptionCtaAction.UPGRADE,
                    regionDetector = regionDetector,
                )

                Spacer(modifier = Modifier.height(12.dp))

                CosmoButton(
                    onClick = onDismiss,
                    variant = CosmoButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Maybe Later") }
            }
        }
    }
}

@Composable
private fun UsageSnippet(resource: String, usage: UsageResponse) {
    val (used, limit, label) = when (resource) {
        "worlds" -> Triple(usage.worldsCreated, usage.worldsLimit, "stories created this period")
        "nodes" -> Triple(usage.nodesUsed, usage.nodesLimit, "generations used")
        "audio" -> Triple(usage.audioUsed, usage.audioLimit, "audio narrations used")
        else -> Triple(0, 0, "used")
    }

    val isFreeOrExplorer = usage.tier.uppercase().let { it == "FREE" || it == "EXPLORER" }
    val isAudioLifetime = resource == "audio" && isFreeOrExplorer

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CosmoTheme.colors.muted.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$used of $limit $label",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )

            if (isAudioLifetime) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "One-time allowance \u2014 does not reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            } else if (usage.periodEnd != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Resets on ${formatDate(usage.periodEnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }
    }
}
