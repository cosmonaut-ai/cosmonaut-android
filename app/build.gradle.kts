plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

android {
    namespace = "com.cosmonaut.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cosmonaut.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_GWLKBPNKF\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"7bsqjkt7g0notvqm4m5rvdl56g\"")
            buildConfigField("String", "COGNITO_DOMAIN", "\"cosmonaut-dev.auth.us-east-2.amazoncognito.com\"")
            buildConfigField("String", "COGNITO_REDIRECT_URI", "\"cosmonaut.dev://callback\"")
            buildConfigField("String", "AWS_REGION", "\"us-east-2\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["cognitoRedirectScheme"] = "cosmonaut"
            buildConfigField("String", "API_BASE_URL", "\"https://api.cosmonaut-ai.com\"")
            buildConfigField("String", "STREAMING_BASE_URL", "\"https://streaming.cosmonaut-ai.com\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_NE7ZsAjT9\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"127ioqo9dk9hc4n677t8lf9ft6\"")
            buildConfigField("String", "COGNITO_DOMAIN", "\"cosmonaut-prod.auth.us-east-2.amazoncognito.com\"")
            buildConfigField("String", "COGNITO_REDIRECT_URI", "\"cosmonaut://callback\"")
            buildConfigField("String", "AWS_REGION", "\"us-east-2\"")
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
                "proguard-rules.pro"
            )
        }
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

    // Browser (Custom Tabs for OAuth)
    implementation(libs.androidx.browser)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Logging
    implementation(libs.timber)
}
