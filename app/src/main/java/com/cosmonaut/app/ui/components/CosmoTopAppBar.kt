package com.cosmonaut.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.ui.theme.OrbitronFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmoTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CosmoTheme.colors.background,
            titleContentColor = CosmoTheme.colors.foreground,
            navigationIconContentColor = CosmoTheme.colors.foreground,
            actionIconContentColor = CosmoTheme.colors.foreground,
        ),
    )
}
