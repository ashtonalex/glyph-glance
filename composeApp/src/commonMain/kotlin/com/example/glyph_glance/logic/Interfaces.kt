package com.example.glyph_glance.logic

import com.example.glyph_glance.database.Rule
import kotlinx.coroutines.flow.Flow

// Consumed by Track 2 (Background Service)
interface IntelligenceEngine {
    /**
     * Process a notification through the intelligence pipeline.
     * 
     * @param text The notification text
     * @param senderId The sender/app identifier
     * @param userKeywords User-defined keywords that take priority over pre-defined semantics
     * @return DecisionResult with urgency score, sentiment, and glyph pattern
     */
    suspend fun processNotification(
        text: String, 
        senderId: String,
        userKeywords: List<String> = emptyList()
    ): DecisionResult
}

// Consumed by Track 3 (UI)
interface RulesRepository {
    suspend fun addNaturalLanguageRule(text: String)
    fun getRulesFlow(): Flow<List<Rule>>
}

data class DecisionResult(
    val shouldLightUp: Boolean,
    val pattern: GlyphPattern,
    // Analysis data from SentimentAnalysisService
    val urgencyScore: Int,
    val sentiment: String,
    val rawAiResponse: String? = null,
    // Rule matching info
    val triggeredRuleId: Int? = null,
    // Semantic keyword matching info
    val semanticMatched: Boolean = false,
    val semanticCategories: Set<String> = emptySet(),
    val semanticBoost: Int = 0, // How much the score was boosted by semantics
    val isBuffered: Boolean = false // True if this result is from a buffered/superseded notification
)

enum class GlyphPattern {
    URGENT,
    AMBER_BREATHE,
    NONE
}
