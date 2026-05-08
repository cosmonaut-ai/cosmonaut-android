package com.cosmonaut.app.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import timber.log.Timber

/**
 * Maps navigation route class names to human-readable screen names for analytics.
 * Matches the web app's route → page title conventions.
 */
private fun routeToScreenName(route: String?): String? {
    if (route == null) return null
    return when {
        route.contains("WorldHome") -> "World Home"
        route.contains("StoryNode") -> "Story Reader"
        route.contains("StoryMap") -> "Story Map"
        route.contains("Home") -> "Dashboard"
        route.contains("Create") -> "Create World"
        route.contains("Settings") -> "Settings"
        route.contains("Feedback") -> "Feedback"
        route.contains("Login") -> "Login"
        route.contains("OnboardingCarousel") -> "Onboarding Carousel"
        route.contains("Onboarding") -> "Onboarding"
        else -> route.substringAfterLast('.').substringBefore('/')
    }
}

/**
 * Composable side-effect that observes navigation changes and reports
 * screen views to [CosmoAnalytics]. Place this once in the composition
 * tree near the NavHost.
 */
@Composable
fun TrackScreenViews(navController: NavController, analytics: CosmoAnalytics) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            val routeName = destination.route
            val screenName = routeToScreenName(routeName) ?: return@OnDestinationChangedListener

            val properties = buildMap<String, Any> {
                put("route", routeName ?: "unknown")
                arguments?.getString("worldId")?.let { put("world_id", it) }
                arguments?.getString("nodeId")?.let { put("node_id", it) }
            }

            Timber.d("Screen view: %s", screenName)
            analytics.trackScreenView(screenName, properties)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}
