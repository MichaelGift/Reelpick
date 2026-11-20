package com.myth.playpick.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.myth.playpick.R

// Playpick's "HUD" type: Space Grotesk for titles, Inter for reading, JetBrains Mono for every
// number, label and tag. All three ship as variable fonts (SIL OFL, see games/licenses).

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val SpaceGrotesk = FontFamily(variable(R.font.space_grotesk, 500), variable(R.font.space_grotesk, 700))
val Inter = FontFamily(variable(R.font.inter, 400), variable(R.font.inter, 500), variable(R.font.inter, 600), variable(R.font.inter, 700))
val JetBrainsMono = FontFamily(variable(R.font.jetbrains_mono, 500), variable(R.font.jetbrains_mono, 700))

private val base = Typography()

private fun TextStyle.display(size: Int, tracking: Double = -0.01) =
    copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.1).sp, letterSpacing = tracking.em)

private fun TextStyle.ui(weight: Int) = copy(fontFamily = Inter, fontWeight = FontWeight(weight), letterSpacing = 0.em)

val PlaypickTypography = Typography(
    displayLarge = base.displayLarge.display(48, -0.02),
    displayMedium = base.displayMedium.display(40, -0.02),
    displaySmall = base.displaySmall.display(30),
    headlineLarge = base.headlineLarge.display(28),
    headlineMedium = base.headlineMedium.display(24),
    headlineSmall = base.headlineSmall.display(18, 0.0),
    titleLarge = base.titleLarge.ui(700),
    titleMedium = base.titleMedium.ui(600),
    titleSmall = base.titleSmall.ui(600),
    bodyLarge = base.bodyLarge.ui(400).copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.ui(400).copy(lineHeight = 20.sp),
    bodySmall = base.bodySmall.ui(400),
    labelLarge = base.labelLarge.ui(600),
    labelMedium = base.labelMedium.ui(600),
    labelSmall = base.labelSmall.ui(600),
)

/** Small uppercase HUD label: section eyebrows, stat captions. */
val HudLabel = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.08.em)

/** Numbers that matter: match %, counts, hours, ratings. */
val HudStat = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.em)

/** Meta lines such as "2025 · ROGUELIKE · 24 H · ★ 4.6". */
val HudMeta = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.02.em)
