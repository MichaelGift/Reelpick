package com.myth.movieapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.myth.movieapp.R

// Both families ship as variable fonts; each weight is an instance of the same file.

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Fraunces at a large optical size with softened terminals: an editorial, cinematic display face. */
@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int) = Font(
    R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", 96f),
        FontVariation.Setting("SOFT", 50f),
    ),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))
val Fraunces = FontFamily(fraunces(500), fraunces(600), fraunces(700))

private val base = Typography()

private fun TextStyle.ui(weight: Int, tracking: Double = 0.0) =
    copy(fontFamily = Manrope, fontWeight = FontWeight(weight), letterSpacing = tracking.em)

private fun TextStyle.display(weight: Int, tracking: Double) =
    copy(fontFamily = Fraunces, fontWeight = FontWeight(weight), letterSpacing = tracking.em)

val AppTypography = Typography(
    displayLarge = base.displayLarge.display(600, -0.02),
    displayMedium = base.displayMedium.display(600, -0.02),
    displaySmall = base.displaySmall.display(600, -0.015),
    headlineLarge = base.headlineLarge.display(600, -0.01),
    headlineMedium = base.headlineMedium.display(600, -0.01),
    headlineSmall = base.headlineSmall.display(600, -0.005),
    titleLarge = base.titleLarge.ui(700, -0.01),
    titleMedium = base.titleMedium.ui(700),
    titleSmall = base.titleSmall.ui(700),
    bodyLarge = base.bodyLarge.ui(500).copy(lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.ui(500).copy(lineHeight = 22.sp),
    bodySmall = base.bodySmall.ui(500),
    labelLarge = base.labelLarge.ui(700, 0.01),
    labelMedium = base.labelMedium.ui(700, 0.02),
    labelSmall = base.labelSmall.ui(700, 0.04),
)

/** Small uppercase eyebrow label above section titles. */
val EyebrowStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(800), fontSize = 11.sp, letterSpacing = 0.14.em)
