package com.example.glyph_glance.logic

import com.example.glyph_glance.data.models.SemanticMatchResult
import com.example.glyph_glance.database.ContactDao
import com.example.glyph_glance.database.ContactProfile
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.database.RuleDao
import com.example.glyph_glance.logging.LiveLogger
import kotlinx.datetime.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

class GlyphIntelligenceEngine(
    private val sentimentAnalysisService: SentimentAnalysisService,
    private val semanticMatcher: SemanticMatcher,
    private val ruleDao: RuleDao,
    private val contactDao: ContactDao
) : IntelligenceEngine {

    private val bufferMutex = Mutex()
    private val messageBuffers = mutableMapOf<String, MutableList<String>>()
    private val activeJobs = mutableMapOf<String, Job>()

    override suspend fun processNotification(
        text: String, 
        senderId: String,
        userKeywords: List<String>
    ): DecisionResult = coroutineScope {
        val jobToWait = bufferMutex.withLock {
            activeJobs[senderId]?.cancel()
            
            val buffer = messageBuffers.getOrPut(senderId) { mutableListOf() }
            buffer.add(text)
            
            // Log actual buffer size
            LiveLogger.logBufferQueue(
                senderId = senderId,
                action = "Buffered message",
                queueSize = buffer.size
            )
            
            val newJob = launch {
                delay(5000)
            }
            activeJobs[senderId] = newJob
            newJob
        }

        try {
            jobToWait.join()
        } catch (e: CancellationException) {
            // Job cancelled, handled by isCancelled check
        }

        if (jobToWait.isCancelled) {
             return@coroutineScope DecisionResult(
                shouldLightUp = false,
                pattern = GlyphPattern.NONE,
                urgencyScore = 0,
                sentiment = "NEUTRAL",
                rawAiResponse = "Buffered (Merged into newer notification)",
                triggeredRuleId = null,
                semanticMatched = false,
                semanticCategories = emptySet(),
                semanticBoost = 0,
                isBuffered = true
            )
        }

        val combinedText = bufferMutex.withLock {
            val buffer = messageBuffers[senderId] ?: return@withLock ""
            val size = buffer.size
            val batchText = buffer.joinToString("\n")
            messageBuffers.remove(senderId)
            activeJobs.remove(senderId)
            
            LiveLogger.logBufferQueue(
                senderId = senderId,
                action = "Processing batch",
                queueSize = size
            )
            
            batchText
        }
        
        if (combinedText.isEmpty()) {
             return@coroutineScope DecisionResult(
                shouldLightUp = false,
                pattern = GlyphPattern.NONE,
                urgencyScore = 0,
                sentiment = "NEUTRAL",
                rawAiResponse = "Empty Batch",
                triggeredRuleId = null,
                semanticMatched = false,
                semanticCategories = emptySet(),
                semanticBoost = 0
            )
        }

        processBatch(combinedText, senderId, userKeywords)
    }

    private suspend fun processBatch(
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
        updateContactStats(profile)

        // 2. Check Semantic Keywords FIRST - if urgency 6 found, skip AI for instant priority
        // User-defined keywords take priority over pre-defined semantics
        val semanticResult = semanticMatcher.match(text, userKeywords)
        val semanticUrgency = if (semanticResult.matched) semanticResult.highestLevel else 0
        
        // If semantic/keyword match gives us max urgency (6), skip AI processing entirely
        val skipAiProcessing = semanticUrgency >= 6
        
        val aiUrgencyScore: Int
        val sentiment: String
        val rawAiResponse: String?
        
        if (skipAiProcessing) {
            // Instant priority - skip AI analysis for performance
            aiUrgencyScore = 6
            sentiment = "URGENT"
            rawAiResponse = "[Urgency 6 keyword detected - AI analysis skipped for instant priority]"
        } else {
            // 3. AI Analysis using SentimentAnalysisService
            val analysisResult = sentimentAnalysisService.analyzeNotification(text, senderId)
            aiUrgencyScore = analysisResult?.urgencyScore ?: 3
            sentiment = analysisResult?.sentiment ?: "NEUTRAL"
            rawAiResponse = analysisResult?.rawAiResponse
        }
        
        // 4. Calculate final score (max of AI and semantic)
        val finalUrgencyScore = if (semanticResult.matched) {
            maxOf(aiUrgencyScore, semanticResult.highestLevel)
        } else {
            aiUrgencyScore
        }

        // 5. Rule Matching (database rules)
        val activeRules = ruleDao.getAllRules()
        val triggeredRule = matchRules(text, senderId, activeRules)

        // 4. Determine Output based on urgency score and sentiment
        // Priority: Triggered rules > High urgency > Sentiment > Lower urgency
        val pattern = when {
            triggeredRule != null -> {
                // Triggered rules always get high priority
                GlyphPattern.HIGH_STROBE
            }
            aiResult.urgencyScore >= 4 -> {
                // High urgency takes precedence over sentiment
                GlyphPattern.HIGH_STROBE
            }
            aiResult.sentiment == "NEGATIVE" -> {
                // Negative sentiment triggers amber breathe for non-urgent messages
                GlyphPattern.AMBER_BREATHE
            }
            aiResult.sentiment == "POSITIVE" -> {
                // Positive sentiment triggers gentle glow for non-urgent messages
                GlyphPattern.POSITIVE_GLOW
            }
            else -> {
                // Fall back to urgency-based pattern for neutral sentiment
                urgencyToPattern(aiResult.urgencyScore)
            }
        }
        
        return DecisionResult(
            shouldLightUp = pattern != GlyphPattern.NONE,
            pattern = pattern
        )
    }
    
    /**
     * Maps AI urgency scores to proportionate Glyph patterns.
     * - Score >= 4: HIGH_STROBE (aggressive all-channel strobe)
     * - Score >= 2: MEDIUM_PULSE (moderate dual-channel pulse)
     * - Score >= 1: LOW_SUBTLE (gentle single-channel glow)
     * - Score < 1: NONE (no notification)
     */
    private fun urgencyToPattern(score: Int): GlyphPattern = when {
        score >= 4 -> GlyphPattern.HIGH_STROBE
        score >= 2 -> GlyphPattern.MEDIUM_PULSE
        score >= 1 -> GlyphPattern.LOW_SUBTLE
        else -> GlyphPattern.NONE
        // 6. Determine Glyph Pattern based on urgency and rules
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
            finalScore = finalUrgencyScore,
            aiSkipped = skipAiProcessing
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
        finalScore: Int,
        aiSkipped: Boolean = false
    ): String {
        val sb = StringBuilder()
        
        // AI Response section
        if (rawAiResponse != null) {
            sb.appendLine("=== AI Analysis ===")
            sb.appendLine(rawAiResponse)
            if (!aiSkipped) {
                sb.appendLine("AI Urgency Score: $aiScore")
            }
            sb.appendLine()
        }
        
        // Semantic matching section
        if (semanticResult.matched) {
            sb.appendLine("=== Semantic Keywords Matched ===")
            semanticResult.matchedKeywords.forEach { keyword ->
                sb.appendLine("• \"${keyword.text}\" → Level ${keyword.level} (${keyword.category})")
            }
            sb.appendLine("Semantic Highest Level: ${semanticResult.highestLevel}")
            if (aiSkipped) {
                sb.appendLine("⚡ Instant Priority: Level 6 keyword detected, AI skipped")
            }
            sb.appendLine()
        }
        
        // Final score
        if (semanticResult.matched && finalScore != aiScore && !aiSkipped) {
            sb.appendLine("=== Final Score ===")
            sb.appendLine("Adjusted from $aiScore → $finalScore (semantic boost)")
        }
        
        return if (sb.isEmpty()) rawAiResponse ?: "" else sb.toString().trim()
    }

    private suspend fun updateContactStats(profile: ContactProfile) {
        // Update with current timestamp
        val newProfile = profile.copy(
            lastMessageTimestamp = Clock.System.now().toEpochMilliseconds()
        )
        contactDao.update(newProfile)
    }

    private fun matchRules(text: String, sender: String, rules: List<Rule>): Rule? {
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
