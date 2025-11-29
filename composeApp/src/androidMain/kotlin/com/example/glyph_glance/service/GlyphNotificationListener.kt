package com.example.glyph_glance.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.glyph_glance.di.AppModule

/**
 * Notification listener service that intercepts all notifications.
 * Filters out system notifications and passes relevant ones to the BufferEngine.
 */
class GlyphNotificationListener : NotificationListenerService() {

    private var bufferEngine: BufferEngine? = null

    override fun onCreate() {
        super.onCreate()
        LiveLogger.addLog("GlyphNotificationListener: Service created")
        
        // Initialize BufferEngine from DI
        try {
            bufferEngine = AppModule.getBufferEngine()
            LiveLogger.addLog("GlyphNotificationListener: BufferEngine connected")
        } catch (e: IllegalStateException) {
            LiveLogger.addLog("GlyphNotificationListener: ERROR - AppModule not initialized: ${e.message}")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        LiveLogger.addLog("GlyphNotificationListener: Connected to notification service")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LiveLogger.addLog("GlyphNotificationListener: Disconnected from notification service")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            if (shouldIgnore(notification)) {
                return
            }
            
            // Extract notification content
            val title = notification.notification.extras.getString("android.title") ?: ""
            val text = notification.notification.extras.getString("android.text") ?: ""
            val senderPackage = notification.packageName
            
            // Log to UI (Track 3)
            LiveLogger.addLog("Intercepted: $senderPackage - ${text.take(30)}...")
            
            // Pass to Buffer Engine
            val combinedText = if (title.isNotEmpty() && text.isNotEmpty()) {
                "$title: $text"
            } else {
                title.ifEmpty { text }
            }
            
            bufferEngine?.handleIncoming(senderPackage, combinedText) ?: run {
                LiveLogger.addLog("ERROR: BufferEngine not available, notification dropped")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Optional: Handle notification removal if needed
    }

    /**
     * Determine if a notification should be ignored.
     * Filters out system notifications, ongoing notifications, and the app's own notifications.
     */
    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        
        // Ignore system notifications
        if (packageName == "android" || packageName == "com.android.systemui") {
            return true
        }
        
        // Ignore ongoing notifications (like music players)
        if (sbn.isOngoing) {
            return true
        }
        
        // Ignore our own app's notifications
        if (packageName == this.packageName) {
            return true
        }
        
        // Ignore notifications without text content
        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getString("android.text") ?: ""
        if (title.isEmpty() && text.isEmpty()) {
            return true
        }
        
        return false
    }
}

