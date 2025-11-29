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
import com.example.glyph_glance.logic.SentimentAnalysisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlyphNotificationListenerService : NotificationListenerService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var database: AppDatabase
    private lateinit var repository: NotificationRepositoryImpl
    private val sentimentAnalysisService = SentimentAnalysisService()
    
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "glyph_glance_db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
        
        repository = NotificationRepositoryImpl(database.notificationDao())
        
        // Initialize sentiment analysis service in background
        serviceScope.launch {
            try {
                val initialized = sentimentAnalysisService.initialize()
                if (initialized) {
                    Log.d(TAG, "Sentiment analysis service initialized successfully")
                } else {
                    Log.w(TAG, "Failed to initialize sentiment analysis service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing sentiment analysis service", e)
            }
        }
        
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
        
        // Create notification model (initially without sentiment)
        var notificationModel = com.example.glyph_glance.data.models.Notification(
            title = title,
            message = message,
            timestamp = sbn.postTime,
            priority = priority,
            appPackage = sbn.packageName,
            appName = appName
        )
        
        // Process notification: analyze sentiment and save to database
        serviceScope.launch {
            try {
                // Perform sentiment analysis
                val fullText = if (title.isNotEmpty() && message.isNotEmpty()) {
                    "$title: $message"
                } else {
                    title.ifEmpty { message }
                }
                
                val analysisResult = sentimentAnalysisService.analyzeNotification(
                    content = fullText,
                    senderId = appName
                )
                
                // Update notification model with sentiment analysis results
                notificationModel = notificationModel.copy(
                    sentiment = analysisResult?.sentiment,
                    urgencyScore = analysisResult?.urgencyScore
                )
                
                // Save to database
                repository.insertNotification(notificationModel)
                
                Log.d(TAG, "Saved notification: $title from $appName (Priority: $priority, " +
                        "Sentiment: ${analysisResult?.sentiment}, Urgency: ${analysisResult?.urgencyScore})")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                // Save without sentiment if analysis fails
                try {
                    repository.insertNotification(notificationModel)
                } catch (saveError: Exception) {
                    Log.e(TAG, "Error saving notification", saveError)
                }
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle notification removal
        Log.d(TAG, "Notification removed: ${sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sentimentAnalysisService.unload()
        database.close()
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
