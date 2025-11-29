package com.example.glyph_glance.logic

import com.example.glyph_glance.service.LiveLogger

/**
 * Mock implementation of IntelligenceEngine for testing Track 2 independently.
 * This allows Track 2 to work without Track 1 being fully implemented.
 * 
 * Uses tiered keyword detection to simulate priority-based pattern selection:
 * - HIGH_STROBE: urgent, emergency, critical
 * - MEDIUM_PULSE: important, asap, soon
 * - LOW_SUBTLE: fyi, update, reminder
 */
class MockIntelligenceEngine : IntelligenceEngine {
    
    private val highPriorityKeywords = listOf("urgent", "emergency", "critical", "911", "help")
    private val mediumPriorityKeywords = listOf("important", "asap", "soon", "need", "please")
    private val lowPriorityKeywords = listOf("fyi", "update", "reminder", "info", "note")
    
    override suspend fun processNotification(text: String, senderId: String): DecisionResult {
        LiveLogger.addLog("MOCK IntelligenceEngine: Processing notification from $senderId")
        
        val pattern = determinePattern(text)
        val patternName = pattern.name
        
        return if (pattern != GlyphPattern.NONE) {
            LiveLogger.addLog("MOCK: Detected pattern $patternName for text")
            DecisionResult(shouldLightUp = true, pattern = pattern)
        } else {
            LiveLogger.addLog("MOCK: No priority keywords detected - no glyph trigger")
            DecisionResult(shouldLightUp = false, pattern = GlyphPattern.NONE)
        }
    }
    
    private fun determinePattern(text: String): GlyphPattern {
        return when {
            highPriorityKeywords.any { text.contains(it, ignoreCase = true) } -> GlyphPattern.HIGH_STROBE
            mediumPriorityKeywords.any { text.contains(it, ignoreCase = true) } -> GlyphPattern.MEDIUM_PULSE
            lowPriorityKeywords.any { text.contains(it, ignoreCase = true) } -> GlyphPattern.LOW_SUBTLE
            else -> GlyphPattern.NONE
        }
    }
}

