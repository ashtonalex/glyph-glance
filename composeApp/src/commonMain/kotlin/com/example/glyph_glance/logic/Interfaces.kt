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
    val pattern: GlyphPattern
)

/**
 * Glyph light patterns mapped to notification priority levels and sentiment.
 */
enum class GlyphPattern {
    HIGH_STROBE,      // Aggressive all-channel strobe (urgent/high priority)
    MEDIUM_PULSE,     // Moderate pulsing effect (medium priority)
    LOW_SUBTLE,       // Gentle single-channel glow (low priority)
    AMBER_BREATHE,    // Breathing animation pattern (negative sentiment)
    POSITIVE_GLOW,    // Gentle warm glow (positive sentiment)
    NONE              // Off
}
