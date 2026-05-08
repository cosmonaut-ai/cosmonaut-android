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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.SubscriptionCta
import com.cosmonaut.app.ui.components.SubscriptionCtaAction
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Dialog shown when the user hits a quota limit (worlds, nodes, or audio).
 * Renders a region-aware CTA: clickable link for US users, plain text for non-US.
 */
@Composable
fun UpgradePromptDialog(
    resource: String,
    regionDetector: RegionDetector,
    usage: UsageResponse?,
    onDismiss: () -> Unit,
) {
    val title = when (resource) {
        "worlds" -> "Story Creation Limit Reached"
        "nodes" -> "Generation Limit Reached"
        "audio" -> "Audio Narration Limit Reached"
        else -> "Usage Limit Reached"
    }
    val message = when (resource) {
        "worlds" -> "You've reached your story creation limit for this period."
        "nodes" -> "You've used all your story nodes for this period."
        "audio" -> if (usage?.tier?.uppercase() == "FREE" || usage?.tier?.uppercase() == "EXPLORER") {
            "You've used all your free audio narrations."
        } else {
            "You've reached your audio narration limit for this period."
        }
        else -> "You've reached your usage limit."
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
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmoTheme.colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )

                if (usage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    UsageSnippet(resource = resource, usage = usage)
                }

                Spacer(modifier = Modifier.height(24.dp))

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
        "worlds" -> Triple(usage.worldsCreated, usage.worldsLimit, "stories created")
        "nodes" -> Triple(usage.nodesUsed, usage.nodesLimit, "generations used")
        "audio" -> Triple(usage.audioUsed, usage.audioLimit, "audio narrations used")
        else -> Triple(0, 0, "used")
    }

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
            if (resource == "audio" &&
                (usage.tier.uppercase() == "FREE" || usage.tier.uppercase() == "EXPLORER")
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "One-time allowance — does not reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }
    }
}
