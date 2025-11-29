package com.example.glyph_glance.logic

import com.example.glyph_glance.database.Rule
import kotlinx.coroutines.flow.Flow

// Consumed by Track 2 (Background Service)
interface IntelligenceEngine {
    suspend fun processNotification(text: String, senderId: String): DecisionResult
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
    val triggeredRuleId: Int? = null
)

enum class GlyphPattern {
    URGENT,
    AMBER_BREATHE,
    NONE
}
