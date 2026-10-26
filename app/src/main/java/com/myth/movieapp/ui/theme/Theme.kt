package com.myth.movieapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.myth.movieapp.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A65),
    onPrimary = Color(0xFF3A0B00),
    primaryContainer = Color(0xFF5C1D0A),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF9ECAFF),
    onSecondary = Color(0xFF003258),
    background = Color(0xFF101014),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF101014),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF26262D),
    onSurfaceVariant = Color(0xFFC9C5CA),
    surfaceContainer = Color(0xFF1A1A20),
    surfaceContainerHigh = Color(0xFF222229),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB5401C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF00629E),
    background = Color(0xFFFCF8F6),
    surface = Color(0xFFFCF8F6),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun MovieAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
