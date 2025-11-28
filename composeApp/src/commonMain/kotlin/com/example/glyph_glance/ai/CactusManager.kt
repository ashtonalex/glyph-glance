package com.example.glyph_glance.ai

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.ChatMessage
import com.cactus.CactusCompletionParams
import kotlinx.serialization.json.Json

class CactusManager {
    
    // Toggle this to FALSE when testing on a real device with the model downloaded
    private val USE_MOCK = true // Set to true to avoid Cactus SDK initialization issues during development

    private val cactusLM = CactusLM()
    private val modelSlug = "qwen3-0.6"

    suspend fun initialize() {
        if (!USE_MOCK) {
            cactusLM.downloadModel(modelSlug)
            cactusLM.initializeModel(CactusInitParams(model = modelSlug, contextSize = 2048))
        }
    }

    suspend fun analyzeText(text: String, historyContext: String?): AIResult {
        if (USE_MOCK) return mockAnalysis(text)
        
        val prompt = "Analyze the urgency (1-5) and sentiment (POSITIVE, NEGATIVE, NEUTRAL) of this text. Return ONLY JSON: {\"urgencyScore\": <int>, \"sentiment\": <string>}. Text: $text"
        
        val result = cactusLM.generateCompletion(
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            params = CactusCompletionParams(maxTokens = 100)
        )

        return if (result?.success == true) {
            try {
                val jsonString = result.response?.trim() ?: ""
                // Simple parsing or use kotlinx.serialization if response is clean JSON
                // For robustness, let's assume the model returns valid JSON or we might need regex
                // Here we'll do a basic manual parse or use a helper
                parseAIResult(jsonString)
            } catch (e: Exception) {
                AIResult(3, "NEUTRAL") // Fallback
            }
        } else {
            AIResult(1, "NEUTRAL") // Fail safe
        }
    }
    
    suspend fun translateRule(userInput: String): String {
        if (USE_MOCK) return "{}"

        val prompt = "You are a config parser. Convert '$userInput' into valid JSON schema: { \"whitelist_keywords\": [strings], \"whitelist_contacts\": [strings], \"min_urgency\": \"LOW\"|\"MEDIUM\"|\"HIGH\" }. Return ONLY JSON."
        
        val result = cactusLM.generateCompletion(
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            params = CactusCompletionParams(maxTokens = 200)
        )
        
        return result?.response ?: "{}"
    }

    private fun mockAnalysis(text: String): AIResult {
        // Simple logic for testing without model
        val urgency = if (text.contains("urgent", ignoreCase = true)) 5 else 1
        val sentiment = if (text.contains("happy", ignoreCase = true)) "POSITIVE" else "NEUTRAL"
        return AIResult(urgency, sentiment)
    }

    private fun parseAIResult(json: String): AIResult {
        // In a real app, use kotlinx.serialization
        // This is a quick hacky parser for the example
        val urgency = if (json.contains("5")) 5 else if (json.contains("4")) 4 else 1
        val sentiment = if (json.contains("POSITIVE")) "POSITIVE" else if (json.contains("NEGATIVE")) "NEGATIVE" else "NEUTRAL"
        return AIResult(urgency, sentiment)
    }
    
    fun unload() {
        cactusLM.unload()
    }
}

data class AIResult(val urgencyScore: Int, val sentiment: String)
