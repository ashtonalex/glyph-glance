package com.example.glyph_glance.service

import com.example.glyph_glance.logic.IntelligenceEngine
import com.example.glyph_glance.hardware.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Buffer engine that handles split-text message buffering.
 * Implements the logic to buffer messages from the same sender and flush them
 * after a timeout or when the buffer reaches a certain size.
 */
class BufferEngine(
    private val intelligenceEngine: IntelligenceEngine,
    private val glyphManager: GlyphManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val buffers = mutableMapOf<String, SenderBuffer>()
    
    companion object {
        private const val FLUSH_TIMEOUT_MS = 20_000L // 20 seconds
        private const val MAX_BUFFER_SIZE = 5 // Immediate flush if buffer reaches this size
    }

    /**
     * Handle an incoming notification message.
     * Adds it to the buffer and manages the flush timer.
     */
    fun handleIncoming(senderId: String, text: String) {
        // 1. Check if we already have an active buffer for this sender
        val buffer = buffers.getOrPut(senderId) { SenderBuffer() }
        
        // 2. Add text to queue
        buffer.messages.add(text)
        LiveLogger.addLog("Buffer: Added message from $senderId (queue size: ${buffer.messages.size})")
        
        // 3. Reset/Start Flush Timer
        buffer.flushJob?.cancel()
        buffer.flushJob = scope.launch {
            delay(FLUSH_TIMEOUT_MS)
            flush(senderId)
        }
        
        // 4. Immediate Flush Override (e.g. queue too long)
        if (buffer.messages.size >= MAX_BUFFER_SIZE) {
            buffer.flushJob?.cancel()
            scope.launch {
                flush(senderId)
            }
        }
    }

    /**
     * Flush the buffer for a specific sender.
     * Combines all messages and sends them to the IntelligenceEngine.
     */
    private suspend fun flush(senderId: String) {
        val buffer = buffers[senderId] ?: return
        val messages = buffer.messages.toList() // Create a copy
        buffer.messages.clear()
        buffer.flushJob = null
        
        if (messages.isEmpty()) {
            buffers.remove(senderId)
            return
        }
        
        val combinedText = messages.joinToString("\n")
        LiveLogger.addLog("Flushing Buffer for $senderId: ${combinedText.take(50)}...")
        
        // CALL TRACK 1 (THE BRAIN)
        val decision = intelligenceEngine.processNotification(combinedText, senderId)
        
        // EXECUTE DECISION
        if (decision.shouldLightUp) {
            glyphManager.triggerPattern(decision.pattern)
            LiveLogger.addLog("Glyph triggered: ${decision.pattern}")
        } else {
            LiveLogger.addLog("No glyph trigger (decision: ${decision.pattern})")
        }
        
        // Clean up empty buffers
        if (buffer.messages.isEmpty()) {
            buffers.remove(senderId)
        }
    }
    
    /**
     * Data class to hold buffer state for each sender.
     */
    data class SenderBuffer(
        val messages: MutableList<String> = mutableListOf(),
        var flushJob: Job? = null
    )
}

