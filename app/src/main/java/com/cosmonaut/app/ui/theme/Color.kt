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
    val LightOutline = Color(0xFFE2E4EA)
    val LightOutlineForeground = Color(0xFF2D3D4F)
    val LightOutlineDepth = Color(0xFFCED1D9)
    val LightBorder = Color(0xFFE8E9F0)
    val LightInput = Color(0xFFE8E9F0)
    val LightRing = Color(0xFF4B6CB7)

    // ── Dark Theme ──────────────────────────────────────────────────

    val DarkBackground = Color(0xFF121A21)
    val DarkForeground = Color(0xFFF3F5F8)
    val DarkCard = Color(0xFF1B2731)
    val DarkCardForeground = Color(0xFFF3F5F8)
    val DarkPrimary = Color(0xFFFFF1AE)
    val DarkPrimaryForeground = Color(0xFF121A21)
    val DarkPrimaryDepth = Color(0xFFD4B860)
    val DarkSecondary = Color(0xFF2C445A)
    val DarkSecondaryForeground = Color(0xFF7F92A2)
    val DarkSecondaryDepth = Color(0xFF193249)
    val DarkMuted = Color(0xFF22303C)
    val DarkMutedForeground = Color(0xFFACBBC8)
    val DarkAccent = Color(0xFF7A6F42)
    val DarkAccentForeground = Color(0xFFEBE6D6)
    val DarkDestructive = Color(0xFFA0478D)
    val DarkDestructiveForeground = Color(0xFFFFFFFF)
    val DarkDestructiveDepth = Color(0xFF840A68)
    val DarkOutline = Color(0xFF22303C)
    val DarkOutlineForeground = Color(0xFFFFFFFF)
    val DarkOutlineDepth = Color(0xFF324757)
    val DarkOutlineBorder = Color(0xFF2E4251)
    val DarkBorder = Color(0xFF344858)
    val DarkInput = Color(0xFF344858)
    val DarkRing = Color(0xFFFFF1AE)

    // ── Graph / Chart Colors ──────────────────────────────────────────

    val GraphStart = Color(0xFF22C55E) // green-500 — root/start node ring
    val GraphEnd = Color(0xFFF97316) // orange-500 — ending node ring
    val GraphCurrentLight = Color(0xFF6366F1) // chart-2 in light theme (indigo-500)
    val GraphCurrentDark = Color(0xFF818CF8) // chart-2 in dark theme (indigo-400)
    val GraphDotLight = Color(0xFFCBD5E1) // muted dot grid in light theme
    val GraphDotDark = Color(0xFF475569) // muted dot grid in dark theme
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
    val outline: Color,
    val outlineForeground: Color,
    val outlineDepth: Color,
    val outlineBorder: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
    val graphStart: Color,
    val graphEnd: Color,
    val graphCurrent: Color,
    val graphDot: Color,
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
    outline = CosmoColors.LightOutline,
    outlineForeground = CosmoColors.LightOutlineForeground,
    outlineDepth = CosmoColors.LightOutlineDepth,
    outlineBorder = CosmoColors.LightBorder,
    border = CosmoColors.LightBorder,
    input = CosmoColors.LightInput,
    ring = CosmoColors.LightRing,
    graphStart = CosmoColors.GraphStart,
    graphEnd = CosmoColors.GraphEnd,
    graphCurrent = CosmoColors.GraphCurrentLight,
    graphDot = CosmoColors.GraphDotLight,
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
    outline = CosmoColors.DarkOutline,
    outlineForeground = CosmoColors.DarkOutlineForeground,
    outlineDepth = CosmoColors.DarkOutlineDepth,
    outlineBorder = CosmoColors.DarkOutlineBorder,
    border = CosmoColors.DarkBorder,
    input = CosmoColors.DarkInput,
    ring = CosmoColors.DarkRing,
    graphStart = CosmoColors.GraphStart,
    graphEnd = CosmoColors.GraphEnd,
    graphCurrent = CosmoColors.GraphCurrentDark,
    graphDot = CosmoColors.GraphDotDark,
)
