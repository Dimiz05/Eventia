package com.example.eventia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADD0FF),
    onPrimary = Color(0xFF0B335C),
    primaryContainer = Color(0xFF214B76),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFFA6D8E8),
    onSecondary = Color(0xFF113744),
    secondaryContainer = Color(0xFF234957),
    onSecondaryContainer = Color(0xFFCEF0FA),
    background = Color(0xFF101B29),
    onBackground = Color(0xFFF1F6FC),
    surface = Color(0xFF101B29),
    onSurface = Color(0xFFF1F6FC),
    surfaceContainer = Color(0xFF1A293B),
    surfaceContainerHighest = Color(0xFF2C4055),
    onSurfaceVariant = Color(0xFFBBCDDF),
    outline = Color(0xFF8EA3B9),
    outlineVariant = Color(0xFF425972)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF17385F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFF),
    onPrimaryContainer = Color(0xFF132F51),
    secondary = Color(0xFF275B6C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEFF5),
    onSecondaryContainer = Color(0xFF154556),
    background = Color(0xFFF7FAFE),
    onBackground = Color(0xFF132238),
    surface = Color(0xFFF7FAFE),
    onSurface = Color(0xFF132238),
    surfaceContainer = Color(0xFFEEF4FA),
    surfaceContainerHighest = Color(0xFFDFEAF4),
    onSurfaceVariant = Color(0xFF43566C),
    outline = Color(0xFF6B7E93),
    outlineVariant = Color(0xFFC9D7E5)
)

@Composable
fun EventiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
