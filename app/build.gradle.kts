plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry.android.gradle)
}

val versionMajor = 1
val versionMinor = 0
val versionPatch = 0
val computedVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: (versionMajor * 10000 + versionMinor * 100 + versionPatch)

android {
    namespace = "com.cosmonaut.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cosmonaut.app"
        minSdk = 26
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE") ?: "release-keystore.jks"
            val keyStoreFile = listOf(
                file(storeFilePath),
                rootProject.file(storeFilePath),
            ).firstOrNull { it.exists() }
            if (keyStoreFile != null) {
                storeFile = keyStoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["cognitoRedirectScheme"] = "cosmonaut.dev"
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.cosmonaut-ai.com\"")
            buildConfigField("String", "STREAMING_BASE_URL", "\"https://streaming.dev.cosmonaut-ai.com\"")
            buildConfigField("String", "WEB_BASE_URL", "\"https://dev.cosmonaut-ai.com\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_GWLKBPNKF\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"7bsqjkt7g0notvqm4m5rvdl56g\"")
            buildConfigField("String", "COGNITO_DOMAIN", "\"cosmonaut-dev.auth.us-east-2.amazoncognito.com\"")
            buildConfigField("String", "COGNITO_REDIRECT_URI", "\"cosmonaut.dev://callback\"")
            buildConfigField("String", "AWS_REGION", "\"us-east-2\"")
            buildConfigField("String", "SENTRY_DSN", "\"https://a737601da6d420d0745431649af5b18d@o4511032796905472.ingest.us.sentry.io/4511032822792192\"")
            buildConfigField("String", "POSTHOG_API_KEY", "\"phc_tZwyBfSZQFAsGkPyRVstpwe4rRU2rmvHhhD4XDxfiQyS\"")
            buildConfigField("String", "POSTHOG_HOST", "\"https://i.cosmonaut-ai.com\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["cognitoRedirectScheme"] = "cosmonaut"
            buildConfigField("String", "API_BASE_URL", "\"https://api.cosmonaut-ai.com\"")
            buildConfigField("String", "STREAMING_BASE_URL", "\"https://streaming.cosmonaut-ai.com\"")
            buildConfigField("String", "WEB_BASE_URL", "\"https://cosmonaut-ai.com\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_NE7ZsAjT9\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"127ioqo9dk9hc4n677t8lf9ft6\"")
            buildConfigField("String", "COGNITO_DOMAIN", "\"cosmonaut-prod.auth.us-east-2.amazoncognito.com\"")
            buildConfigField("String", "COGNITO_REDIRECT_URI", "\"cosmonaut://callback\"")
            buildConfigField("String", "AWS_REGION", "\"us-east-2\"")
            buildConfigField("String", "SENTRY_DSN", "\"https://a737601da6d420d0745431649af5b18d@o4511032796905472.ingest.us.sentry.io/4511032822792192\"")
            buildConfigField("String", "POSTHOG_API_KEY", "\"phc_tZwyBfSZQFAsGkPyRVstpwe4rRU2rmvHhhD4XDxfiQyS\"")
            buildConfigField("String", "POSTHOG_HOST", "\"https://i.cosmonaut-ai.com\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile?.exists() == true) {
                signingConfig = releaseConfig
            }
        }
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose BOM
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // AWS Amplify
    implementation(libs.amplify.core)
    implementation(libs.amplify.core.kotlin)
    implementation(libs.amplify.auth.cognito)

    // Store5 (data caching — TanStack Query equivalent)
    implementation(libs.store5)

    // Browser (Custom Tabs for OAuth)
    implementation(libs.androidx.browser)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Media3 (Audio playback)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui.compose)

    // Play Billing (region detection only — no in-app purchase UI)
    implementation(libs.play.billing)

    // Collections
    implementation(libs.kotlinx.collections.immutable)

    // Logging
    implementation(libs.timber)

    // Performance
    debugImplementation(libs.leakcanary)
    implementation(libs.profileinstaller)

    // Sentry (crash reporting, performance monitoring, session replay)
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.android)
    implementation(libs.sentry.compose.android)
    implementation(libs.sentry.okhttp)
    implementation(libs.sentry.android.timber)
    implementation(libs.sentry.android.navigation)
    implementation(libs.sentry.kotlin.extensions)

    // PostHog (product analytics)
    implementation(libs.posthog.android)
}

sentry {
    org = "cosmonaut"
    projectName = "cosmonaut-android"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")

    tracingInstrumentation {
        enabled = true
        features = setOf(
            io.sentry.android.gradle.extensions.InstrumentationFeature.FILE_IO,
            io.sentry.android.gradle.extensions.InstrumentationFeature.OKHTTP,
            io.sentry.android.gradle.extensions.InstrumentationFeature.COMPOSE,
        )
    }

    autoUploadProguardMapping =
        System.getenv("SENTRY_AUTH_TOKEN") != null
    includeSourceContext =
        System.getenv("SENTRY_AUTH_TOKEN") != null
}
