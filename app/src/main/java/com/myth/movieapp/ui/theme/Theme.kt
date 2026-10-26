package com.myth.movieapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myth.movieapp.domain.model.ThemeMode

// Brand palette: near-black "screening room" surfaces with an ember → magenta accent.
private val Ember = Color(0xFFFF6B4A)
private val Magenta = Color(0xFFE8336D)
private val Gold = Color(0xFFF4C46B)

private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF1C0700),
    primaryContainer = Color(0xFF3B1409),
    onPrimaryContainer = Color(0xFFFFDACF),
    secondary = Gold,
    onSecondary = Color(0xFF261A00),
    tertiary = Color(0xFFB4A8FF),
    background = Color(0xFF09090C),
    onBackground = Color(0xFFF3F0EC),
    surface = Color(0xFF09090C),
    onSurface = Color(0xFFF3F0EC),
    surfaceVariant = Color(0xFF1F1E25),
    onSurfaceVariant = Color(0xFFA9A5AF),
    surfaceContainerLowest = Color(0xFF060608),
    surfaceContainerLow = Color(0xFF101014),
    surfaceContainer = Color(0xFF15151A),
    surfaceContainerHigh = Color(0xFF1C1C22),
    surfaceContainerHighest = Color(0xFF26252D),
    outline = Color(0xFF3B3942),
    outlineVariant = Color(0xFF26252C),
    inverseSurface = Color(0xFFF3F0EC),
    inverseOnSurface = Color(0xFF17161A),
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD5401F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDACF),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF9A6A00),
    tertiary = Color(0xFF5B4DC7),
    background = Color(0xFFFAF7F4),
    onBackground = Color(0xFF17151A),
    surface = Color(0xFFFAF7F4),
    onSurface = Color(0xFF17151A),
    surfaceVariant = Color(0xFFEEE9E4),
    onSurfaceVariant = Color(0xFF5E5A63),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F1ED),
    surfaceContainer = Color(0xFFF0EBE6),
    surfaceContainerHigh = Color(0xFFEAE4DF),
    surfaceContainerHighest = Color(0xFFE3DDD8),
    outline = Color(0xFFCFC8C2),
    outlineVariant = Color(0xFFE3DDD8),
)

/** Brand values Material's colour scheme has no slot for. */
@Immutable
data class BrandTokens(
    val gradient: Brush,
    val gradientColors: List<Color>,
    val rating: Color,
    val match: Color,
    /** Gradient from transparent to the page background, used to fade artwork into content. */
    val fadeToBackground: Brush,
    val isDark: Boolean,
)

private fun brandTokens(colors: ColorScheme, dark: Boolean): BrandTokens {
    val gradientColors = listOf(Ember, Magenta)
    return BrandTokens(
        gradient = Brush.linearGradient(gradientColors),
        gradientColors = gradientColors,
        rating = if (dark) Gold else Color(0xFFB07800),
        match = if (dark) Color(0xFF5EE0A0) else Color(0xFF13804E),
        fadeToBackground = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to colors.background.copy(alpha = 0.55f),
            1f to colors.background,
        ),
        isDark = dark,
    )
}

private val LocalBrand = staticCompositionLocalOf { brandTokens(DarkColors, dark = true) }

object Brand {
    val tokens: BrandTokens
        @Composable @ReadOnlyComposable get() = LocalBrand.current
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MovieAppTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalBrand provides brandTokens(colors, dark)) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
