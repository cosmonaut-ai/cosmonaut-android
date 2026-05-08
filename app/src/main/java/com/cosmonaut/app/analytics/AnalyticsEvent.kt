package com.cosmonaut.app.analytics

/**
 * Type-safe analytics event definitions matching the web app's event names exactly.
 * This ensures cross-platform analytics consistency in PostHog.
 */
sealed class AnalyticsEvent(
    val name: String,
    open val properties: Map<String, Any> = emptyMap(),
) {
    // ── Auth ─────────────────────────────────────────────────────────────

    data class Login(val method: String) : AnalyticsEvent(
        name = "login",
        properties = mapOf("method" to method),
    )

    data class SignUp(val method: String) : AnalyticsEvent(
        name = "sign_up",
        properties = mapOf("method" to method),
    )

    data class AuthFailed(val method: String, val action: String) : AnalyticsEvent(
        name = "auth_failed",
        properties = mapOf("method" to method, "action" to action),
    )

    data object EmailVerified : AnalyticsEvent(name = "email_verified")

    data object PasswordReset : AnalyticsEvent(name = "password_reset")

    data class OnboardingCompleted(val newsletterOptIn: Boolean) : AnalyticsEvent(
        name = "onboarding_completed",
        properties = mapOf("newsletter_opt_in" to newsletterOptIn),
    )

    // ── CTA & Subscription ───────────────────────────────────────────────

    data class CtaClicked(val location: String) : AnalyticsEvent(
        name = "cta_clicked",
        properties = mapOf("location" to location),
    )

    data class CheckoutInitiated(val tier: String) : AnalyticsEvent(
        name = "checkout_initiated",
        properties = mapOf("tier" to tier),
    )

    data object CheckoutCompleted : AnalyticsEvent(name = "checkout_completed")

    data object BillingPortalOpened : AnalyticsEvent(name = "billing_portal_opened")

    data class UpgradePromptShown(val resource: String) : AnalyticsEvent(
        name = "upgrade_prompt_shown",
        properties = mapOf("resource" to resource),
    )

    data class UpgradePromptClicked(val resource: String) : AnalyticsEvent(
        name = "upgrade_prompt_clicked",
        properties = mapOf("resource" to resource),
    )

    // ── Story ────────────────────────────────────────────────────────────

    data class StoryStarted(val worldId: String) : AnalyticsEvent(
        name = "story_started",
        properties = mapOf("world_id" to worldId),
    )

    data class StoryChoiceMade(
        val worldId: String,
        val choiceType: String,
    ) : AnalyticsEvent(
        name = "story_choice_made",
        properties = mapOf("world_id" to worldId, "choice_type" to choiceType),
    )

    data class StoryRestarted(val worldId: String) : AnalyticsEvent(
        name = "story_restarted",
        properties = mapOf("world_id" to worldId),
    )

    data class StoryEnded(val worldId: String, val pathLength: Int) : AnalyticsEvent(
        name = "story_ended",
        properties = mapOf("world_id" to worldId, "path_length" to pathLength),
    )

    // ── World ────────────────────────────────────────────────────────────

    data object RandomPromptUsed : AnalyticsEvent(name = "random_prompt_used")

    data class WorldShared(val visibility: String, val sharedCount: Int) : AnalyticsEvent(
        name = "world_shared",
        properties = mapOf("visibility" to visibility, "shared_count" to sharedCount),
    )

    data object ShareLinkCopied : AnalyticsEvent(name = "share_link_copied")

    data object WorldPromptCopied : AnalyticsEvent(name = "world_prompt_copied")

    data class FeaturedWorldClicked(val worldId: String) : AnalyticsEvent(
        name = "featured_world_clicked",
        properties = mapOf("world_id" to worldId),
    )

    // ── Audio ────────────────────────────────────────────────────────────

    data class NarrationStarted(val worldId: String, val nodeId: String) : AnalyticsEvent(
        name = "narration_started",
        properties = mapOf("world_id" to worldId, "node_id" to nodeId),
    )

    // ── Map ──────────────────────────────────────────────────────────────

    data class MapViewed(val worldId: String) : AnalyticsEvent(
        name = "map_viewed",
        properties = mapOf("world_id" to worldId),
    )

    // ── Demo (landing page) ──────────────────────────────────────────────

    data object DemoChoiceMade : AnalyticsEvent(name = "demo_choice_made")
    data object DemoRestarted : AnalyticsEvent(name = "demo_restarted")
}
