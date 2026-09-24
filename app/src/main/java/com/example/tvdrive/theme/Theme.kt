package com.example.tvdrive.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Modern Light color scheme for TV — clean, vibrant, high-contrast.
 */
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
