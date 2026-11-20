package com.myth.playpick.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.myth.core.design.BrandPalette
import com.myth.core.design.BrandTheme
import com.myth.playpick.domain.model.ThemeMode

/** Playpick: an electric violet → cyan accent, sharing Reelpick's surfaces and type. */
val PlaypickPalette = BrandPalette(
    accent = Color(0xFF9B6BFF),
    accentEnd = Color(0xFF22D3EE),
    onAccent = Color(0xFF16003D),
    accentContainer = Color(0xFF2A1260),
    onAccentContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFF4C46B),
    onSecondary = Color(0xFF261A00),
    tertiary = Color(0xFF67E8F9),
    accentLight = Color(0xFF6D3BE6),
    accentContainerLight = Color(0xFFE9DDFF),
    onAccentContainerLight = Color(0xFF22005C),
    secondaryLight = Color(0xFF9A6A00),
    tertiaryLight = Color(0xFF0E7490),
    ratingLight = Color(0xFFB07800),
)

@Composable
fun PlaypickTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    BrandTheme(PlaypickPalette, dark, content)
}
