package com.example.glyph_glance.data.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String = "User",
    val greeting: String = "Welcome back"
)

@Serializable
data class KeywordRule(
    val keyword: String,
    val priority: NotificationPriority
)

@Serializable
data class AppRule(
    val packageName: String,
    val appName: String,
    val priority: NotificationPriority
)

@Serializable
data class LedTheme(
    val highPriorityColor: String = "#FF3B30",  // NeonRed
    val mediumPriorityColor: String = "#007AFF", // NeonBlue
    val lowPriorityColor: String = "#34C759"     // NeonGreen
) {
    fun getColorForPriority(priority: NotificationPriority): Color {
        return when (priority) {
            NotificationPriority.HIGH -> parseHexColor(highPriorityColor)
            NotificationPriority.MEDIUM -> parseHexColor(mediumPriorityColor)
            NotificationPriority.LOW -> parseHexColor(lowPriorityColor)
        }
    }
    
    companion object {
        fun parseHexColor(hex: String): Color {
            val cleanHex = hex.trim().removePrefix("#").uppercase()
            
            // Parse hex string to RGB components
            val colorInt = when {
                cleanHex.length == 6 -> cleanHex.toLongOrNull(16) ?: 0L
                cleanHex.length == 8 -> cleanHex.toLongOrNull(16) ?: 0L
                else -> 0L // Default to black if invalid
            }
            
            val r: Int
            val g: Int
            val b: Int
            val a: Int
            
            if (cleanHex.length == 8) {
                // ARGB format
                a = ((colorInt shr 24) and 0xFF).toInt()
                r = ((colorInt shr 16) and 0xFF).toInt()
                g = ((colorInt shr 8) and 0xFF).toInt()
                b = (colorInt and 0xFF).toInt()
            } else {
                // RGB format (default alpha to 255)
                r = ((colorInt shr 16) and 0xFF).toInt()
                g = ((colorInt shr 8) and 0xFF).toInt()
                b = (colorInt and 0xFF).toInt()
                a = 255
            }
            
            // Create Color using RGB components (alpha is normalized 0-1)
            return Color(
                red = r / 255f,
                green = g / 255f,
                blue = b / 255f,
                alpha = a / 255f
            )
        }
        
        fun colorToHex(color: Color): String {
            // Clamp values to valid range and convert to hex
            val r = (color.red * 255).toInt().coerceIn(0, 255)
            val g = (color.green * 255).toInt().coerceIn(0, 255)
            val b = (color.blue * 255).toInt().coerceIn(0, 255)
            return "#%02X%02X%02X".format(r, g, b)
        }
    }
}

@Serializable
data class ThemePreset(
    val name: String,
    val theme: LedTheme,
    val isCustom: Boolean = false
)

object ThemePresets {
    val CALM_SERENE = ThemePreset(
        name = "Calm & Serene",
        theme = LedTheme(
            highPriorityColor = "#E1F0F4",
            mediumPriorityColor = "#7FB3C9",
            lowPriorityColor = "#3A6B7E"
        )
    )
    
    val ENERGETIC_VIBRANT = ThemePreset(
        name = "Energetic & Vibrant",
        theme = LedTheme(
            highPriorityColor = "#FFE066",
            mediumPriorityColor = "#FF9A1F",
            lowPriorityColor = "#D8434E"
        )
    )
    
    val EARTHY_GROUNDED = ThemePreset(
        name = "Earthy & Grounded",
        theme = LedTheme(
            highPriorityColor = "#F5E9D5",
            mediumPriorityColor = "#C19A6B",
            lowPriorityColor = "#4A3C2A"
        )
    )
    
    val OCEAN_BREEZE = ThemePreset(
        name = "Ocean Breeze",
        theme = LedTheme(
            highPriorityColor = "#B8E6E6",
            mediumPriorityColor = "#5FB3B3",
            lowPriorityColor = "#2D5F5F"
        )
    )
    
    val SUNSET_GLOW = ThemePreset(
        name = "Sunset Glow",
        theme = LedTheme(
            highPriorityColor = "#FFD4B3",
            mediumPriorityColor = "#FF8C69",
            lowPriorityColor = "#C65D7B"
        )
    )
    
    val FOREST_CANOPY = ThemePreset(
        name = "Forest Canopy",
        theme = LedTheme(
            highPriorityColor = "#D4E6D1",
            mediumPriorityColor = "#7FB069",
            lowPriorityColor = "#2D5016"
        )
    )
    
    val PURPLE_DREAM = ThemePreset(
        name = "Purple Dream",
        theme = LedTheme(
            highPriorityColor = "#E6D4F7",
            mediumPriorityColor = "#B19CD9",
            lowPriorityColor = "#6B4C93"
        )
    )
    
    val NEON_DEFAULT = ThemePreset(
        name = "Neon Default",
        theme = LedTheme(
            highPriorityColor = "#FF3B30",
            mediumPriorityColor = "#007AFF",
            lowPriorityColor = "#34C759"
        )
    )
    
    val ALL_PRESETS = listOf(
        NEON_DEFAULT,
        CALM_SERENE,
        ENERGETIC_VIBRANT,
        EARTHY_GROUNDED,
        OCEAN_BREEZE,
        SUNSET_GLOW,
        FOREST_CANOPY,
        PURPLE_DREAM
    )
}
