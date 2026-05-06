package com.cosmonaut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val CARD_BG_ALPHA = 0.8f
private const val BORDER_ALPHA = 0.5f

@Composable
fun GlassCard(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, content: @Composable () -> Unit,) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(CosmoTheme.colors.card.copy(alpha = CARD_BG_ALPHA))
            .border(
                width = 1.dp,
                color = CosmoTheme.colors.border.copy(alpha = BORDER_ALPHA),
                shape = RoundedCornerShape(cornerRadius),
            ),
    ) {
        content()
    }
}
