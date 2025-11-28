package com.example.glyph_glance.logic

import com.example.glyph_glance.ai.AIResult
import com.example.glyph_glance.ai.CactusManager
import com.example.glyph_glance.database.ContactDao
import com.example.glyph_glance.database.ContactProfile
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.database.RuleDao
import kotlinx.datetime.Clock

class GlyphIntelligenceEngine(
    private val cactusManager: CactusManager,
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
        
        // (Note: Track 2 handles the buffering, but you handle the profile update)
        updateContactStats(profile, senderId)

        // 2. AI Analysis
        val aiResult = cactusManager.analyzeText(text, null)

        // 3. Rule Matching
        val activeRules = ruleDao.getAllRules()
        val triggeredRule = matchRules(aiResult, text, senderId, activeRules)

        // 4. Determine Output
        return if (triggeredRule != null || aiResult.urgencyScore >= 4) {
             DecisionResult(shouldLightUp = true, pattern = GlyphPattern.URGENT)
        } else {
             DecisionResult(shouldLightUp = false, pattern = GlyphPattern.NONE)
        }
    }

    private suspend fun updateContactStats(profile: ContactProfile, senderId: String) {
        // Simple update logic
        val newProfile = profile.copy(lastMessageTimestamp = 0L) // TODO: Fix timestamp logic
        contactDao.update(newProfile)
    }

    private fun matchRules(ai: AIResult, text: String, sender: String, rules: List<Rule>): Rule? {
        // Iterate through JSON rules and see if conditions are met
        // Return the first matching rule or null
        // Basic implementation for now
        return rules.firstOrNull { rule ->
            // Very simple check: if rule instruction is contained in text (this is a placeholder for real JSON logic)
            // In reality, we would parse rule.jsonLogic and check against ai result
            text.contains(rule.rawInstruction, ignoreCase = true)
        }
    }
}
