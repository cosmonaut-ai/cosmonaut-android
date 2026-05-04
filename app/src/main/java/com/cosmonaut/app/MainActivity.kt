package com.cosmonaut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cosmonaut.app.navigation.BottomNavItem
import com.cosmonaut.app.navigation.CosmoNavHost
import com.cosmonaut.app.ui.components.CosmoTopAppBar
import com.cosmonaut.app.ui.theme.CosmoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CosmoTheme {
                CosmoAppContent()
            }
        }
    }
}

@Composable
private fun CosmoAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentNavItem = BottomNavItem.entries.find { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmoTheme.colors.background,
        topBar = {
            CosmoTopAppBar(
                title = currentNavItem?.label ?: "Cosmonaut",
            )
        },
        bottomBar = {
            CosmoBottomBar(
                currentNavItem = currentNavItem,
                onItemSelected = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        CosmoNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun CosmoBottomBar(
    currentNavItem: BottomNavItem?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = CosmoTheme.colors.card,
        contentColor = CosmoTheme.colors.cardForeground,
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = item == currentNavItem
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CosmoTheme.colors.primary,
                    selectedTextColor = CosmoTheme.colors.primary,
                    unselectedIconColor = CosmoTheme.colors.mutedForeground,
                    unselectedTextColor = CosmoTheme.colors.mutedForeground,
                    indicatorColor = CosmoTheme.colors.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
