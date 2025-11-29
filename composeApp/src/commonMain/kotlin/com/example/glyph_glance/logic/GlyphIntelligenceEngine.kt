package com.example.glyph_glance.logic

import com.example.glyph_glance.data.models.SemanticMatchResult
import com.example.glyph_glance.database.ContactDao
import com.example.glyph_glance.database.ContactProfile
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.database.RuleDao
import kotlinx.datetime.Clock

class GlyphIntelligenceEngine(
    private val sentimentAnalysisService: SentimentAnalysisService,
    private val semanticMatcher: SemanticMatcher,
    private val ruleDao: RuleDao,
    private val contactDao: ContactDao
) : IntelligenceEngine {

    override suspend fun processNotification(
        text: String, 
        senderId: String,
        userKeywords: List<String>
    ): DecisionResult {
        // 1. Fetch Profile & Check "Split Texter" Logic
        var profile = contactDao.getProfile(senderId)
        if (profile == null) {
            profile = ContactProfile(senderId = senderId)
            contactDao.insert(profile)
        }
        
        // Update contact stats with current timestamp
        updateContactStats(profile, senderId)

        // 2. AI Analysis using SentimentAnalysisService
        val analysisResult = sentimentAnalysisService.analyzeNotification(text, senderId)
        val aiUrgencyScore = analysisResult?.urgencyScore ?: 3
        val sentiment = analysisResult?.sentiment ?: "NEUTRAL"
        val rawAiResponse = analysisResult?.rawAiResponse
        
        // 3. Semantic Keyword Matching (boosts urgency based on key phrases)
        // User-defined keywords take priority over pre-defined semantics
        val semanticResult = semanticMatcher.match(text, userKeywords)
        val semanticAdjustedScore = if (semanticResult.matched) {
            maxOf(aiUrgencyScore, semanticResult.highestLevel)
        } else {
            aiUrgencyScore
        }

        // 4. Rule Matching (database rules)
        val activeRules = ruleDao.getAllRules()
        val triggeredRule = matchRules(semanticAdjustedScore, text, senderId, activeRules)
        
        // Final urgency score after all adjustments
        val finalUrgencyScore = semanticAdjustedScore

        // 5. Determine Glyph Pattern based on urgency and rules
        val shouldLightUp = triggeredRule != null || finalUrgencyScore >= 4
        val pattern = when {
            finalUrgencyScore >= 5 -> GlyphPattern.URGENT
            finalUrgencyScore >= 4 || triggeredRule != null -> GlyphPattern.AMBER_BREATHE
            else -> GlyphPattern.NONE
        }
        
        // Build enhanced raw response with semantic match info
        val enhancedRawResponse = buildEnhancedResponse(
            rawAiResponse = rawAiResponse,
            aiScore = aiUrgencyScore,
            semanticResult = semanticResult,
            finalScore = finalUrgencyScore
        )

        return DecisionResult(
            shouldLightUp = shouldLightUp,
            pattern = pattern,
            urgencyScore = finalUrgencyScore,
            sentiment = sentiment,
            rawAiResponse = enhancedRawResponse,
            triggeredRuleId = triggeredRule?.id,
            semanticMatched = semanticResult.matched,
            semanticCategories = semanticResult.categories,
            semanticBoost = if (semanticResult.matched && semanticResult.highestLevel > aiUrgencyScore) 
                semanticResult.highestLevel - aiUrgencyScore else 0
        )
    }
    
    /**
     * Build an enhanced raw response that includes semantic match information.
     */
    private fun buildEnhancedResponse(
        rawAiResponse: String?,
        aiScore: Int,
        semanticResult: SemanticMatchResult,
        finalScore: Int
    ): String {
        val sb = StringBuilder()
        
        // AI Response section
        if (rawAiResponse != null) {
            sb.appendLine("=== AI Analysis ===")
            sb.appendLine(rawAiResponse)
            sb.appendLine("AI Urgency Score: $aiScore")
            sb.appendLine()
        }
        
        // Semantic matching section
        if (semanticResult.matched) {
            sb.appendLine("=== Semantic Keywords Matched ===")
            semanticResult.matchedKeywords.forEach { keyword ->
                sb.appendLine("• \"${keyword.text}\" → Level ${keyword.level} (${keyword.category})")
            }
            sb.appendLine("Semantic Boost: ${semanticResult.highestLevel}")
            sb.appendLine()
        }
        
        // Final score
        if (semanticResult.matched && finalScore != aiScore) {
            sb.appendLine("=== Final Score ===")
            sb.appendLine("Adjusted from $aiScore → $finalScore (semantic boost)")
        }
        
        return if (sb.isEmpty()) rawAiResponse ?: "" else sb.toString().trim()
    }

    private suspend fun updateContactStats(profile: ContactProfile, senderId: String) {
        // Update with current timestamp
        val newProfile = profile.copy(
            lastMessageTimestamp = Clock.System.now().toEpochMilliseconds()
        )
        contactDao.update(newProfile)
    }

    private fun matchRules(urgencyScore: Int, text: String, sender: String, rules: List<Rule>): Rule? {
        // Iterate through rules and see if conditions are met
        // Return the first matching rule or null
        return rules.firstOrNull { rule ->
            // Check if rule instruction is contained in text or sender
            // In reality, we would parse rule.jsonLogic and check against urgency score
            text.contains(rule.rawInstruction, ignoreCase = true) ||
            sender.contains(rule.rawInstruction, ignoreCase = true)
        }
    }
}
