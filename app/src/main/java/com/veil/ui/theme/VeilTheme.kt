package com.veil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.veil.ui.appearance.AppTheme

object VeilColors {
    val Background     = Color(0xFF0A0A0B)
    val Surface        = Color(0xFF111113)
    val SurfaceVariant = Color(0xFF1A1A1D)
    val Accent         = Color(0xFF4ECDC4)
    val AccentDim      = Color(0xFF2A8F88)
    val AccentSubtle   = Color(0xFF1A3A38)
    val TextPrimary    = Color(0xFFF0F0F2)
    val TextSecondary  = Color(0xFF8A8A90)
    val TextDisabled   = Color(0xFF3A3A40)
    val Border         = Color(0xFF222226)
    val BorderStrong   = Color(0xFF333338)
    val Success        = Color(0xFF4CAF7D)
    val Error          = Color(0xFFE05C6A)
    val Warning        = Color(0xFFE8A838)
}

// ── Per-theme background colors ────────────────────────────────────────

private object DarkColors {
    val Background     = Color(0xFF0A0A0B)
    val Surface        = Color(0xFF111113)
    val SurfaceVariant = Color(0xFF1A1A1D)
    val TextPrimary    = Color(0xFFF0F0F2)
    val TextSecondary  = Color(0xFF8A8A90)
    val Border         = Color(0xFF222226)
    val BorderStrong   = Color(0xFF333338)
}

private object LightColors {
    val Background     = Color(0xFFF5F5F7)
    val Surface        = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEEEF0)
    val TextPrimary    = Color(0xFF0A0A0B)
    val TextSecondary  = Color(0xFF6A6A70)
    val Border         = Color(0xFFDDDDDF)
    val BorderStrong   = Color(0xFFCCCCCE)
}

private object AmoledColors {
    val Background     = Color(0xFF000000)
    val Surface        = Color(0xFF0A0A0A)
    val SurfaceVariant = Color(0xFF111111)
    val TextPrimary    = Color(0xFFF0F0F2)
    val TextSecondary  = Color(0xFF8A8A90)
    val Border         = Color(0xFF1A1A1A)
    val BorderStrong   = Color(0xFF222222)
}

private val darkColorScheme = darkColorScheme(
    primary          = VeilColors.Accent,
    onPrimary        = Color(0xFF0A0A0B),
    primaryContainer = VeilColors.AccentSubtle,
    background       = DarkColors.Background,
    surface          = DarkColors.Surface,
    surfaceVariant   = DarkColors.SurfaceVariant,
    onBackground     = DarkColors.TextPrimary,
    onSurface        = DarkColors.TextPrimary,
    onSurfaceVariant = DarkColors.TextSecondary,
    outline          = DarkColors.Border,
    error            = VeilColors.Error
)

private val lightColorScheme = lightColorScheme(
    primary          = VeilColors.Accent,
    onPrimary        = Color(0xFF0A0A0B),
    primaryContainer = VeilColors.AccentSubtle,
    background       = LightColors.Background,
    surface          = LightColors.Surface,
    surfaceVariant   = LightColors.SurfaceVariant,
    onBackground     = LightColors.TextPrimary,
    onSurface        = LightColors.TextPrimary,
    onSurfaceVariant = LightColors.TextSecondary,
    outline          = LightColors.Border,
    error            = VeilColors.Error
)

private val amoledColorScheme = darkColorScheme(
    primary          = VeilColors.Accent,
    onPrimary        = Color(0xFF000000),
    primaryContainer = VeilColors.AccentSubtle,
    background       = AmoledColors.Background,
    surface          = AmoledColors.Surface,
    surfaceVariant   = AmoledColors.SurfaceVariant,
    onBackground     = AmoledColors.TextPrimary,
    onSurface        = AmoledColors.TextPrimary,
    onSurfaceVariant = AmoledColors.TextSecondary,
    outline          = AmoledColors.Border,
    error            = VeilColors.Error
)

val VeilTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight    = FontWeight.Medium,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 26.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun VeilTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content : @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = when (appTheme) {
        AppTheme.DARK   -> darkColorScheme
        AppTheme.LIGHT  -> lightColorScheme
        AppTheme.AMOLED -> amoledColorScheme
        AppTheme.SYSTEM -> if (isSystemDark) darkColorScheme else lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VeilTypography,
        content     = content
    )
}
