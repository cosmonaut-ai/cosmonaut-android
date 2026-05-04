package com.cosmonaut.app.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
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
fun CreateScreen(modifier: Modifier = Modifier,) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = CosmoTheme.colors.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create a Story",
            style = MaterialTheme.typography.headlineMedium,
            color = CosmoTheme.colors.foreground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Describe your world and let AI\nbring it to life.",
            style = MaterialTheme.typography.bodyLarge,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
