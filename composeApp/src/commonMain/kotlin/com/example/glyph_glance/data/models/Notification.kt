package com.example.glyph_glance.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val priority: NotificationPriority,
    val appPackage: String,
    val appName: String,
    val isRead: Boolean = false
)
