package com.myth.core.design

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

/**
 * What makes each app look like itself. Surfaces, type and shapes are shared; the accent, its
 * gradient and the secondary colour are the brand.
 */
@Immutable
data class BrandPalette(
    /** Gradient start; also the primary colour in dark mode. */
    val accent: Color,
    /** Gradient end. */
    val accentEnd: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Secondary colour in dark mode; also used for star ratings there. */
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val accentLight: Color,
    val accentContainerLight: Color,
    val onAccentContainerLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val ratingLight: Color,
)

private fun darkColors(p: BrandPalette) = darkColorScheme(
    primary = p.accent,
    onPrimary = p.onAccent,
    primaryContainer = p.accentContainer,
    onPrimaryContainer = p.onAccentContainer,
    secondary = p.secondary,
    onSecondary = p.onSecondary,
    tertiary = p.tertiary,
    // Near-black "screening room" surfaces shared by every app.
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

private fun lightColors(p: BrandPalette) = lightColorScheme(
    primary = p.accentLight,
    onPrimary = Color.White,
    primaryContainer = p.accentContainerLight,
    onPrimaryContainer = p.onAccentContainerLight,
    secondary = p.secondaryLight,
    tertiary = p.tertiaryLight,
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

private fun brandTokens(palette: BrandPalette, colors: ColorScheme, dark: Boolean): BrandTokens {
    val gradientColors = listOf(palette.accent, palette.accentEnd)
    return BrandTokens(
        gradient = Brush.linearGradient(gradientColors),
        gradientColors = gradientColors,
        rating = if (dark) palette.secondary else palette.ratingLight,
        match = if (dark) Color(0xFF5EE0A0) else Color(0xFF13804E),
        fadeToBackground = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to colors.background.copy(alpha = 0.55f),
            1f to colors.background,
        ),
        isDark = dark,
    )
}

private val LocalBrand = staticCompositionLocalOf<BrandTokens> { error("BrandTheme is missing") }

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

/** Material theme plus brand tokens for one app. */
@Composable
fun BrandTheme(palette: BrandPalette, dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColors(palette) else lightColors(palette)
    CompositionLocalProvider(LocalBrand provides brandTokens(palette, colors, dark)) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
