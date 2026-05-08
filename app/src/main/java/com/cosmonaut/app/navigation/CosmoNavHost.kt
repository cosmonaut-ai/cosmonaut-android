package com.cosmonaut.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
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
import com.cosmonaut.app.ui.theme.CosmoMotion

private const val TRANSITION_DURATION_MS = 350
private const val FADE_DURATION_MS = 200

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

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
    val isReducedMotion = CosmoMotion.config.isReducedMotion
    val duration = if (isReducedMotion) 0 else TRANSITION_DURATION_MS
    val fadeDuration = if (isReducedMotion) 0 else FADE_DURATION_MS

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(fadeDuration)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(duration, easing = EmphasizedDecelerate),
                    initialOffset = { it / 4 },
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(fadeDuration)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(duration, easing = EmphasizedAccelerate),
                    targetOffset = { it / 4 },
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(fadeDuration)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(duration, easing = EmphasizedDecelerate),
                    initialOffset = { it / 4 },
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(fadeDuration)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(duration, easing = EmphasizedAccelerate),
                    targetOffset = { it / 4 },
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
                onNavigateToCreate = {
                    navController.navigate(CosmoRoute.Create) {
                        launchSingleTop = true
                    }
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
                fadeIn(tween(fadeDuration)) +
                    slideIntoContainer(
                        direction,
                        tween(duration, easing = EmphasizedDecelerate),
                        initialOffset = { it / 4 },
                    )
            },
            exitTransition = {
                val direction = if (storyNodeDirection == StoryNodeDirection.BACKWARD) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                fadeOut(tween(fadeDuration)) +
                    slideOutOfContainer(
                        direction,
                        tween(duration, easing = EmphasizedAccelerate),
                        targetOffset = { it / 4 },
                    )
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
    if (fromIndex < 0 || toIndex < 0) return fadeIn(tween(FADE_DURATION_MS))
    val direction = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideIntoContainer(
        direction,
        tween(TRANSITION_DURATION_MS, easing = EmphasizedDecelerate),
        initialOffset = { it / 5 },
    ) + fadeIn(tween(FADE_DURATION_MS))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bottomNavExit(): ExitTransition {
    val fromIndex = initialState.bottomNavIndex()
    val toIndex = targetState.bottomNavIndex()
    if (fromIndex < 0 || toIndex < 0) return fadeOut(tween(FADE_DURATION_MS))
    val direction = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideOutOfContainer(
        direction,
        tween(TRANSITION_DURATION_MS, easing = EmphasizedAccelerate),
        targetOffset = { it / 5 },
    ) + fadeOut(tween(FADE_DURATION_MS))
}
