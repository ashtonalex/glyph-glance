package com.example.glyph_glance.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Helper utilities for checking and requesting notification listener access.
 */
object NotificationPermissionHelper {
    
    /**
     * Check if notification listener access is enabled for this app.
     * 
     * @param context The application context
     * @return true if notification access is granted, false otherwise
     */
    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        val packageName = context.packageName
        return enabledListeners?.contains(packageName) == true
    }
    
    /**
     * Open the Android Settings page for notification access.
     * This allows users to enable notification listener access for the app.
     * 
     * @param context The context to start the activity from
     */
    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

