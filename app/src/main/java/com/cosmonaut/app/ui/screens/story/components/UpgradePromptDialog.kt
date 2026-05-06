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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Dialog shown when the user hits a quota limit (nodes or audio).
 * Offers to view pricing plans or dismiss.
 */
@Composable
fun UpgradePromptDialog(resource: String, onViewPlans: () -> Unit, onDismiss: () -> Unit,) {
    val title = when (resource) {
        "nodes" -> "Node Limit Reached"
        "audio" -> "Audio Limit Reached"
        else -> "Usage Limit Reached"
    }
    val message = when (resource) {
        "nodes" -> "You've used all your story nodes for this period. Upgrade your plan to continue exploring."
        "audio" -> "You've used all your audio narrations for this period. Upgrade to generate more."
        else -> "You've reached your usage limit. Upgrade to continue."
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

                Spacer(modifier = Modifier.height(24.dp))

                CosmoButton(
                    onClick = onViewPlans,
                    variant = CosmoButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("View Plans") }

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
