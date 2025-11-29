package com.example.glyph_glance.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.glyph_glance.data.models.Notification
import com.example.glyph_glance.data.models.NotificationPriority

@Database(
    entities = [ContactProfile::class, Rule::class, Notification::class], 
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun ruleDao(): RuleDao
    abstract fun notificationDao(): NotificationDao
}
