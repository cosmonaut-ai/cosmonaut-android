@file:Suppress("MatchingDeclarationName")

package com.cosmonaut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

enum class BadgeVariant {
    Default,
    Secondary,
    Destructive,
    Success,
    Warning,
}

private const val BADGE_BG_ALPHA = 0.35f

@Composable
fun CosmoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
    icon: ImageVector? = null,
) {
    val (bgColor, textColor) = when (variant) {
        BadgeVariant.Default -> CosmoTheme.colors.primary to CosmoTheme.colors.primary
        BadgeVariant.Secondary -> CosmoTheme.colors.secondary to CosmoTheme.colors.secondaryForeground
        BadgeVariant.Destructive -> CosmoTheme.colors.destructive to CosmoTheme.colors.destructive
        BadgeVariant.Success -> Color(0xFF22C55E) to Color(0xFF22C55E)
        BadgeVariant.Warning -> Color(0xFFF59E0B) to Color(0xFFF59E0B)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor.copy(alpha = BADGE_BG_ALPHA))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}
