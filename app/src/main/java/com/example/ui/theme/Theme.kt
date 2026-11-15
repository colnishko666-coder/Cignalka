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
import com.example.data.model.ThemeMode

private val CockpitDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = IgnitionAmber,
    onSecondary = Color.White,
    secondaryContainer = IgnitionAmberContainer,
    onSecondaryContainer = IgnitionAmberGlow,
    tertiary = SafeEmerald,
    onTertiary = ObsidianBg,
    tertiaryContainer = SafeEmeraldContainer,
    onTertiaryContainer = SafeEmerald,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = Color(0xFFFCA5A5),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedGraphite,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = ElevatedGraphite,
    surfaceContainerHigh = HighlightGraphite,
    outline = SubtleBorderDark,
    outlineVariant = Color(0xFF1F2A3D)
)

private val CockpitLightColorScheme = lightColorScheme(
    primary = CyanLightPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanLightContainer,
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = IgnitionAmber,
    onSecondary = Color.White,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = DaylightSurface,
    surfaceContainerLow = DaylightBg,
    surfaceContainer = DaylightElevated,
    surfaceContainerHigh = Color(0xFFDDE6F2),
    outline = DaylightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

val CockpitShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun IbtStartTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) CockpitDarkColorScheme else CockpitLightColorScheme,
        typography = AppTypography,
        shapes = CockpitShapes,
        content = content
    )
}
