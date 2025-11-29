package com.example.glyph_glance.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.glyph_glance.data.models.Notification

@Database(
    entities = [Notification::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sentiment column (nullable TEXT)
                database.execSQL("ALTER TABLE notifications ADD COLUMN sentiment TEXT")
                // Add urgencyScore column (nullable INTEGER)
                database.execSQL("ALTER TABLE notifications ADD COLUMN urgencyScore INTEGER")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add rawAiResponse column (nullable TEXT) for storing AI thought process
                database.execSQL("ALTER TABLE notifications ADD COLUMN rawAiResponse TEXT")
            }
        }
    }
}
