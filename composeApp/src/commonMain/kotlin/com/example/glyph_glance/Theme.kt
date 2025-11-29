package com.example.glyph_glance

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.glyph_glance.data.models.LedTheme
import com.example.glyph_glance.data.models.NotificationPriority

// Theme colors that can be dynamically changed
data class ThemeColors(
    val highPriority: Color,
    val mediumPriority: Color,
    val lowPriority: Color
) {
    companion object {
        val Default = ThemeColors(
            highPriority = NeonRed,
            mediumPriority = NeonBlue,
            lowPriority = NeonGreen
        )
    }
}

// CompositionLocal to provide theme colors throughout the app
val LocalThemeColors = compositionLocalOf { ThemeColors.Default }

@Composable
fun GlyphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val prefsManager = rememberPreferencesManager()
    var ledTheme by remember { mutableStateOf(LedTheme()) }
    
    // Load theme on initialization
    LaunchedEffect(Unit) {
        ledTheme = prefsManager.getLedTheme()
    }
    
    // Reload theme periodically to catch changes when presets are selected
    // This ensures theme updates across all screens when a preset is applied
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Small delay to let initial load complete
        while (true) {
            kotlinx.coroutines.delay(200) // Check every 200ms for theme changes (faster response)
            val newTheme = prefsManager.getLedTheme()
            // Use string comparison to detect any changes
            if (newTheme.highPriorityColor != ledTheme.highPriorityColor ||
                newTheme.mediumPriorityColor != ledTheme.mediumPriorityColor ||
                newTheme.lowPriorityColor != ledTheme.lowPriorityColor) {
                ledTheme = newTheme
            }
        }
    }
    
    // Create theme colors from LED theme - don't use remember to ensure recomposition
    val themeColors = ThemeColors(
        highPriority = ledTheme.getColorForPriority(NotificationPriority.HIGH),
        mediumPriority = ledTheme.getColorForPriority(NotificationPriority.MEDIUM),
        lowPriority = ledTheme.getColorForPriority(NotificationPriority.LOW)
    )
    
    val colorScheme = remember(themeColors.highPriority, themeColors.mediumPriority, themeColors.lowPriority) {
        darkColorScheme(
            primary = themeColors.mediumPriority,
            secondary = NeonPurple,
            tertiary = themeColors.lowPriority,
            background = DarkBackground,
            surface = SurfaceBlack,
            onPrimary = TextWhite,
            onSecondary = TextWhite,
            onTertiary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite,
        )
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}

// Helper functions to get theme colors
@Composable
fun getThemeHighPriority(): Color = LocalThemeColors.current.highPriority

@Composable
fun getThemeMediumPriority(): Color = LocalThemeColors.current.mediumPriority

@Composable
fun getThemeLowPriority(): Color = LocalThemeColors.current.lowPriority
