package com.example.glyph_glance.data.models

enum class NotificationPriority {
    HIGH,
    MEDIUM,
    LOW;
    
    companion object {
        fun fromImportance(importance: Int): NotificationPriority {
            return when {
                importance >= 4 -> HIGH      // IMPORTANCE_HIGH and above
                importance >= 3 -> MEDIUM    // IMPORTANCE_DEFAULT
                else -> LOW                   // IMPORTANCE_LOW and below
            }
        }
    }
}
