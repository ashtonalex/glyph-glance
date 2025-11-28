package com.example.glyph_glance.data.database

import androidx.room.TypeConverter
import com.example.glyph_glance.data.models.NotificationPriority

class Converters {
    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toNotificationPriority(value: String): NotificationPriority {
        return NotificationPriority.valueOf(value)
    }
}
