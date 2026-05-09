# Cosmonaut ProGuard / R8 Rules

# ── kotlinx.serialization ────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cosmonaut.app.**$$serializer { *; }
-keepclassmembers class com.cosmonaut.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.cosmonaut.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Retrofit ─────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── OkHttp ───────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.** { *; }

# ── Coil ─────────────────────────────────────────────────────────────
-dontwarn coil3.**

# ── Sentry ───────────────────────────────────────────────────────────
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
-keepattributes LineNumberTable,SourceFile

# ── PostHog ──────────────────────────────────────────────────────────
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# ── AWS Amplify ──────────────────────────────────────────────────────
-keep class com.amplifyframework.** { *; }
-dontwarn com.amplifyframework.**
-keep class com.amazonaws.** { *; }
-dontwarn com.amazonaws.**

# ── Store5 ───────────────────────────────────────────────────────────
-keep class org.mobilenativefoundation.store.** { *; }
-dontwarn org.mobilenativefoundation.store.**

# ── Media3 / ExoPlayer ──────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Play Billing ─────────────────────────────────────────────────────
-keep class com.android.vending.billing.** { *; }
-dontwarn com.android.vending.billing.**

# ── Hilt / Dagger ────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories { *; }

# ── Navigation Compose ───────────────────────────────────────────────
-keep class * extends androidx.navigation.Navigator { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Timber ───────────────────────────────────────────────────────────
-dontwarn org.jetbrains.annotations.**

# ── General Android ──────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
