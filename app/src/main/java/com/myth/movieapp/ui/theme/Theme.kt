package com.myth.movieapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.myth.core.design.BrandPalette
import com.myth.core.design.BrandTheme
import com.myth.movieapp.domain.model.ThemeMode

/** Reelpick: an ember → magenta accent with gold ratings. */
val ReelpickPalette = BrandPalette(
    accent = Color(0xFFFF6B4A),
    accentEnd = Color(0xFFE8336D),
    onAccent = Color(0xFF1C0700),
    accentContainer = Color(0xFF3B1409),
    onAccentContainer = Color(0xFFFFDACF),
    secondary = Color(0xFFF4C46B),
    onSecondary = Color(0xFF261A00),
    tertiary = Color(0xFFB4A8FF),
    accentLight = Color(0xFFD5401F),
    accentContainerLight = Color(0xFFFFDACF),
    onAccentContainerLight = Color(0xFF3B0900),
    secondaryLight = Color(0xFF9A6A00),
    tertiaryLight = Color(0xFF5B4DC7),
    ratingLight = Color(0xFFB07800),
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
    BrandTheme(ReelpickPalette, dark, content)
}
