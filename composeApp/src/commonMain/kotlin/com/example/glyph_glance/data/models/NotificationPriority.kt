package com.example.glyph_glance.data.models

enum class NotificationPriority {
    HIGH,
    MEDIUM,
    LOW;
    
    /**
     * Converts priority to urgency score (1-5)
     * HIGH -> 5 (Critical)
     * MEDIUM -> 3 (Medium)
     * LOW -> 1 (Low)
     */
    fun toUrgencyScore(): Int {
        return when (this) {
            HIGH -> 5
            MEDIUM -> 3
            LOW -> 1
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
         * Converts urgency score (1-5) to priority level
         * 5, 4 -> HIGH
         * 3, 2 -> MEDIUM
         * 1 -> LOW
         */
        fun fromUrgencyScore(urgencyScore: Int): NotificationPriority {
            return when {
                urgencyScore >= 4 -> HIGH
                urgencyScore >= 2 -> MEDIUM
                else -> LOW
            }
        }
    }
}
