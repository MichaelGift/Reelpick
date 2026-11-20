package com.myth.playpick.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myth.core.design.BrandPalette
import com.myth.core.design.BrandTheme
import com.myth.playpick.domain.model.ThemeMode

/**
 * Playpick's HUD look: graphite surfaces and one electric-lime accent, used only for what you can
 * act on or have earned. The accent is flat (no gradient), with dark text on it.
 */
object Hud {
    val Lime = Color(0xFFC6FF3D)
    val Ink = Color(0xFF0B0D10)
    val Panel = Color(0xFF14171C)
    val Raised = Color(0xFF1B1F26)
    val Line = Color(0xFF262B33)
    val Text = Color(0xFFF2F4F7)
    val Muted = Color(0xFF9AA3AF)
    val Label = Color(0xFF8A93A0)
    val Gold = Color(0xFFF5B83D)

    /** Corner radius of buttons, pills and tags. */
    val ControlShape = RoundedCornerShape(12.dp)
    val TagShape = RoundedCornerShape(7.dp)
    val CardShape = RoundedCornerShape(16.dp)
}

val PlaypickPalette = BrandPalette(
    accent = Hud.Lime,
    accentEnd = Hud.Lime,
    onAccent = Hud.Ink,
    accentContainer = Color(0xFF2C3A0B),
    onAccentContainer = Color(0xFFE6FFB0),
    secondary = Hud.Gold,
    onSecondary = Color(0xFF261A00),
    tertiary = Color(0xFF60A5FA),
    // Lime can't carry text on a light background, so light mode uses a deep olive instead.
    accentLight = Color(0xFF4D7C0F),
    accentContainerLight = Color(0xFFE6FFB0),
    onAccentContainerLight = Color(0xFF1A2600),
    secondaryLight = Color(0xFF9A6A00),
    tertiaryLight = Color(0xFF1D4ED8),
    ratingLight = Color(0xFFB07800),
    onGradient = Hud.Ink,
)

@Composable
fun PlaypickTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    BrandTheme(PlaypickPalette, dark, typography = PlaypickTypography, controlShape = Hud.ControlShape, content = content)
}
