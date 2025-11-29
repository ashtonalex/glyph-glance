package com.example.glyph_glance.ai

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.ChatMessage
import com.cactus.CactusCompletionParams
import com.example.glyph_glance.service.LiveLogger
import kotlinx.serialization.json.Json

class CactusManager {

    private val cactusLM = CactusLM()
    private val modelSlug = "qwen3-0.6"
    private var isInitialized = false
    private var isModelDownloaded = false

    suspend fun initialize() {
        LiveLogger.addLog("CACTUS: Initialization started")
        
        try {
            // Step 1: Download model
            LiveLogger.addLog("CACTUS: Starting model download: $modelSlug")
            val downloadStartTime = System.currentTimeMillis()
            
            try {
                cactusLM.downloadModel(modelSlug)
                val downloadTime = System.currentTimeMillis() - downloadStartTime
                isModelDownloaded = true
                LiveLogger.addLog("CACTUS: Model download SUCCESS (took ${downloadTime}ms)")
            } catch (e: Exception) {
                val downloadTime = System.currentTimeMillis() - downloadStartTime
                LiveLogger.addLog("CACTUS: ERROR - Model download FAILED (took ${downloadTime}ms) - ${e.message}")
                throw e // Re-throw to outer catch block
            }
            
            // Step 2: Initialize model
            LiveLogger.addLog("CACTUS: Initializing model: $modelSlug (contextSize=2048)")
            val initStartTime = System.currentTimeMillis()
            
            try {
                cactusLM.initializeModel(
                    CactusInitParams(model = modelSlug, contextSize = 2048)
                )
                val initTime = System.currentTimeMillis() - initStartTime
                isInitialized = true
                LiveLogger.addLog("CACTUS: Model initialization SUCCESS (took ${initTime}ms)")
                LiveLogger.addLog("CACTUS: Ready for inference - LLM mode ACTIVE")
            } catch (e: Exception) {
                val initTime = System.currentTimeMillis() - initStartTime
                LiveLogger.addLog("CACTUS: ERROR - Model initialization FAILED (took ${initTime}ms) - ${e.message}")
                throw e // Re-throw to outer catch block
            }
            
        } catch (e: Exception) {
            LiveLogger.addLog("CACTUS: ERROR during initialization - ${e.message}")
            LiveLogger.addLog("CACTUS: Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }
    }

    suspend fun analyzeText(text: String, historyContext: String?): AIResult {
        if (!isInitialized) {
            LiveLogger.addLog("CACTUS: WARNING - analyzeText() called but model not initialized, using fallback")
            return AIResult(1, "NEUTRAL")
        }
        
        LiveLogger.addLog("CACTUS: analyzeText() called - using LLM")
        LiveLogger.addLog("CACTUS: Input text: ${text.take(100)}${if (text.length > 100) "..." else ""}")
        
        val prompt = "Analyze the urgency (1-5) and sentiment (POSITIVE, NEGATIVE, NEUTRAL) of this text. Return ONLY JSON: {\"urgencyScore\": <int>, \"sentiment\": <string>}. Text: $text"
        
        try {
            val inferenceStartTime = System.currentTimeMillis()
            
            val result = cactusLM.generateCompletion(
                messages = listOf(ChatMessage(role = "user", content = prompt)),
                params = CactusCompletionParams(maxTokens = 100)
            )
            
            val inferenceTime = System.currentTimeMillis() - inferenceStartTime
            LiveLogger.addLog("CACTUS: Inference completed in ${inferenceTime}ms")

            return if (result?.success == true) {
                val jsonString = result.response?.trim() ?: ""
                LiveLogger.addLog("CACTUS: LLM response received: ${jsonString.take(200)}")
                
                try {
                    val parsed = parseAIResult(jsonString)
                    LiveLogger.addLog("CACTUS: Parsed result - urgency=${parsed.urgencyScore}, sentiment=${parsed.sentiment}")
                    parsed
                } catch (e: Exception) {
                    LiveLogger.addLog("CACTUS: ERROR parsing response - ${e.message}, using fallback")
                    AIResult(3, "NEUTRAL") // Fallback
                }
            } else {
                LiveLogger.addLog("CACTUS: ERROR - generateCompletion failed (success=false), using fail-safe")
                AIResult(1, "NEUTRAL") // Fail safe
            }
        } catch (e: Exception) {
            LiveLogger.addLog("CACTUS: EXCEPTION during analyzeText - ${e.message}")
            LiveLogger.addLog("CACTUS: Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            return AIResult(1, "NEUTRAL")
        }
    }
    
    suspend fun translateRule(userInput: String): String {
        if (!isInitialized) {
            LiveLogger.addLog("CACTUS: WARNING - translateRule() called but model not initialized, returning empty JSON")
            return "{}"
        }
        
        LiveLogger.addLog("CACTUS: translateRule() called - converting: '$userInput'")
        
        val prompt = "You are a config parser. Convert '$userInput' into valid JSON schema: { \"whitelist_keywords\": [strings], \"whitelist_contacts\": [strings], \"min_urgency\": \"LOW\"|\"MEDIUM\"|\"HIGH\" }. Return ONLY JSON."
        
        try {
            val result = cactusLM.generateCompletion(
                messages = listOf(ChatMessage(role = "user", content = prompt)),
                params = CactusCompletionParams(maxTokens = 200)
            )
            
            val response = result?.response ?: "{}"
            LiveLogger.addLog("CACTUS: translateRule response: ${response.take(200)}")
            return response
        } catch (e: Exception) {
            LiveLogger.addLog("CACTUS: ERROR in translateRule - ${e.message}, returning empty JSON")
            return "{}"
        }
    }
    
    /**
     * Get the current status of Cactus initialization.
     * Useful for debugging and UI status displays.
     */
    fun getStatus(): CactusStatus {
        return CactusStatus(
            isInitialized = isInitialized,
            isModelDownloaded = isModelDownloaded,
            modelSlug = modelSlug
        )
    }

    private fun parseAIResult(json: String): AIResult {
        // In a real app, use kotlinx.serialization
        // This is a quick hacky parser for the example
        val urgency = if (json.contains("5")) 5 else if (json.contains("4")) 4 else 1
        val sentiment = if (json.contains("POSITIVE")) "POSITIVE" else if (json.contains("NEGATIVE")) "NEGATIVE" else "NEUTRAL"
        return AIResult(urgency, sentiment)
    }
    
    fun unload() {
        LiveLogger.addLog("CACTUS: Unloading model")
        try {
            cactusLM.unload()
            isInitialized = false
            isModelDownloaded = false
            LiveLogger.addLog("CACTUS: Model unloaded successfully")
        } catch (e: Exception) {
            LiveLogger.addLog("CACTUS: ERROR during unload - ${e.message}")
        }
    }
}

data class AIResult(val urgencyScore: Int, val sentiment: String)

/**
 * Status information about Cactus initialization and readiness.
 */
data class CactusStatus(
    val isInitialized: Boolean,
    val isModelDownloaded: Boolean,
    val modelSlug: String
) {
    val isReady: Boolean
        get() = isInitialized && isModelDownloaded
}
