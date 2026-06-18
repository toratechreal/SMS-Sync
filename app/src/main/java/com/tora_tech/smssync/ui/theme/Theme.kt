package com.tora_tech.smssync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = QuikTealDark,
    onPrimary = Color.White,
    primaryContainer = QuikTeal,
    onPrimaryContainer = Color.White,
    secondary = QuikTealDark,
    onSecondary = Color.White,
    tertiary = QuikTealDark,
    background = QuikBgDark,
    onBackground = QuikTextPrimaryDark,
    surface = QuikToolbarDark,
    onSurface = QuikTextPrimaryDark,
    surfaceVariant = QuikBubbleInDark,
    onSurfaceVariant = QuikTextSecondaryDark,
    outline = QuikSeparatorDark,
    outlineVariant = QuikSeparatorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = QuikTeal,
    onPrimary = Color.White,
    primaryContainer = QuikTeal,
    onPrimaryContainer = Color.White,
    secondary = QuikTeal,
    onSecondary = Color.White,
    tertiary = QuikTeal,
    background = QuikBgLight,
    onBackground = QuikTextPrimaryLight,
    surface = QuikToolbarLight,
    onSurface = QuikTextPrimaryLight,
    surfaceVariant = QuikBubbleInLight,
    onSurfaceVariant = QuikTextSecondaryLight,
    outline = QuikSeparatorLight,
    outlineVariant = QuikSeparatorLight,
)

@Composable
fun SMSSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // QUIK uses its own teal brand, so dynamic (Material You) color is intentionally off.
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
