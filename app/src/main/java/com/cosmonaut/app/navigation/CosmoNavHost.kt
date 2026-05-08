package com.cosmonaut.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmonaut.app.ui.screens.audio.AudioNarrationViewModel
import com.cosmonaut.app.ui.screens.auth.LoginScreen
import com.cosmonaut.app.ui.screens.create.CreateScreen
import com.cosmonaut.app.ui.screens.home.HomeScreen
import com.cosmonaut.app.ui.screens.onboarding.OnboardingCarouselScreen
import com.cosmonaut.app.ui.screens.onboarding.OnboardingScreen
import com.cosmonaut.app.ui.screens.settings.FeedbackScreen
import com.cosmonaut.app.ui.screens.settings.SettingsScreen
import com.cosmonaut.app.ui.screens.story.StoryReaderScreen
import com.cosmonaut.app.ui.screens.storymap.StoryMapScreen
import com.cosmonaut.app.ui.screens.world.WorldHomeScreen

private const val TRANSITION_DURATION_MS = 300

private fun NavBackStackEntry.bottomNavIndex(): Int {
    BottomNavItem.entries.forEachIndexed { index, item ->
        if (destination.hasRoute(item.route::class)) return index
    }
    return -1
}

private enum class StoryNodeDirection { FORWARD, BACKWARD }

@Composable
fun CosmoNavHost(
    navController: NavHostController,
    startDestination: CosmoRoute,
    modifier: Modifier = Modifier,
    audioViewModel: AudioNarrationViewModel? = null,
) {
    var storyNodeDirection by remember { mutableStateOf(StoryNodeDirection.FORWARD) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
        // ── Auth Destinations ─────────────────────────────────────

        composable<CosmoRoute.OnboardingCarousel> {
            OnboardingCarouselScreen(
                onComplete = {
                    navController.navigate(CosmoRoute.Login) {
                        popUpTo<CosmoRoute.OnboardingCarousel> { inclusive = true }
                    }
                },
            )
        }

        composable<CosmoRoute.Login> {
            LoginScreen(
                onAuthSuccess = {
                    navController.navigate(CosmoRoute.Onboarding) {
                        popUpTo<CosmoRoute.Login> { inclusive = true }
                    }
                },
            )
        }

        composable<CosmoRoute.Onboarding> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(CosmoRoute.Home) {
                        popUpTo<CosmoRoute.Onboarding> { inclusive = true }
                    }
                },
            )
        }

        // ── Bottom Navigation Destinations ────────────────────────

        composable<CosmoRoute.Home>(
            enterTransition = { bottomNavEnter() },
            exitTransition = { bottomNavExit() },
            popEnterTransition = { bottomNavEnter() },
            popExitTransition = { bottomNavExit() },
        ) {
            HomeScreen(
                onNavigateToWorld = { worldId ->
                    navController.navigate(CosmoRoute.WorldHome(worldId))
                },
                onNavigateToStoryNode = { worldId, nodeId ->
                    navController.navigate(CosmoRoute.StoryNode(worldId, nodeId))
                },
            )
        }

        composable<CosmoRoute.Create>(
            enterTransition = { bottomNavEnter() },
            exitTransition = { bottomNavExit() },
            popEnterTransition = { bottomNavEnter() },
            popExitTransition = { bottomNavExit() },
        ) {
            CreateScreen(
                onNavigateToWorld = { worldId ->
                    navController.navigate(CosmoRoute.WorldHome(worldId)) {
                        popUpTo<CosmoRoute.Home>()
                    }
                },
            )
        }

        composable<CosmoRoute.Settings>(
            enterTransition = { bottomNavEnter() },
            exitTransition = { bottomNavExit() },
            popEnterTransition = { bottomNavEnter() },
            popExitTransition = { bottomNavExit() },
        ) {
            SettingsScreen(
                onNavigateToFeedback = {
                    navController.navigate(CosmoRoute.Feedback)
                },
            )
        }

        composable<CosmoRoute.Feedback> {
            FeedbackScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── World Destinations ────────────────────────────────────

        composable<CosmoRoute.WorldHome> {
            WorldHomeScreen(
                onNavigateToStoryNode = { worldId, nodeId ->
                    navController.navigate(CosmoRoute.StoryNode(worldId, nodeId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = { worldId ->
                    navController.navigate(CosmoRoute.StoryMap(worldId))
                },
            )
        }

        composable<CosmoRoute.StoryNode>(
            enterTransition = {
                val direction = if (storyNodeDirection == StoryNodeDirection.BACKWARD) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                fadeIn(tween(TRANSITION_DURATION_MS)) +
                    slideIntoContainer(direction, tween(TRANSITION_DURATION_MS))
            },
            exitTransition = {
                val direction = if (storyNodeDirection == StoryNodeDirection.BACKWARD) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                fadeOut(tween(TRANSITION_DURATION_MS)) +
                    slideOutOfContainer(direction, tween(TRANSITION_DURATION_MS))
            },
        ) {
            StoryReaderScreen(
                onNavigateToNode = { worldId, nodeId ->
                    storyNodeDirection = StoryNodeDirection.FORWARD
                    navController.navigate(CosmoRoute.StoryNode(worldId, nodeId))
                },
                onNavigateToParent = { worldId, nodeId ->
                    storyNodeDirection = StoryNodeDirection.BACKWARD
                    navController.navigate(CosmoRoute.StoryNode(worldId, nodeId)) {
                        popUpTo<CosmoRoute.StoryNode> { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    storyNodeDirection = StoryNodeDirection.FORWARD
                    navController.navigate(CosmoRoute.Home) {
                        popUpTo<CosmoRoute.Home> { inclusive = true }
                    }
                },
                onNavigateToMap = { worldId, currentNodeId ->
                    storyNodeDirection = StoryNodeDirection.FORWARD
                    navController.navigate(CosmoRoute.StoryMap(worldId, currentNodeId))
                },
                audioViewModel = audioViewModel,
            )
        }

        composable<CosmoRoute.StoryMap> {
            StoryMapScreen(
                onNavigateToNode = { worldId, nodeId ->
                    storyNodeDirection = StoryNodeDirection.FORWARD
                    navController.navigate(CosmoRoute.StoryNode(worldId, nodeId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bottomNavEnter(): EnterTransition {
    val fromIndex = initialState.bottomNavIndex()
    val toIndex = targetState.bottomNavIndex()
    if (fromIndex < 0 || toIndex < 0) return fadeIn(tween(TRANSITION_DURATION_MS))
    val direction = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideIntoContainer(direction, tween(TRANSITION_DURATION_MS)) +
        fadeIn(tween(TRANSITION_DURATION_MS))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bottomNavExit(): ExitTransition {
    val fromIndex = initialState.bottomNavIndex()
    val toIndex = targetState.bottomNavIndex()
    if (fromIndex < 0 || toIndex < 0) return fadeOut(tween(TRANSITION_DURATION_MS))
    val direction = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideOutOfContainer(direction, tween(TRANSITION_DURATION_MS)) +
        fadeOut(tween(TRANSITION_DURATION_MS))
}
