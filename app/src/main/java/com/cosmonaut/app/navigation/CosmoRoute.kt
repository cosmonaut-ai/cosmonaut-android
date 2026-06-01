package com.cosmonaut.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for the Cosmonaut app.
 * Uses kotlinx.serialization with Navigation Compose 2.8+ for compile-time safety.
 */
sealed interface CosmoRoute {

    // ── Bottom Navigation Destinations ──────────────────────────────

    @Serializable
    data object Home : CosmoRoute

    @Serializable
    data object Create : CosmoRoute

    @Serializable
    data object Settings : CosmoRoute

    // ── Settings Sub-Destinations ───────────────────────────────────

    @Serializable
    data object Feedback : CosmoRoute

    // ── Auth ────────────────────────────────────────────────────────

    @Serializable
    data object OnboardingCarousel : CosmoRoute

    @Serializable
    data object Login : CosmoRoute

    @Serializable
    data object Onboarding : CosmoRoute

    // ── World (will be implemented in Stage 3+) ─────────────────────

    @Serializable
    data class WorldHome(val worldId: String, val invite: String? = null) : CosmoRoute

    @Serializable
    data class SessionHome(val sessionId: String) : CosmoRoute

    @Serializable
    data class StoryNode(val sessionId: String, val nodeId: String) : CosmoRoute

    @Serializable
    data class StoryMap(val sessionId: String, val currentNodeId: String? = null) : CosmoRoute
}
