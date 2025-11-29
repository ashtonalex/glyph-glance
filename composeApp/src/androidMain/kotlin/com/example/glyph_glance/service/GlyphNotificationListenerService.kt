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
import com.example.glyph_glance.logic.DecisionResult
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
    private lateinit var glyphManager: GlyphManager
    
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
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
        
        repository = NotificationRepositoryImpl(notificationDatabase.notificationDao())
        preferencesManager = AndroidPreferencesManager(applicationContext)
        
        // Initialize GlyphManager
        glyphManager = GlyphManager.getInstance(applicationContext)
        glyphManager.initialize()
        
        // Log service start
        LiveLogger.setServiceRunning(true)
        LiveLogger.setNotificationAccessEnabled(true)
        
        Log.d(TAG, "Notification Listener Service Created with GlyphIntelligenceEngine and GlyphManager")
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
        
        // Log buffer queue activity - MOVED to GlyphIntelligenceEngine for accuracy
        // LiveLogger.incrementQueuing() handles the UI spinner state
        LiveLogger.incrementQueuing()
        
        // Process notification through GlyphIntelligenceEngine
        serviceScope.launch {
            try {
                // Build full text for analysis
                val fullText = if (title.isNotEmpty() && message.isNotEmpty()) {
                    "$title: $message"
                } else {
                    title.ifEmpty { message }
                }
                
                // Load user-defined keyword and app rules from preferences
                val keywordRules = preferencesManager.getKeywordRules()
                val appRules = preferencesManager.getAppRules()
                
                // Check if this app is ignored - skip AI analysis and assign urgency 1
                val isAppIgnored = appRules.any { rule ->
                    rule.isIgnored && (
                        rule.appName.equals(appName, ignoreCase = true) ||
                        rule.packageName.equals(sbn.packageName, ignoreCase = true)
                    )
                }
                
                if (isAppIgnored) {
                    // Skip AI analysis for ignored apps - just save with urgency 1
                    Log.d(TAG, "App ignored: $appName - skipping AI, assigning urgency 1")
                    LiveLogger.addLog(
                        type = com.example.glyph_glance.logging.LogType.SERVICE_STATUS,
                        message = "Ignored: $appName (urgency → 1, no AI)",
                        status = com.example.glyph_glance.logging.LogStatus.INFO
                    )
                    
                    val notificationModel = com.example.glyph_glance.data.models.Notification(
                        title = title,
                        message = message,
                        timestamp = sbn.postTime,
                        priority = NotificationPriority.LOW,
                        appPackage = sbn.packageName,
                        appName = appName,
                        sentiment = "NEUTRAL",
                        urgencyScore = 1, // Forced to lowest urgency
                        rawAiResponse = "[App Ignored - AI analysis skipped]"
                    )
                    
                    repository.insertNotification(notificationModel)
                    LiveLogger.logGlyphInteraction("NONE", false) // No glyph for ignored apps
                    LiveLogger.decrementQueuing()
                    return@launch
                }
                
                // Extract keyword strings for semantic exclusion
                // User keywords take priority over pre-defined semantics
                val userKeywords = keywordRules.map { it.keyword }
                
                // Calculate preference priority first to check for urgency 6
                val preferencePriority = calculatePriority(
                    text = fullText,
                    appPackage = sbn.packageName,
                    appName = appName,
                    keywordRules = keywordRules,
                    appRules = appRules,
                    basePriority = basePriority
                )
                val preferenceUrgencyScore = preferencePriority.toUrgencyScore()
                
                // Process decision - skip AI if urgency 6 keyword found
                val finalUrgencyScore: Int
                val finalPriority: NotificationPriority
                val decision: DecisionResult
                
                if (preferenceUrgencyScore >= 6) {
                    // Instant priority - skip AI processing entirely
                    Log.d(TAG, "Urgency 6 keyword detected: $appName - skipping AI for instant priority")
                    
                    // Transition from "Queuing" to "Thinking" (briefly)
                    LiveLogger.decrementQueuing()
                    LiveLogger.setProcessingNotification(true)
                    
                    LiveLogger.addLog(
                        type = com.example.glyph_glance.logging.LogType.AI_RESPONSE,
                        message = "⚡ Instant Priority: $appName (urgency → 6, AI skipped)",
                        status = com.example.glyph_glance.logging.LogStatus.INFO
                    )
                    
                    finalUrgencyScore = 6
                    finalPriority = NotificationPriority.HIGH
                    decision = DecisionResult(
                        shouldLightUp = true,
                        pattern = GlyphPattern.URGENT,
                        urgencyScore = 6,
                        sentiment = "URGENT",
                        rawAiResponse = "[Urgency 6 keyword detected - AI analysis skipped for instant priority]",
                        triggeredRuleId = null,
                        semanticMatched = false,
                        semanticCategories = emptySet(),
                        semanticBoost = 0
                    )
                } else {
                    // Process through GlyphIntelligenceEngine (handles AI analysis, contact profiles, and DB rules)
                    // This will wait for the 5-second buffering window
                    val engineDecision = intelligenceEngine.processNotification(fullText, appName, userKeywords)
                    
                    // If notification was buffered (superseded by a newer one), stop processing here
                    if (engineDecision.isBuffered) {
                        Log.d(TAG, "Notification buffered/superseded: $title from $appName - Skipping save/notify")
                        LiveLogger.decrementQueuing()
                        return@launch
                    }
                    
                    // Now that buffering is complete, transition from "Queuing" to "Thinking"
                    LiveLogger.decrementQueuing()
                    LiveLogger.setProcessingNotification(true)
                    
                    decision = engineDecision
                    
                    // Combine AI urgency with preference rules (take maximum)
                    finalUrgencyScore = maxOf(decision.urgencyScore, preferenceUrgencyScore)
                    finalPriority = NotificationPriority.fromUrgencyScore(finalUrgencyScore)
                }
                
                Log.d(TAG, "Flagging check - LLM Urgency Score: $llmUrgencyScore (used for glyph), " +
                        "PreferenceUrgencyScore: $preferenceUrgencyScore, FinalUrgencyScoreForDb: $finalUrgencyScoreForDb, " +
                        "TriggeredRuleId: ${decision.triggeredRuleId}, FinalShouldLightUp: $shouldLightUp, FinalPattern: $finalPattern")
                
                val notificationModel = com.example.glyph_glance.data.models.Notification(
                    title = title,
                    message = message,
                    timestamp = sbn.postTime,
                    priority = finalPriority,
                    appPackage = sbn.packageName,
                    appName = appName,
                    sentiment = decision.sentiment,
                    urgencyScore = finalUrgencyScoreForDb, // Store combined score in DB
                    rawAiResponse = decision.rawAiResponse
                )
                
                // Save to database
                repository.insertNotification(notificationModel)
                
                // Log with glyph pattern info
                Log.d(TAG, "Saved notification: $title from $appName " +
                        "(Priority: $finalPriority, LLM Urgency: $llmUrgencyScore, DB Urgency: $finalUrgencyScoreForDb, " +
                        "Sentiment: ${decision.sentiment}, Pattern: $finalPattern, ShouldLightUp: $shouldLightUp)")
                
                // Handle glyph pattern ONLY after all processing is complete and message is flagged
                if (shouldLightUp) {
                    handleGlyphPattern(finalPattern)
                } else {
                    LiveLogger.logGlyphInteraction(finalPattern.name, false)
                }
                
                // Processing complete
                LiveLogger.setProcessingNotification(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                LiveLogger.logError("NotificationListener", e.message ?: "Error processing notification")
                LiveLogger.decrementQueuing() // Ensure we decrement on error (in case it wasn't already)
                LiveLogger.setProcessingNotification(false)
                
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
        
        Log.d(TAG, "handleGlyphPattern called with pattern: $pattern")
        
        // Flash glyph 3 times whenever a message is flagged (any pattern except NONE)
        when (pattern) {
            GlyphPattern.URGENT -> {
                Log.d(TAG, "Triggering URGENT glyph pattern - flashing 3 times")
                glyphManager.flashThreeTimes()
            }
            GlyphPattern.AMBER_BREATHE -> {
                Log.d(TAG, "Triggering AMBER_BREATHE glyph pattern - flashing 3 times")
                glyphManager.flashThreeTimes()
            }
            GlyphPattern.NONE -> {
                Log.d(TAG, "No glyph pattern - skipping flash")
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
        
        // Cleanup GlyphManager
        glyphManager.cleanup()
        
        // Log service stop
        LiveLogger.setServiceRunning(false)
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
