package com.cosmonaut.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: CosmoRoute,
) {
    HOME(
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = CosmoRoute.Home,
    ),
    CREATE(
        label = "Create",
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add,
        route = CosmoRoute.Create,
    ),
    SETTINGS(
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        route = CosmoRoute.Settings,
    ),
}
