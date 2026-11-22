package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.data.ThemeStyle

val LocalLiquidGlass = staticCompositionLocalOf { false }

private val ClassXiLightScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF4FF),
    onPrimaryContainer = RoyalBlueDark,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = EmeraldKas,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECFDF5),
    onTertiaryContainer = EmeraldDark,
    error = RoseAlpa,
    onError = Color.White,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFFB91C1C),
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

private val ClassXiDarkScheme = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color(0xFF001C4D),
    primaryContainer = Color(0xFF0A4FC4),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6),
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

private val ClassXiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun ClassXiTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    themeStyle: ThemeStyle = ThemeStyle.DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val isGlass = themeStyle == ThemeStyle.GLASS
    val baseScheme = if (darkTheme) ClassXiDarkScheme else ClassXiLightScheme
    val colorScheme = if (isGlass) {
        baseScheme.copy(
            background = if (darkTheme) Color(0xFF060B18) else Color(0xFFDCE8FB),
            surface = if (darkTheme) Color(0xCC182242) else Color(0xD9FFFFFF),
            surfaceVariant = if (darkTheme) Color(0x66243049) else Color(0x99EEF4FF)
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalLiquidGlass provides isGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ClassXiTypography,
            shapes = ClassXiShapes,
            content = content
        )
    }
}
