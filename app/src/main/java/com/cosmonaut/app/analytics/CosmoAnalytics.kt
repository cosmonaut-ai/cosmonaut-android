package com.cosmonaut.app.analytics

import com.posthog.PostHog
import io.sentry.Sentry
import io.sentry.protocol.User
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Unified analytics facade for Cosmonaut.
 * Dispatches events to PostHog (product analytics) and sets user context
 * on both PostHog and Sentry. Mirrors the web app's `analytics.ts` module.
 */
@Singleton
class CosmoAnalytics @Inject constructor() {

    fun trackEvent(event: AnalyticsEvent) {
        Timber.d("Analytics: %s %s", event.name, event.properties)
        PostHog.capture(
            event = event.name,
            properties = event.properties,
        )
    }

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        Timber.d("Analytics: screen_view → %s", screenName)
        PostHog.screen(
            screenTitle = screenName,
            properties = properties,
        )
    }

    fun identifyUser(
        distinctId: String,
        email: String? = null,
        username: String? = null,
    ) {
        Timber.d("Analytics: identify → %s", distinctId)

        val userProperties = buildMap {
            email?.let { put("email", it) }
            username?.let { put("username", it) }
            put("platform", "android")
        }

        PostHog.identify(
            distinctId = distinctId,
            userProperties = userProperties,
        )

        Sentry.setUser(
            User().apply {
                id = distinctId
                this.email = email
                this.username = username
            },
        )
    }

    fun resetUser() {
        Timber.d("Analytics: reset user")
        PostHog.reset()
        Sentry.setUser(null)
    }
}
