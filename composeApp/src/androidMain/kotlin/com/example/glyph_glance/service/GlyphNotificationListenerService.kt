package com.example.glyph_glance.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
        
        repository = NotificationRepositoryImpl(notificationDatabase.notificationDao())
        preferencesManager = AndroidPreferencesManager(applicationContext)
        
        // Log service start
        LiveLogger.setServiceRunning(true)
        LiveLogger.setNotificationAccessEnabled(true)
        
        Log.d(TAG, "Notification Listener Service Created with GlyphIntelligenceEngine")
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
                
                val notificationModel = com.example.glyph_glance.data.models.Notification(
                    title = title,
                    message = message,
                    timestamp = sbn.postTime,
                    priority = finalPriority,
                    appPackage = sbn.packageName,
                    appName = appName,
                    sentiment = decision.sentiment,
                    urgencyScore = finalUrgencyScore,
                    rawAiResponse = decision.rawAiResponse
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
        notificationDatabase.close()
        
        // Log service stop
        LiveLogger.setServiceRunning(false)
    }
    
    companion object {
        private const val TAG = "GlyphNotificationListener"
    }
}
