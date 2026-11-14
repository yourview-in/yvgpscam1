package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode

private val YvGpsCamDarkColorScheme = darkColorScheme(
    primary = TelemetryEmerald,
    onPrimary = ObsidianBg,
    primaryContainer = TelemetryEmeraldContainer,
    onPrimaryContainer = TelemetryEmerald,
    secondary = SignalCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceHigh,
    onSecondaryContainer = InkPrimary,
    tertiary = SignalAmber,
    onTertiary = ObsidianBg,
    tertiaryContainer = SignalAmberContainer,
    onTertiaryContainer = SignalAmber,
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = ObsidianSurface,
    onSurface = InkPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder.copy(alpha = 0.6f),
    error = AlertCoral,
    onError = InkPrimary,
    errorContainer = AlertCoralContainer,
    onErrorContainer = AlertCoral
)

private val YvGpsCamLightColorScheme = lightColorScheme(
    primary = Color(0xFF0A8F62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F6EA),
    onPrimaryContainer = Color(0xFF044A32),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EFF7),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val YvGpsCamShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YvGpsCamTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) YvGpsCamDarkColorScheme else YvGpsCamLightColorScheme,
        typography = YvGpsCamTypography,
        shapes = YvGpsCamShapes,
        content = content
    )
}
