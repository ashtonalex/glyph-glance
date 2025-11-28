package com.example.glyph_glance.logic

import com.example.glyph_glance.service.LiveLogger

/**
 * Mock implementation of IntelligenceEngine for testing Track 2 independently.
 * This allows Track 2 to work without Track 1 being fully implemented.
 */
class MockIntelligenceEngine : IntelligenceEngine {
    
    override suspend fun processNotification(text: String, senderId: String): DecisionResult {
        LiveLogger.addLog("MOCK IntelligenceEngine: Processing notification from $senderId")
        
        // Simple mock logic: trigger glyph for urgent keywords
        val urgentKeywords = listOf("urgent", "emergency", "important", "asap", "now")
        val hasUrgentKeyword = urgentKeywords.any { text.contains(it, ignoreCase = true) }
        
        return if (hasUrgentKeyword) {
            LiveLogger.addLog("MOCK: Urgent keyword detected - triggering URGENT pattern")
            DecisionResult(shouldLightUp = true, pattern = GlyphPattern.URGENT)
        } else {
            LiveLogger.addLog("MOCK: No urgent keywords - no glyph trigger")
            DecisionResult(shouldLightUp = false, pattern = GlyphPattern.NONE)
        }
    }
}

