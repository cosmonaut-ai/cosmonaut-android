package com.cosmonaut.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val M3DarkColorScheme = darkColorScheme(
    primary = CosmoColors.DarkPrimary,
    onPrimary = CosmoColors.DarkPrimaryForeground,
    secondary = CosmoColors.DarkSecondary,
    onSecondary = CosmoColors.DarkSecondaryForeground,
    tertiary = CosmoColors.DarkAccent,
    onTertiary = CosmoColors.DarkAccentForeground,
    background = CosmoColors.DarkBackground,
    onBackground = CosmoColors.DarkForeground,
    surface = CosmoColors.DarkCard,
    onSurface = CosmoColors.DarkCardForeground,
    surfaceVariant = CosmoColors.DarkMuted,
    onSurfaceVariant = CosmoColors.DarkMutedForeground,
    error = CosmoColors.DarkDestructive,
    onError = CosmoColors.DarkDestructiveForeground,
    outline = CosmoColors.DarkBorder,
    outlineVariant = CosmoColors.DarkInput,
)

private val M3LightColorScheme = lightColorScheme(
    primary = CosmoColors.LightPrimary,
    onPrimary = CosmoColors.LightPrimaryForeground,
    secondary = CosmoColors.LightSecondary,
    onSecondary = CosmoColors.LightSecondaryForeground,
    tertiary = CosmoColors.LightAccent,
    onTertiary = CosmoColors.LightAccentForeground,
    background = CosmoColors.LightBackground,
    onBackground = CosmoColors.LightForeground,
    surface = CosmoColors.LightCard,
    onSurface = CosmoColors.LightCardForeground,
    surfaceVariant = CosmoColors.LightMuted,
    onSurfaceVariant = CosmoColors.LightMutedForeground,
    error = CosmoColors.LightDestructive,
    onError = CosmoColors.LightDestructiveForeground,
    outline = CosmoColors.LightBorder,
    outlineVariant = CosmoColors.LightInput,
)

val LocalCosmoColors = staticCompositionLocalOf { CosmoDarkColorScheme }

@Composable
fun CosmoTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val m3ColorScheme = if (darkTheme) M3DarkColorScheme else M3LightColorScheme
    val cosmoColors = if (darkTheme) CosmoDarkColorScheme else CosmoLightColorScheme
    val reducedMotion = isReducedMotionEnabled()
    val motionConfig = remember(reducedMotion) { CosmoMotionConfig(isReducedMotion = reducedMotion) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalCosmoColors provides cosmoColors,
        LocalCosmoMotion provides motionConfig,
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = CosmoTypography,
            shapes = CosmoShapes,
            content = content,
        )
    }
}

object CosmoTheme {
    val colors: CosmoColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalCosmoColors.current
}
