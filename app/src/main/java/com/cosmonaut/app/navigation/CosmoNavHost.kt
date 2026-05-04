package com.cosmonaut.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmonaut.app.ui.screens.create.CreateScreen
import com.cosmonaut.app.ui.screens.home.HomeScreen
import com.cosmonaut.app.ui.screens.settings.SettingsScreen

private const val TRANSITION_DURATION_MS = 300

@Composable
fun CosmoNavHost(navController: NavHostController, modifier: Modifier = Modifier,) {
    NavHost(
        navController = navController,
        startDestination = CosmoRoute.Home,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(TRANSITION_DURATION_MS),
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(TRANSITION_DURATION_MS),
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(TRANSITION_DURATION_MS),
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(TRANSITION_DURATION_MS),
                )
        },
    ) {
        // Bottom Navigation destinations
        composable<CosmoRoute.Home> {
            HomeScreen()
        }

        composable<CosmoRoute.Create> {
            CreateScreen()
        }

        composable<CosmoRoute.Settings> {
            SettingsScreen()
        }
    }
}
