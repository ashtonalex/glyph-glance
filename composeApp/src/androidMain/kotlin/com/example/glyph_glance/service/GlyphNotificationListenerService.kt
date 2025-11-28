package com.example.glyph_glance.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.room.Room
import com.example.glyph_glance.data.database.AppDatabase
import com.example.glyph_glance.data.models.NotificationPriority
import com.example.glyph_glance.data.repository.NotificationRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlyphNotificationListenerService : NotificationListenerService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var database: AppDatabase
    private lateinit var repository: NotificationRepositoryImpl
    
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "glyph_glance_db"
        ).build()
        
        repository = NotificationRepositoryImpl(database.notificationDao())
        Log.d(TAG, "Notification Listener Service Created")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        
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
        
        // Determine priority based on notification importance
        val importance = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            sbn.notification.priority
        } else {
            notification.priority
        }
        
        val priority = NotificationPriority.fromImportance(importance)
        
        // Create notification model
        val notificationModel = com.example.glyph_glance.data.models.Notification(
            title = title,
            message = message,
            timestamp = sbn.postTime,
            priority = priority,
            appPackage = sbn.packageName,
            appName = appName
        )
        
        // Save to database
        serviceScope.launch {
            try {
                repository.insertNotification(notificationModel)
                Log.d(TAG, "Saved notification: $title from $appName (Priority: $priority)")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle notification removal
        Log.d(TAG, "Notification removed: ${sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
