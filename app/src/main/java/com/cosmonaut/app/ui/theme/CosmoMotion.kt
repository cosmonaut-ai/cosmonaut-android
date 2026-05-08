package com.cosmonaut.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

@Immutable
data class CosmoMotionConfig(val isReducedMotion: Boolean = false,)

val LocalCosmoMotion = staticCompositionLocalOf { CosmoMotionConfig() }

object CosmoMotion {
    val config: CosmoMotionConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalCosmoMotion.current
}

@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}

object CosmoAnimationDefaults {
    const val TRANSITION_MS = 350
    const val STAGGER_DELAY_MS = 60L
    const val ENTRANCE_MS = 400
    const val MICRO_MS = 200
}

@Composable
fun <T> cosmoSpring(
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
): AnimationSpec<T> = if (CosmoMotion.config.isReducedMotion) {
    tween(durationMillis = 0)
} else {
    spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

@Composable
fun <T> cosmoTween(durationMillis: Int = CosmoAnimationDefaults.TRANSITION_MS): FiniteAnimationSpec<T> =
    if (CosmoMotion.config.isReducedMotion) {
        tween(durationMillis = 0)
    } else {
        tween(durationMillis = durationMillis)
    }

fun cosmoStaggerDelay(index: Int, isReducedMotion: Boolean): Long =
    if (isReducedMotion) 0L else index * CosmoAnimationDefaults.STAGGER_DELAY_MS
