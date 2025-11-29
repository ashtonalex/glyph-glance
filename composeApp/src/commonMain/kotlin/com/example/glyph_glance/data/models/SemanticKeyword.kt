package com.example.glyph_glance.data.models

import kotlinx.serialization.Serializable

/**
 * A semantic keyword or phrase that affects notification urgency scoring.
 * 
 * @property text The keyword or phrase to match (case-insensitive)
 * @property level The urgency level (1-6) to apply when matched
 * @property category The category of this semantic (e.g., "safety", "priority", "scheduling")
 */
@Serializable
data class SemanticKeyword(
    val text: String,
    val level: Int,
    val category: String
)

/**
 * Container for the semantic keywords JSON file.
 */
@Serializable
data class SemanticKeywordsFile(
    val version: Int = 1,
    val description: String = "",
    val semantics: List<SemanticKeyword> = emptyList()
)

/**
 * Result of semantic matching against notification text.
 * 
 * @property matched Whether any semantic keywords were matched
 * @property highestLevel The highest urgency level among matched keywords (1-6)
 * @property matchedKeywords List of all matched semantic keywords
 * @property categories Set of categories that were matched
 */
data class SemanticMatchResult(
    val matched: Boolean,
    val highestLevel: Int,
    val matchedKeywords: List<SemanticKeyword>,
    val categories: Set<String>
) {
    companion object {
        val NONE = SemanticMatchResult(
            matched = false,
            highestLevel = 0,
            matchedKeywords = emptyList(),
            categories = emptySet()
        )
    }
}

