package com.cosmonaut.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber

@HiltAndroidApp
class CosmoApp :
    Application(),
    SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initializeSentry()
        initializePostHog()
        initializeAmplify()

        Timber.i("Cosmonaut app initialized — env: %s", BuildConfig.BUILD_TYPE)
    }

    private fun initializeSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = if (BuildConfig.DEBUG) "development" else BuildConfig.FLAVOR
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.isEnableAutoSessionTracking = true
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
            options.profilesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
            options.isAnrEnabled = true
            options.sessionReplay.sessionSampleRate = if (BuildConfig.DEBUG) 0.0 else 0.1
            options.sessionReplay.onErrorSampleRate = if (BuildConfig.DEBUG) 0.0 else 1.0
            options.isAttachAnrThreadDump = true
            options.setBeforeSend { event, _ ->
                if (BuildConfig.DEBUG) {
                    Timber.d("Sentry event: %s", event.eventId)
                }
                event
            }

            options.addIntegration(
                SentryTimberIntegration(minEventLevel = SentryLevel.ERROR, minBreadcrumbLevel = SentryLevel.INFO),
            )
        }
        Timber.i("Sentry initialized")
    }

    private fun initializePostHog() {
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            captureApplicationLifecycleEvents = true
            captureDeepLinks = true
            captureScreenViews = false
            debug = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(this, config)
        Timber.i("PostHog initialized")
    }

    private fun initializeAmplify() {
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(applicationContext)
            Timber.i("Amplify configured successfully")
        } catch (expected: AmplifyException) {
            Timber.e(expected, "Failed to configure Amplify")
        }
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader = ImageLoader.Builder(context)
        .crossfade(CROSSFADE_DURATION_MS)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, MEMORY_CACHE_PERCENT)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(DISK_CACHE_PERCENT)
                .build()
        }
        .build()

    companion object {
        private const val MEMORY_CACHE_PERCENT = 0.25
        private const val DISK_CACHE_PERCENT = 0.05
        private const val CROSSFADE_DURATION_MS = 300
    }
}
