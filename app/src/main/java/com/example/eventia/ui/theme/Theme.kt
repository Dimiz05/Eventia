package com.example.eventia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Sage = Color(0xFF48602D)
internal val PaleSage = Color(0xFFD7E3C9)
internal val Ink = Color(0xFF182019)
internal val MutedInk = Color(0xFF465043)

private val EventiaColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = PaleSage,
    onPrimaryContainer = Color(0xFF293A20),
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = PaleSage,
    onSecondaryContainer = Ink,
    tertiary = Sage,
    tertiaryContainer = PaleSage,
    onTertiaryContainer = Ink,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    surfaceVariant = PaleSage,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9F4),
    surfaceContainer = Color(0xFFF1F5EC),
    surfaceContainerHigh = Color(0xFFE8EFE0),
    surfaceContainerHighest = PaleSage,
    outline = Color(0xFF78876F),
    outlineVariant = Color(0xFFCBD6C4)
)

@Composable
fun EventiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EventiaColors, typography = EventiaTypography, content = content)
}
