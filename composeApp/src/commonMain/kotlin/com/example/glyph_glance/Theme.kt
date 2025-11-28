package com.example.glyph_glance

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonPurple,
    tertiary = NeonGreen,
    background = DarkBackground,
    surface = SurfaceBlack,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

@Composable
fun GlyphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Force dark theme by default for this aesthetic?
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    // For this specific "Dark/Neon" aesthetic, we might want to enforce dark mode
    // or at least default to it.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography, // We can customize this later if needed
        content = content
    )
}
