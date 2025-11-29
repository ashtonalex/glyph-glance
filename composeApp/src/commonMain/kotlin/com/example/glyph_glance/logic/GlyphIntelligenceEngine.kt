package com.example.glyph_glance.logic

import com.example.glyph_glance.database.ContactDao
import com.example.glyph_glance.database.ContactProfile
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.database.RuleDao
import kotlinx.datetime.Clock

class GlyphIntelligenceEngine(
    private val sentimentAnalysisService: SentimentAnalysisService,
    private val ruleDao: RuleDao,
    private val contactDao: ContactDao
) : IntelligenceEngine {

    override suspend fun processNotification(text: String, senderId: String): DecisionResult {
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
        val urgencyScore = analysisResult?.urgencyScore ?: 3
        val sentiment = analysisResult?.sentiment ?: "NEUTRAL"
        val rawAiResponse = analysisResult?.rawAiResponse

        // 3. Rule Matching
        val activeRules = ruleDao.getAllRules()
        val triggeredRule = matchRules(urgencyScore, text, senderId, activeRules)

        // 4. Determine Glyph Pattern based on urgency and rules
        val shouldLightUp = triggeredRule != null || urgencyScore >= 4
        val pattern = when {
            urgencyScore >= 5 -> GlyphPattern.URGENT
            urgencyScore >= 4 || triggeredRule != null -> GlyphPattern.AMBER_BREATHE
            else -> GlyphPattern.NONE
        }

        return DecisionResult(
            shouldLightUp = shouldLightUp,
            pattern = pattern,
            urgencyScore = urgencyScore,
            sentiment = sentiment,
            rawAiResponse = rawAiResponse,
            triggeredRuleId = triggeredRule?.id
        )
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
