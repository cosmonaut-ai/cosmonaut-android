package com.cosmonaut.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier,) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = CosmoTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = CosmoTheme.colors.foreground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Account, preferences, and more.\nComing in Stage 9.",
            style = MaterialTheme.typography.bodyLarge,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
