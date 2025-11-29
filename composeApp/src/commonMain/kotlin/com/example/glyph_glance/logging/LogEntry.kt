package com.example.glyph_glance.logging

import kotlinx.datetime.Clock

/**
 * Types of log entries for the activity logs screen
 */
enum class LogType {
    CACTUS_INIT,
    MODEL_DOWNLOAD,
    NOTIFICATION_RECEIVED,
    BUFFER_QUEUE,
    GLYPH_INTERACTION,
    AI_RESPONSE,
    SERVICE_STATUS,
    ERROR
}

/**
 * Status levels for log entries
 */
enum class LogStatus {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    PENDING
}

/**
 * A single log entry in the activity logs
 */
data class LogEntry(
    val id: Long = Clock.System.now().toEpochMilliseconds(),
    val type: LogType,
    val message: String,
    val status: LogStatus = LogStatus.INFO,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val details: String? = null
)

