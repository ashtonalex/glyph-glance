package com.example.glyph_glance.logic

import com.example.glyph_glance.data.models.SemanticKeyword
import com.example.glyph_glance.data.models.SemanticKeywordsFile
import com.example.glyph_glance.data.models.SemanticMatchResult
import kotlinx.serialization.json.Json

/**
 * Service for matching notification text against semantic keywords.
 * 
 * Semantic keywords are phrases that indicate specific urgency levels,
 * such as "call 911" (level 6) or "no rush" (level 1).
 * 
 * The matcher performs case-insensitive substring matching.
 */
class SemanticMatcher {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private var semanticKeywords: List<SemanticKeyword> = emptyList()
    private var isLoaded = false
    
    /**
     * Load semantic keywords from JSON string.
     * Call this once during app initialization.
     * 
     * @param jsonString The JSON content from semantic_keywords.json
     * @return true if loaded successfully
     */
    fun loadFromJson(jsonString: String): Boolean {
        return try {
            val file = json.decodeFromString<SemanticKeywordsFile>(jsonString)
            semanticKeywords = file.semantics
            isLoaded = true
            println("SemanticMatcher: Loaded ${semanticKeywords.size} semantic keywords")
            true
        } catch (e: Exception) {
            println("SemanticMatcher: Error loading semantics: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Load semantic keywords directly from a list.
     * Useful for testing or dynamic updates.
     */
    fun loadFromList(keywords: List<SemanticKeyword>) {
        semanticKeywords = keywords
        isLoaded = true
    }
    
    /**
     * Match notification text against all semantic keywords.
     * 
     * User-defined keywords take priority over pre-defined semantics.
     * Any semantic keyword that matches a user keyword will be excluded.
     * 
     * @param text The notification text to analyze
     * @param userKeywordsToExclude List of user-defined keywords that should take priority over semantics
     * @return SemanticMatchResult with all matched keywords and highest level
     */
    fun match(text: String, userKeywordsToExclude: List<String> = emptyList()): SemanticMatchResult {
        if (!isLoaded || semanticKeywords.isEmpty()) {
            return SemanticMatchResult.NONE
        }
        
        val lowerText = text.lowercase()
        val lowerUserKeywords = userKeywordsToExclude.map { it.lowercase() }.toSet()
        val matched = mutableListOf<SemanticKeyword>()
        
        for (keyword in semanticKeywords) {
            val lowerKeyword = keyword.text.lowercase()
            
            // Skip this semantic keyword if it matches any user-defined keyword
            // User keywords take priority over pre-defined semantics
            val isOverriddenByUser = lowerUserKeywords.any { userKeyword ->
                // Check if semantic keyword matches or is contained in user keyword (or vice versa)
                lowerKeyword.contains(userKeyword) || userKeyword.contains(lowerKeyword)
            }
            
            if (isOverriddenByUser) {
                println("SemanticMatcher: Skipping '${keyword.text}' - overridden by user keyword rule")
                continue
            }
            
            if (lowerText.contains(lowerKeyword)) {
                matched.add(keyword)
            }
        }
        
        if (matched.isEmpty()) {
            return SemanticMatchResult.NONE
        }
        
        val highestLevel = matched.maxOf { it.level }
        val categories = matched.map { it.category }.toSet()
        
        println("SemanticMatcher: Matched ${matched.size} keywords in text, highest level: $highestLevel")
        matched.forEach { 
            println("  - '${it.text}' (level ${it.level}, category: ${it.category})")
        }
        
        return SemanticMatchResult(
            matched = true,
            highestLevel = highestLevel,
            matchedKeywords = matched,
            categories = categories
        )
    }
    
    /**
     * Apply semantic matching to adjust an AI urgency score.
     * 
     * The final score is the MAXIMUM of:
     * - The AI-determined urgency score
     * - The highest semantic keyword level
     * 
     * This ensures semantic keywords can boost urgency but never lower it.
     * User-defined keywords take priority over pre-defined semantics.
     * 
     * @param text The notification text to analyze
     * @param aiUrgencyScore The urgency score from AI analysis (1-6)
     * @param userKeywordsToExclude List of user-defined keywords that should take priority
     * @return Adjusted urgency score (1-6)
     */
    fun adjustUrgencyScore(text: String, aiUrgencyScore: Int, userKeywordsToExclude: List<String> = emptyList()): Int {
        val matchResult = match(text, userKeywordsToExclude)
        
        return if (matchResult.matched) {
            val adjusted = maxOf(aiUrgencyScore, matchResult.highestLevel)
            if (adjusted != aiUrgencyScore) {
                println("SemanticMatcher: Adjusted urgency from $aiUrgencyScore to $adjusted (semantic match)")
            }
            adjusted.coerceIn(1, 6)
        } else {
            aiUrgencyScore
        }
    }
    
    /**
     * Get detailed match info for debugging/display purposes.
     * 
     * @param text The notification text
     * @param userKeywordsToExclude User keywords that take priority (will be skipped)
     */
    fun getMatchDetails(text: String, userKeywordsToExclude: List<String> = emptyList()): String {
        val result = match(text, userKeywordsToExclude)
        
        if (!result.matched) {
            return "No semantic keywords matched"
        }
        
        val sb = StringBuilder()
        sb.appendLine("Matched ${result.matchedKeywords.size} semantic keywords:")
        result.matchedKeywords.forEach {
            sb.appendLine("  • \"${it.text}\" → Level ${it.level} (${it.category})")
        }
        sb.appendLine("Highest urgency level: ${result.highestLevel}")
        
        if (userKeywordsToExclude.isNotEmpty()) {
            sb.appendLine("(${userKeywordsToExclude.size} user keywords take priority over semantics)")
        }
        
        return sb.toString()
    }
    
    /**
     * Check if semantics have been loaded.
     */
    fun isReady(): Boolean = isLoaded && semanticKeywords.isNotEmpty()
    
    /**
     * Get all loaded semantic keywords.
     */
    fun getAllKeywords(): List<SemanticKeyword> = semanticKeywords.toList()
    
    /**
     * Get keywords filtered by category.
     */
    fun getKeywordsByCategory(category: String): List<SemanticKeyword> {
        return semanticKeywords.filter { it.category.equals(category, ignoreCase = true) }
    }
    
    /**
     * Get all unique categories.
     */
    fun getCategories(): Set<String> {
        return semanticKeywords.map { it.category }.toSet()
    }
}

