package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Informational card shown when the node belongs to a different playthrough session.
 * Directs user to the map or to start a new playthrough.
 */
@Composable
fun WrongSessionCard(onViewMap: () -> Unit, onDashboard: () -> Unit, modifier: Modifier = Modifier,) {
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
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Different Playthrough",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This node belongs to a different playthrough session. " +
                    "Use the map to navigate to explored nodes, or start over.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            CosmoButton(
                onClick = onViewMap,
                variant = CosmoButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("View Map") }

            Spacer(modifier = Modifier.height(12.dp))

            CosmoButton(
                onClick = onDashboard,
                variant = CosmoButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Dashboard") }
        }
    }
}
