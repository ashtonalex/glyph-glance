package com.example.glyph_glance.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.room.Room
import com.example.glyph_glance.data.database.AppDatabase
import com.example.glyph_glance.data.models.NotificationPriority
import com.example.glyph_glance.data.preferences.AndroidPreferencesManager
import com.example.glyph_glance.data.repository.NotificationRepositoryImpl
import com.example.glyph_glance.di.AppModule
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import com.example.glyph_glance.logic.GlyphPattern
import com.example.glyph_glance.logic.calculatePriority
import com.example.glyph_glance.logging.LiveLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlyphNotificationListenerService : NotificationListenerService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationDatabase: AppDatabase
    private lateinit var repository: NotificationRepositoryImpl
    private lateinit var preferencesManager: AndroidPreferencesManager
    private lateinit var intelligenceEngine: GlyphIntelligenceEngine
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AppModule for AI and contact/rule database
        AppModule.initialize(applicationContext)
        intelligenceEngine = AppModule.getIntelligenceEngine()
        
        // Initialize notification database (separate from contact/rule database)
        notificationDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "glyph_glance_db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
        
        repository = NotificationRepositoryImpl(notificationDatabase.notificationDao())
        preferencesManager = AndroidPreferencesManager(applicationContext)
        
        // Log service start
        LiveLogger.setServiceRunning(true)
        LiveLogger.setNotificationAccessEnabled(true)
        
        Log.d(TAG, "Notification Listener Service Created with GlyphIntelligenceEngine")
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
        
        // Log notification received
        LiveLogger.logNotificationReceived(
            appName = appName,
            title = title,
            preview = message
        )
        
        // Determine base priority based on notification importance
        val importance = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            sbn.notification.priority
        } else {
            notification.priority
        }
        
        val basePriority = NotificationPriority.fromImportance(importance)
        
        // Log buffer queue activity
        LiveLogger.logBufferQueue(
            senderId = appName,
            action = "Added to processing queue",
            queueSize = 1
        )
        
        // Process notification through GlyphIntelligenceEngine
        serviceScope.launch {
            try {
                // Build full text for analysis
                val fullText = if (title.isNotEmpty() && message.isNotEmpty()) {
                    "$title: $message"
                } else {
                    title.ifEmpty { message }
                }
                
                // Process through GlyphIntelligenceEngine (handles AI analysis, contact profiles, and DB rules)
                val decision = intelligenceEngine.processNotification(fullText, appName)
                
                // Also apply preferences-based rules (keyword and app rules from preferences)
                val keywordRules = preferencesManager.getKeywordRules()
                val appRules = preferencesManager.getAppRules()
                val preferencePriority = calculatePriority(
                    text = fullText,
                    appPackage = sbn.packageName,
                    appName = appName,
                    keywordRules = keywordRules,
                    appRules = appRules,
                    basePriority = basePriority
                )
                
                // Combine AI urgency with preference rules (take maximum)
                val preferenceUrgencyScore = preferencePriority.toUrgencyScore()
                val finalUrgencyScore = maxOf(decision.urgencyScore, preferenceUrgencyScore)
                val finalPriority = NotificationPriority.fromUrgencyScore(finalUrgencyScore)
                
                val notificationModel = com.example.glyph_glance.data.models.Notification(
                    title = title,
                    message = message,
                    timestamp = sbn.postTime,
                    priority = finalPriority,
                    appPackage = sbn.packageName,
                    appName = appName,
                    sentiment = decision.sentiment,
                    urgencyScore = finalUrgencyScore
                )
                
                // Save to database
                repository.insertNotification(notificationModel)
                
                // Log with glyph pattern info
                Log.d(TAG, "Saved notification: $title from $appName " +
                        "(Priority: $finalPriority, AI Urgency: ${decision.urgencyScore}, Final Urgency: $finalUrgencyScore, " +
                        "Sentiment: ${decision.sentiment}, Pattern: ${decision.pattern}, ShouldLightUp: ${decision.shouldLightUp})")
                
                // Handle glyph pattern if needed
                if (decision.shouldLightUp) {
                    handleGlyphPattern(decision.pattern)
                } else {
                    LiveLogger.logGlyphInteraction(decision.pattern.name, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                LiveLogger.logError("NotificationListener", e.message ?: "Error processing notification")
                
                // Save with fallback priority if processing fails
                try {
                    val keywordRules = preferencesManager.getKeywordRules()
                    val appRules = preferencesManager.getAppRules()
                    val fullText = if (title.isNotEmpty() && message.isNotEmpty()) {
                        "$title: $message"
                    } else {
                        title.ifEmpty { message }
                    }
                    
                    val calculatedPriority = calculatePriority(
                        text = fullText,
                        appPackage = sbn.packageName,
                        appName = appName,
                        keywordRules = keywordRules,
                        appRules = appRules,
                        basePriority = basePriority
                    )
                    
                    val notificationModel = com.example.glyph_glance.data.models.Notification(
                        title = title,
                        message = message,
                        timestamp = sbn.postTime,
                        priority = calculatedPriority,
                        appPackage = sbn.packageName,
                        appName = appName
                    )
                    repository.insertNotification(notificationModel)
                } catch (saveError: Exception) {
                    Log.e(TAG, "Error saving notification", saveError)
                    LiveLogger.logError("NotificationListener", saveError.message ?: "Error saving notification")
                }
            }
        }
    }
    
    private fun handleGlyphPattern(pattern: GlyphPattern) {
        // Log glyph interaction
        LiveLogger.logGlyphInteraction(pattern.name, pattern != GlyphPattern.NONE)
        
        // TODO: Implement actual glyph light control based on pattern
        when (pattern) {
            GlyphPattern.URGENT -> {
                Log.d(TAG, "Triggering URGENT glyph pattern")
                // Trigger urgent/strobe pattern
            }
            GlyphPattern.AMBER_BREATHE -> {
                Log.d(TAG, "Triggering AMBER_BREATHE glyph pattern")
                // Trigger amber breathing pattern
            }
            GlyphPattern.NONE -> {
                // No pattern needed
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: Handle notification removal
        Log.d(TAG, "Notification removed: ${sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        notificationDatabase.close()
        
        // Log service stop
        LiveLogger.setServiceRunning(false)
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
