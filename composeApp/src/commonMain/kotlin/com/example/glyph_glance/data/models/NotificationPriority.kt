package com.example.glyph_glance.data.models

enum class NotificationPriority {
    HIGH,
    MEDIUM,
    LOW;
    
    /**
     * Converts priority to urgency score (1-6)
     * HIGH -> 6 (High)
     * MEDIUM -> 4 (Medium)
     * LOW -> 2 (Low)
     */
    fun toUrgencyScore(): Int {
        return when (this) {
            HIGH -> 6
            MEDIUM -> 4
            LOW -> 2
        }
    }
    
    companion object {
        fun fromImportance(importance: Int): NotificationPriority {
            return when {
                importance >= 4 -> HIGH      // IMPORTANCE_HIGH and above
                importance >= 3 -> MEDIUM    // IMPORTANCE_DEFAULT
                else -> LOW                   // IMPORTANCE_LOW and below
            }
        }
        
        /**
         * Converts urgency score (1-6) to priority level
         * 5, 6 -> HIGH
         * 3, 4 -> MEDIUM
         * 1, 2 -> LOW
         */
        fun fromUrgencyScore(urgencyScore: Int): NotificationPriority {
            val clamped = urgencyScore.coerceIn(1, 6)
            return when (clamped) {
                5, 6 -> HIGH
                3, 4 -> MEDIUM
                else -> LOW // 1 or 2
            }
        }
    }
}
