package com.cosmonaut.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cosmonaut brand color palette, mapped from the web app's CSS custom properties.
 *
 * Light theme uses a blue-purple primary. Dark theme uses a gold/amber primary.
 * oklch values have been converted to sRGB hex.
 */
object CosmoColors {

    // ── Light Theme ─────────────────────────────────────────────────

    val LightBackground = Color(0xFFFFFFFF)
    val LightForeground = Color(0xFF2D3D4F)
    val LightCard = Color(0xFFFFFFFF)
    val LightCardForeground = Color(0xFF2D3D4F)
    val LightPrimary = Color(0xFF4B6CB7)
    val LightPrimaryForeground = Color(0xFFFFFFFF)
    val LightPrimaryDepth = Color(0xFF2A4080)
    val LightSecondary = Color(0xFFEEEFF8)
    val LightSecondaryForeground = Color(0xFF4E5F8A)
    val LightSecondaryDepth = Color(0xFFC5C8D9)
    val LightMuted = Color(0xFFFFFFFF)
    val LightMutedForeground = Color(0xFF7E8CA0)
    val LightAccent = Color(0xFFEEF8F8)
    val LightAccentForeground = Color(0xFF2D3D4F)
    val LightDestructive = Color(0xFFE53E7A)
    val LightDestructiveForeground = Color(0xFFFFFFFF)
    val LightDestructiveDepth = Color(0xFF991E4F)
    val LightBorder = Color(0xFFE8E9F0)
    val LightInput = Color(0xFFE8E9F0)
    val LightRing = Color(0xFF4B6CB7)

    // ── Dark Theme ──────────────────────────────────────────────────

    val DarkBackground = Color(0xFF1A2332)
    val DarkForeground = Color(0xFFF0F2F5)
    val DarkCard = Color(0xFF24303F)
    val DarkCardForeground = Color(0xFFF0F2F5)
    val DarkPrimary = Color(0xFFF0D060) // Gold — the signature Cosmonaut color
    val DarkPrimaryForeground = Color(0xFF1A2332)
    val DarkPrimaryDepth = Color(0xFFB89830)
    val DarkSecondary = Color(0xFF374A5C)
    val DarkSecondaryForeground = Color(0xFFE0E4EC)
    val DarkSecondaryDepth = Color(0xFF2B3B4A)
    val DarkMuted = Color(0xFF2E3C4D)
    val DarkMutedForeground = Color(0xFFB0BAC8)
    val DarkAccent = Color(0xFF5C6B3A)
    val DarkAccentForeground = Color(0xFFE8E0C8)
    val DarkDestructive = Color(0xFFE53E7A)
    val DarkDestructiveForeground = Color(0xFFFFFFFF)
    val DarkDestructiveDepth = Color(0xFF7A1040)
    val DarkBorder = Color(0xFF3D4D5E)
    val DarkInput = Color(0xFF3D4D5E)
    val DarkRing = Color(0xFF7B9AD0)
}

/**
 * Holds the full set of semantic colors for the Cosmonaut design system.
 * Parallels the web CSS custom property structure.
 */
data class CosmoColorScheme(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val primaryDepth: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val secondaryDepth: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val destructiveDepth: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
)

val CosmoLightColorScheme = CosmoColorScheme(
    background = CosmoColors.LightBackground,
    foreground = CosmoColors.LightForeground,
    card = CosmoColors.LightCard,
    cardForeground = CosmoColors.LightCardForeground,
    primary = CosmoColors.LightPrimary,
    primaryForeground = CosmoColors.LightPrimaryForeground,
    primaryDepth = CosmoColors.LightPrimaryDepth,
    secondary = CosmoColors.LightSecondary,
    secondaryForeground = CosmoColors.LightSecondaryForeground,
    secondaryDepth = CosmoColors.LightSecondaryDepth,
    muted = CosmoColors.LightMuted,
    mutedForeground = CosmoColors.LightMutedForeground,
    accent = CosmoColors.LightAccent,
    accentForeground = CosmoColors.LightAccentForeground,
    destructive = CosmoColors.LightDestructive,
    destructiveForeground = CosmoColors.LightDestructiveForeground,
    destructiveDepth = CosmoColors.LightDestructiveDepth,
    border = CosmoColors.LightBorder,
    input = CosmoColors.LightInput,
    ring = CosmoColors.LightRing,
)

val CosmoDarkColorScheme = CosmoColorScheme(
    background = CosmoColors.DarkBackground,
    foreground = CosmoColors.DarkForeground,
    card = CosmoColors.DarkCard,
    cardForeground = CosmoColors.DarkCardForeground,
    primary = CosmoColors.DarkPrimary,
    primaryForeground = CosmoColors.DarkPrimaryForeground,
    primaryDepth = CosmoColors.DarkPrimaryDepth,
    secondary = CosmoColors.DarkSecondary,
    secondaryForeground = CosmoColors.DarkSecondaryForeground,
    secondaryDepth = CosmoColors.DarkSecondaryDepth,
    muted = CosmoColors.DarkMuted,
    mutedForeground = CosmoColors.DarkMutedForeground,
    accent = CosmoColors.DarkAccent,
    accentForeground = CosmoColors.DarkAccentForeground,
    destructive = CosmoColors.DarkDestructive,
    destructiveForeground = CosmoColors.DarkDestructiveForeground,
    destructiveDepth = CosmoColors.DarkDestructiveDepth,
    border = CosmoColors.DarkBorder,
    input = CosmoColors.DarkInput,
    ring = CosmoColors.DarkRing,
)
