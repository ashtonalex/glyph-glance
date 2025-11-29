package com.example.glyph_glance.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Singleton logger for real-time log streaming to UI.
 * Used by Track 2 (services) to write logs and Track 3 (UI) to observe them.
 */
object LiveLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    private const val MAX_LOGS = 1000
    
    /**
     * Add a log entry. Automatically trims old logs if exceeding MAX_LOGS.
     */
    fun addLog(message: String) {
        val now = Clock.System.now()
        val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = "%02d:%02d:%02d".format(localTime.hour, localTime.minute, localTime.second)
        val logEntry = "[$timestamp] $message"
        
        // Print to system out for debugging
        println("LiveLogger: $message")
        
        val currentLogs = _logs.value
        val newLogs = (currentLogs + logEntry).takeLast(MAX_LOGS)
        _logs.value = newLogs
    }
    
    /**
     * Clear all logs.
     */
    fun clear() {
        _logs.value = emptyList()
    }
}

