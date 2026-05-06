package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Error card shown when story generation fails.
 * Offers Retry, Go Back, and Dashboard actions.
 */
@Composable
fun NodeFailedCard(
    canRetry: Boolean,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CosmoTheme.colors.card,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = CosmoTheme.colors.destructive,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Generation Failed",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Something went wrong while generating this part of the story.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (canRetry) {
                    CosmoButton(
                        onClick = onRetry,
                        variant = CosmoButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    ) { Text("Retry") }
                }

                CosmoButton(
                    onClick = onGoBack,
                    variant = CosmoButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                ) { Text("Go Back") }

                CosmoButton(
                    onClick = onDashboard,
                    variant = CosmoButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                ) { Text("Dashboard") }
            }
        }
    }
}
