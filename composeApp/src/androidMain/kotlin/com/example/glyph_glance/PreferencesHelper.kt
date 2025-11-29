package com.example.glyph_glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.example.glyph_glance.data.database.AppDatabase
import com.example.glyph_glance.data.preferences.AndroidPreferencesManager
import com.example.glyph_glance.data.preferences.PreferencesManager
import com.example.glyph_glance.data.repository.NotificationRepository
import com.example.glyph_glance.data.repository.NotificationRepositoryImpl

@Composable
actual fun rememberPreferencesManager(): PreferencesManager {
    val context = LocalContext.current
    return remember { AndroidPreferencesManager(context) }
}

@Composable
actual fun rememberNotificationRepository(): NotificationRepository? {
    val context = LocalContext.current
    return remember {
        try {
            val database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "glyph_glance_db"
            )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
            NotificationRepositoryImpl(database.notificationDao())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
