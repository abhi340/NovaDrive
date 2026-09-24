package com.example.tvdrive.theme

import androidx.compose.ui.graphics.Color

// ── TV Drive Dark Palette ─────────────────────────────────────────────────────
// Always dark — TV screens look best on a dark background, especially OLED panels

/** Background — near black, easy on the eyes at 10 ft */
val TvBackground      = Color(0xFF0A0A0F)
/** Surface — slightly elevated from background */
val TvSurface         = Color(0xFF13131A)
/** Card surfaces inside list/grid items */
val TvSurfaceCard     = Color(0xFF1C1C26)
/** Subtle dividers, borders */
val TvOutline         = Color(0xFF2A2A38)

// ── TV Drive Modern Light Theme Tokens ─────────────────────────────────────────
val TvLightBackground     = Color(0xFFF1F5F9) // Soft slate background (easy on TV eyes)
val TvLightSurface        = Color(0xFFFFFFFF) // Pure white card surface
val TvLightSurfaceCard    = Color(0xFFFFFFFF)
val TvLightOutline        = Color(0xFFE2E8F0) // Clean slate border
val TvLightOutlineFocused = Color(0xFF2563EB) // Vibrant high-contrast TV focus border
val TvLightTextPrimary    = Color(0xFF0F172A) // Deep high-contrast text
val TvLightTextSecondary  = Color(0xFF334155) // Medium contrast text
val TvLightTextMuted      = Color(0xFF64748B) // Subtitle & hint text
val TvLightCardFocusedBg  = Color(0xFFEFF6FF) // Soft blue highlight on focus

/** Google Blue — primary brand accent */
val GoogleBlue        = Color(0xFF4285F4)
val GoogleBlueLight   = Color(0xFF82B4FF)
val GoogleBlueDark    = Color(0xFF2A5DB0)

/** Focused item glow (slightly brighter than GoogleBlue) */
val FocusGlow         = Color(0xFF6EA8FF)

/** Google Green — success / download */
val GoogleGreen       = Color(0xFF34A853)
val GoogleGreenLight  = Color(0xFF81C784)

/** Google Yellow — warning / star */
val GoogleYellow      = Color(0xFFFBBC04)

/** Google Red — error / delete */
val GoogleRed         = Color(0xFFEA4335)

// ── Text ─────────────────────────────────────────────────────────────────────
val TvOnBackground    = Color(0xFFE8E8F0)
val TvOnSurface       = Color(0xFFD0D0E0)
val TvOnSurfaceMuted  = Color(0xFF8080A0)

// ── Gradient stops for hero backgrounds ───────────────────────────────────────
val GradientTop       = Color(0xFF06070B)
val GradientBottom    = Color(0xFF0E101A)

// ── Glassmorphism Design Tokens ───────────────────────────────────────────────
/** Frosted translucent surface */
val GlassSurface          = Color(0x14FFFFFF) // 8% white frost
/** Elevated frosted translucent surface */
val GlassSurfaceElevated  = Color(0x22FFFFFF) // 13% white frost
/** Card interior backdrop */
val GlassCardBackground   = Color(0x10131B2A)
/** Crisp 1px luminous glass border */
val GlassBorder           = Color(0x26FFFFFF) // 15% white
/** Focused glass border with vibrant blue neon */
val GlassBorderFocused    = Color(0xFF4285F4)
/** Soft ambient focus glow halo */
val GlassBorderGlow       = Color(0x664285F4)

// ── Ambient Glow Spheres (OLED Background Accents) ───────────────────────────
val GlowBlue              = Color(0x204285F4)
val GlowPurple            = Color(0x1C7C4DFF)
val GlowCyan              = Color(0x1800E5FF)

/** Code verification display pill */
val CodeBackground        = Color(0xFF0A0F1D)
val CodeBorder            = Color(0x404285F4)

