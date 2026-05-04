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
import dagger.hilt.android.HiltAndroidApp
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

        initializeAmplify()

        Timber.i("Cosmonaut app initialized — env: %s", BuildConfig.BUILD_TYPE)
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
        .crossfade(true)
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
        private const val DISK_CACHE_PERCENT = 0.02
    }
}
