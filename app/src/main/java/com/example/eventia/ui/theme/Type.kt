package com.example.eventia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.eventia.R

private val CrimsonText = FontFamily(Font(R.font.crimson_text_regular, FontWeight.Normal))

private fun crimson(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = CrimsonText,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.None,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

internal val EventiaTypography = Typography(
    displayLarge = crimson(57, 64),
    displayMedium = crimson(45, 52),
    displaySmall = crimson(36, 44),
    headlineLarge = crimson(32, 40),
    headlineMedium = crimson(28, 32),
    headlineSmall = crimson(24, 28),
    titleLarge = crimson(22, 24),
    titleMedium = crimson(16, 20),
    titleSmall = crimson(14, 18),
    bodyLarge = crimson(16, 22),
    bodyMedium = crimson(14, 18),
    bodySmall = crimson(13, 16),
    labelLarge = crimson(14, 18),
    labelMedium = crimson(14, 18),
    labelSmall = crimson(13, 16)
)
