package com.example.glyph_glance.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Centralized logging system for the Glyph Glance app.
 * Collects logs from notification listener, AI service, buffer queue, and glyph hardware.
 */
object LiveLogger {
    
    private const val MAX_LOGS = 500
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    // Status tracking
    private val _cactusInitialized = MutableStateFlow(false)
    val cactusInitialized: StateFlow<Boolean> = _cactusInitialized.asStateFlow()
    
    private val _modelDownloaded = MutableStateFlow(false)
    val modelDownloaded: StateFlow<Boolean> = _modelDownloaded.asStateFlow()
    
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()
    
    private val _notificationAccessEnabled = MutableStateFlow(false)
    val notificationAccessEnabled: StateFlow<Boolean> = _notificationAccessEnabled.asStateFlow()
    
    /**
     * Add a log entry
     */
    fun addLog(type: LogType, message: String, status: LogStatus = LogStatus.INFO, details: String? = null) {
        val entry = LogEntry(
            type = type,
            message = message,
            status = status,
            details = details
        )
        _logs.update { currentLogs ->
            (listOf(entry) + currentLogs).take(MAX_LOGS)
        }
        
        // Also print to console for debugging
        println("LiveLogger [${type.name}] $message ${details?.let { "- $it" } ?: ""}")
    }
    
    /**
     * Update Cactus initialization status
     */
    fun setCactusInitialized(initialized: Boolean) {
        _cactusInitialized.value = initialized
        addLog(
            type = LogType.CACTUS_INIT,
            message = if (initialized) "Cactus LM initialized successfully" else "Cactus LM not initialized",
            status = if (initialized) LogStatus.SUCCESS else LogStatus.PENDING
        )
    }
    
    /**
     * Update model download status
     */
    fun setModelDownloaded(downloaded: Boolean) {
        _modelDownloaded.value = downloaded
        addLog(
            type = LogType.MODEL_DOWNLOAD,
            message = if (downloaded) "Model downloaded successfully" else "Model download pending",
            status = if (downloaded) LogStatus.SUCCESS else LogStatus.PENDING
        )
    }
    
    /**
     * Update service running status
     */
    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        addLog(
            type = LogType.SERVICE_STATUS,
            message = if (running) "Notification listener service started" else "Notification listener service stopped",
            status = if (running) LogStatus.SUCCESS else LogStatus.WARNING
        )
    }
    
    /**
     * Update notification access permission status
     */
    fun setNotificationAccessEnabled(enabled: Boolean) {
        _notificationAccessEnabled.value = enabled
        if (!enabled) {
            addLog(
                type = LogType.SERVICE_STATUS,
                message = "Notification access not granted - please enable in Settings",
                status = LogStatus.WARNING
            )
        }
    }
    
    /**
     * Log a notification received event
     */
    fun logNotificationReceived(appName: String, title: String, preview: String) {
        addLog(
            type = LogType.NOTIFICATION_RECEIVED,
            message = "Notification from $appName",
            status = LogStatus.INFO,
            details = "$title: ${preview.take(50)}${if (preview.length > 50) "..." else ""}"
        )
    }
    
    /**
     * Log buffer queue activity
     */
    fun logBufferQueue(senderId: String, action: String, queueSize: Int) {
        addLog(
            type = LogType.BUFFER_QUEUE,
            message = "$action for $senderId",
            status = LogStatus.INFO,
            details = "Queue size: $queueSize"
        )
    }
    
    /**
     * Log glyph interaction
     */
    fun logGlyphInteraction(pattern: String, triggered: Boolean) {
        addLog(
            type = LogType.GLYPH_INTERACTION,
            message = if (triggered) "Glyph pattern triggered: $pattern" else "No glyph pattern needed",
            status = if (triggered) LogStatus.SUCCESS else LogStatus.INFO
        )
    }
    
    /**
     * Log AI response
     */
    fun logAIResponse(urgencyScore: Int, sentiment: String, processingTime: Long? = null) {
        addLog(
            type = LogType.AI_RESPONSE,
            message = "AI Analysis: Urgency $urgencyScore/6, Sentiment: $sentiment",
            status = LogStatus.SUCCESS,
            details = processingTime?.let { "Processing time: ${it}ms" }
        )
    }
    
    /**
     * Log an error
     */
    fun logError(source: String, error: String) {
        addLog(
            type = LogType.ERROR,
            message = "Error in $source",
            status = LogStatus.ERROR,
            details = error
        )
    }
    
    /**
     * Clear all logs
     */
    fun clear() {
        _logs.value = emptyList()
    }
    
    /**
     * Get logs filtered by type
     */
    fun getLogsByType(type: LogType): List<LogEntry> {
        return _logs.value.filter { it.type == type }
    }
}

