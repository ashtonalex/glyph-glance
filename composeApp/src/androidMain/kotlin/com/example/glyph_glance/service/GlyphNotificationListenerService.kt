package com.example.glyph_glance.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.glyph_glance.data.models.NotificationPriority
import com.example.glyph_glance.data.repository.NotificationRepositoryImpl
import com.example.glyph_glance.di.AppModule
import com.example.glyph_glance.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification Listener Service that intercepts all notifications and routes them
 * through the BufferEngine for split-text handling, then to IntelligenceEngine for
 * AI analysis, and finally triggers GlyphManager for hardware feedback.
 */
class GlyphNotificationListenerService : NotificationListenerService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bufferEngine: BufferEngine? = null
    private var notificationRepository: NotificationRepositoryImpl? = null
    
    // Packages to ignore (system apps, own app)
    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.example.glyph_glance"
    )
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize AppModule if not already initialized
            AppModule.initialize(applicationContext)
            
            // Get dependencies from AppModule
            bufferEngine = AppModule.getBufferEngine()
            notificationRepository = NotificationRepositoryImpl(
                AppModule.getDatabase().notificationDao()
            )
            
            LiveLogger.addLog("NotificationListener: Service started")
            Log.d(TAG, "Notification Listener Service Created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NotificationListenerService", e)
            LiveLogger.addLog("NotificationListener: ERROR - ${e.message}")
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        
        // Filter out ignored packages
        if (shouldIgnore(sbn)) {
            return
        }
        
        // Get app name
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            sbn.packageName
        }
        
        // Extract notification details
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val message = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        // Ignore empty notifications
        if (title.isEmpty() && message.isEmpty()) return
        
        // Combine title and message for analysis
        val combinedText = "$title: $message"
        
        // Use package name as sender ID for buffering
        val senderId = sbn.packageName
        
        // Log the interception
        LiveLogger.addLog("Intercepted: $appName - ${message.take(30)}...")
        Log.d(TAG, "Intercepted notification: $title from $appName")
        
        // Determine priority based on notification importance
        val importance = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            sbn.notification.priority
        } else {
            notification.priority
        }
        val priority = NotificationPriority.fromImportance(importance)
        
        // Create notification model for database storage
        val notificationModel = com.example.glyph_glance.data.models.Notification(
            title = title,
            message = message,
            timestamp = sbn.postTime,
            priority = priority,
            appPackage = sbn.packageName,
            appName = appName
        )
        
        serviceScope.launch {
            try {
                // Save to database for UI display
                notificationRepository?.insertNotification(notificationModel)
                
                // Route through BufferEngine for split-text handling and AI analysis
                // BufferEngine will call IntelligenceEngine and GlyphManager
                bufferEngine?.handleIncoming(senderId, combinedText)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                LiveLogger.addLog("ERROR: ${e.message}")
            }
        }
    }
    
    /**
     * Check if this notification should be ignored.
     */
    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        // Ignore ongoing notifications (music players, downloads, etc.)
        if (sbn.isOngoing) return true
        
        // Ignore system packages
        if (ignoredPackages.contains(sbn.packageName)) return true
        
        // Ignore our own app's notifications
        if (sbn.packageName == packageName) return true
        
        return false
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        Log.d(TAG, "Notification removed: ${sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LiveLogger.addLog("NotificationListener: Service stopped")
        Log.d(TAG, "Notification Listener Service Destroyed")
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
