package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Story ending composable shown when a path has ended (completed node with no choices).
 * Displays a gradient card with ending message and "Start Over" button.
 */
@Composable
fun EndingCard(onStartOver: () -> Unit, modifier: Modifier = Modifier,) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CosmoTheme.colors.primary.copy(alpha = 0.1f),
                            CosmoTheme.colors.accent.copy(alpha = 0.2f),
                            CosmoTheme.colors.primary.copy(alpha = 0.05f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "This path has ended",
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmoTheme.colors.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your journey along this branch is complete.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = CosmoTheme.colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CosmoButton(
            onClick = onStartOver,
            variant = CosmoButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        ) {
            Text("Start Over")
        }
    }
}
