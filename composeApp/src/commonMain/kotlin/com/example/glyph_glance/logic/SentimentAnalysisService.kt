package com.example.glyph_glance.logic

import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Result of sentiment analysis for a notification
 */
@Serializable
data class AnalysisResult(
    val urgencyScore: Int, // 1 (Low) to 5 (Critical)
    val sentiment: String, // "POSITIVE", "NEGATIVE", "NEUTRAL"
    val triggeredRuleId: Int? = null // Null if no specific rule matched
)

/**
 * Service for analyzing notification sentiment and urgency using Qwen via Cactus SDK
 */
class SentimentAnalysisService {
    
    private var cactusLM: CactusLM? = null
    private var isInitialized = false
    
    /**
     * Initialize the Cactus LM with Qwen model
     * Should be called once before using the service
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        
        return try {
            val lm = CactusLM()
            
            // Download and initialize Qwen3-0.6B model
            // Note: downloadModel and initializeModel may return Unit or Boolean
            // We'll proceed assuming they succeed unless an exception is thrown
            lm.downloadModel("qwen3-0.6")
            lm.initializeModel(
                CactusInitParams(
                    model = "qwen3-0.6",
                    contextSize = 2048
                )
            )
            
            cactusLM = lm
            isInitialized = true
            true
        } catch (e: Exception) {
            println("Error initializing Cactus LM: ${e.message}")
            false
        }
    }
    
    /**
     * Analyze notification text for sentiment and urgency
     * 
     * @param content The notification message text
     * @param senderId The sender identifier (app name or contact)
     * @return AnalysisResult with urgency score and sentiment, or null if analysis fails
     */
    suspend fun analyzeNotification(
        content: String,
        senderId: String
    ): AnalysisResult? {
        if (!isInitialized) {
            val initSuccess = initialize()
            if (!initSuccess) {
                return getDefaultResult()
            }
        }
        
        val lm = cactusLM ?: return getDefaultResult()
        
        return try {
            // Create system prompt for sentiment and urgency analysis
            val systemPrompt = """
                You are a notification analysis system. Analyze the given notification text and determine:
                1. Urgency level (1-5): 1=Low, 2=Medium-Low, 3=Medium, 4=High, 5=Critical/Emergency
                2. Sentiment: POSITIVE, NEGATIVE, or NEUTRAL
                
                Consider these factors for urgency:
                - Emergency keywords (urgent, emergency, help, ASAP, now, immediately)
                - Time-sensitive content (deadline, meeting, appointment)
                - Emotional intensity (crying, angry, excited)
                - Question marks and urgency indicators
                
                Return ONLY valid JSON in this exact format:
                {
                    "urgencyScore": <number 1-5>,
                    "sentiment": "<POSITIVE|NEGATIVE|NEUTRAL>"
                }
            """.trimIndent()
            
            val userPrompt = """
                Analyze this notification:
                From: $senderId
                Message: $content
            """.trimIndent()
            
            // Generate completion
            val result = lm.generateCompletion(
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                params = CactusCompletionParams(
                    maxTokens = 150,
                    temperature = 0.3 // Lower temperature for more consistent classification
                )
            )
            
            val responseText = result?.response
            if (result?.success == true && responseText != null) {
                parseAnalysisResult(responseText)
            } else {
                println("Failed to get analysis result from Cactus")
                getDefaultResult()
            }
        } catch (e: Exception) {
            println("Error analyzing notification: ${e.message}")
            e.printStackTrace()
            getDefaultResult()
        }
    }
    
    /**
     * Parse the JSON response from Qwen into AnalysisResult
     */
    private fun parseAnalysisResult(response: String): AnalysisResult {
        return try {
            // Try to extract JSON from the response (Qwen might add extra text)
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = response.substring(jsonStart, jsonEnd)
                val json = Json { ignoreUnknownKeys = true }
                val jsonObject = json.parseToJsonElement(jsonString).jsonObject
                
                val urgencyScore = jsonObject["urgencyScore"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3
                val sentiment = jsonObject["sentiment"]?.jsonPrimitive?.content ?: "NEUTRAL"
                
                // Validate and clamp values
                AnalysisResult(
                    urgencyScore = urgencyScore.coerceIn(1, 5),
                    sentiment = sentiment.uppercase().takeIf { 
                        it in listOf("POSITIVE", "NEGATIVE", "NEUTRAL") 
                    } ?: "NEUTRAL"
                )
            } else {
                // Fallback: try to extract values with regex if JSON parsing fails
                extractFromText(response)
            }
        } catch (e: Exception) {
            println("Error parsing analysis result: ${e.message}")
            extractFromText(response)
        }
    }
    
    /**
     * Fallback method to extract sentiment/urgency from text if JSON parsing fails
     */
    private fun extractFromText(text: String): AnalysisResult {
        val upperText = text.uppercase()
        
        // Extract urgency score
        val urgencyRegex = """urgencyScore["\s:]*(\d)""".toRegex(RegexOption.IGNORE_CASE)
        val urgencyMatch = urgencyRegex.find(text)
        val urgencyScore = urgencyMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 5) ?: 3
        
        // Extract sentiment
        val sentiment = when {
            upperText.contains("POSITIVE") -> "POSITIVE"
            upperText.contains("NEGATIVE") -> "NEGATIVE"
            else -> "NEUTRAL"
        }
        
        return AnalysisResult(
            urgencyScore = urgencyScore,
            sentiment = sentiment
        )
    }
    
    /**
     * Get default analysis result when analysis fails
     */
    private fun getDefaultResult(): AnalysisResult {
        return AnalysisResult(
            urgencyScore = 3, // Medium urgency
            sentiment = "NEUTRAL"
        )
    }
    
    /**
     * Clean up resources
     */
    fun unload() {
        cactusLM?.unload()
        cactusLM = null
        isInitialized = false
    }
}

