package com.example.tvdrive.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── TV Drive Dark Palette ─────────────────────────────────────────────────────
val TvBackground      = Color(0xFF0A0A0F)
val TvSurface         = Color(0xFF13131A)
val TvSurfaceCard     = Color(0xFF1C1C26)
val TvOutline         = Color(0xFF2A2A38)

// ── TV Drive Modern Light Theme Tokens ─────────────────────────────────────────
val TvLightBackground     = Color(0xFFF1F5F9)
val TvLightSurface        = Color(0xFFFFFFFF)
val TvLightSurfaceCard    = Color(0xFFFFFFFF)
val TvLightOutline        = Color(0xFFE2E8F0)
val TvLightOutlineFocused = Color(0xFF2563EB)
val TvLightTextPrimary    = Color(0xFF0F172A)
val TvLightTextSecondary  = Color(0xFF334155)
val TvLightTextMuted      = Color(0xFF64748B)
val TvLightCardFocusedBg  = Color(0xFFEFF6FF)

val GoogleBlue        = Color(0xFF4285F4)
val GoogleBlueLight   = Color(0xFF82B4FF)
val GoogleBlueDark    = Color(0xFF2A5DB0)
val FocusGlow         = Color(0xFF6EA8FF)
val GoogleGreen       = Color(0xFF34A853)
val GoogleGreenLight  = Color(0xFF81C784)
val GoogleYellow      = Color(0xFFFBBC04)
val GoogleRed         = Color(0xFFEA4335)

val TvOnBackground    = Color(0xFFE8E8F0)
val TvOnSurface       = Color(0xFFD0D0E0)
val TvOnSurfaceMuted  = Color(0xFF8080A0)

val GradientTop       = Color(0xFF06070B)
val GradientBottom    = Color(0xFF0E101A)

val GlassSurface          = Color(0x14FFFFFF)
val GlassSurfaceElevated  = Color(0x22FFFFFF)
val GlassCardBackground   = Color(0x10131B2A)
val GlassBorder           = Color(0x26FFFFFF)
val GlassBorderFocused    = Color(0xFF4285F4)
val GlassBorderGlow       = Color(0x664285F4)

val GlowBlue              = Color(0x204285F4)
val GlowPurple            = Color(0x1C7C4DFF)
val GlowCyan              = Color(0x1800E5FF)
val CodeBackground        = Color(0xFF0A0F1D)
val CodeBorder            = Color(0x404285F4)

// ── TV Typography ─────────────────────────────────────────────────────────────
val TvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

private val TvLightColorScheme = lightColorScheme(
    primary            = Color(0xFF2563EB),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary          = Color(0xFF059669),
    onSecondary        = Color.White,
    tertiary           = Color(0xFFD97706),
    onTertiary         = Color.White,
    background         = TvLightBackground,
    onBackground       = TvLightTextPrimary,
    surface            = TvLightSurface,
    onSurface          = TvLightTextPrimary,
    surfaceVariant     = TvLightSurfaceCard,
    onSurfaceVariant   = TvLightTextSecondary,
    outline            = TvLightOutline,
    error              = Color(0xFFDC2626),
    onError            = Color.White
)

private val TvDarkColorScheme = darkColorScheme(
    primary           = GoogleBlue,
    onPrimary         = TvOnBackground,
    primaryContainer  = GoogleBlueDark,
    onPrimaryContainer = GoogleBlueLight,
    secondary         = GoogleGreen,
    onSecondary       = TvOnBackground,
    tertiary          = GoogleYellow,
    onTertiary        = TvBackground,
    background        = TvBackground,
    onBackground      = TvOnBackground,
    surface           = TvSurface,
    onSurface         = TvOnSurface,
    surfaceVariant    = TvSurfaceCard,
    onSurfaceVariant  = TvOnSurfaceMuted,
    outline           = TvOutline,
    error             = GoogleRed,
    onError           = TvOnBackground
)

@Composable
fun TvDriveTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TvDarkColorScheme else TvLightColorScheme,
        typography  = TvTypography,
        content     = content
    )
}
