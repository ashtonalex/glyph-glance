package com.example.glyph_glance.logic

import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import kotlinx.serialization.Serializable

/**
 * Result of sentiment analysis for a notification
 */
@Serializable
data class AnalysisResult(
    val urgencyScore: Int, // 1-2 Low, 3-4 Medium, 5-6 High
    val sentiment: String, // "POSITIVE", "NEGATIVE", "NEUTRAL"
    val triggeredRuleId: Int? = null, // Null if no specific rule matched
    val rawAiResponse: String? = null // Raw response from AI for debugging
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
            println("SentimentAnalysisService: Downloading Qwen3-0.6 model...")
            val downloadResult = lm.downloadModel("qwen3-0.6")
            println("SentimentAnalysisService: Download result: $downloadResult")
            
            println("SentimentAnalysisService: Initializing Qwen3-0.6 model...")
            val initResult = lm.initializeModel(
                CactusInitParams(
                    model = "qwen3-0.6",
                    contextSize = 2048
                )
            )
            println("SentimentAnalysisService: Initialization result: $initResult")
            
            cactusLM = lm
            isInitialized = true
            println("SentimentAnalysisService: Qwen3 model initialized successfully")
            true
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error initializing Cactus LM: ${e.message}")
            e.printStackTrace()
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
            val appContext = inferAppContext(senderId)
            // Simplified prompt for faster responses - just need a number
            // Explicitly state this is a standalone request with no previous context
            val systemPrompt = "You are an urgency evaluator. Each request is independent with no conversation history. Output ONLY a number 1-6. No explanation."
            
            val userPrompt = """
                This is a standalone request. Analyze ONLY this notification and its implied context:
                
                Rate urgency 1-6 based on how quickly the message should realistically be addressed:
                1 = Very low priority – casual, informational, or can be safely ignored for a long time
                2 = Low priority – can wait comfortably, no real pressure
                3 = Slightly urgent – should be handled at some point soon
                4 = Moderately urgent – should be handled soon, but not immediately
                5 = Urgent – needs attention as soon as reasonably possible
                6 = Extremely urgent / critical – needs attention right away (\"drop everything\")
                
                When deciding the score, use BOTH the sender/app name and the message content:
                - WHO is involved: e.g. a boss, important client, or doctor is usually more important than a game or casual friend
                - TYPE of sender: real people > automated systems > games/ads
                - SITUATION: emergencies, safety issues, or very time-sensitive coordination (\"come here now\", \"we're starting\", \"I'm in trouble\") are more urgent
                - CONTEXT examples:
                  • Boss asking for something soon → higher urgency than a friend saying the same thing casually
                  • A person telling you to go somewhere soon → higher urgency than a game asking you to log in
                  • Clear emergency or crisis language → very high urgency
                
                Focus on how quickly a reasonable person should respond overall, not just specific keywords.
                
                Inferred app context (best guess based on name, may be wrong): $appContext
                You will receive:
                - Sender/App name: \"$senderId\"
                - Message: \"$content\"
                
                Analyze them together when deciding the urgency score.
                
                Output ONLY the number (1, 2, 3, 4, 5, or 6):
            """.trimIndent()
            
            println("SentimentAnalysisService: Sending prompt to Qwen3 for message: '$content'")
            
            // Generate completion - each call is independent with fresh messages list
            // Create a completely new messages list to ensure no conversation history
            val messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
            
            val result = lm.generateCompletion(
                messages = messages,
                params = CactusCompletionParams(
                    maxTokens = 500, // Allow for full response including any explanation
                    temperature = 0.0 // Zero temperature for fastest, most deterministic output
                )
            )
            
            val responseText = result?.response
            println("SentimentAnalysisService: Qwen3 response - Success: ${result?.success}, Response: '$responseText'")
            
            if (result?.success == true && responseText != null && responseText.isNotEmpty()) {
                parseUrgencyScore(responseText, responseText) // Pass raw response for debugging
            } else {
                println("SentimentAnalysisService: Failed to get analysis result from Cactus")
                getDefaultResult()
            }
        } catch (e: Exception) {
            println("Error analyzing notification: ${e.message}")
            e.printStackTrace()
            getDefaultResult()
        }
    }
    
    /**
     * Heuristic function to infer basic app context from the sender/app name.
     * This is offline-only and uses simple string matching, no network calls.
     */
    private fun inferAppContext(senderId: String): String {
        val name = senderId.lowercase()
        
        // Work / productivity / communication
        if (listOf("gmail", "mail", "outlook", "teams", "slack", "zoom", "meet", "calendar", "todo", "task", "work", "office")
                .any { name.contains(it) }) {
            return "Likely a WORK or PRODUCTIVITY app (email, meetings, tasks, or calendar). Messages here are often more important."
        }
        
        // Messaging / phone / SMS
        if (listOf("message", "messages", "sms", "whatsapp", "telegram", "signal", "imessage", "phone", "dialer", "call")
                .any { name.contains(it) }) {
            return "Likely a PERSONAL MESSAGING or PHONE app. Messages are from real people and can often be important or time-sensitive."
        }
        
        // Finance / banking
        if (listOf("bank", "finance", "pay", "paypal", "revolut", "wise", "cashapp", "trading", "broker")
                .any { name.contains(it) }) {
            return "Likely a FINANCE or BANKING app. Notifications can be important but not always immediately urgent."
        }
        
        // Health / medical
        if (listOf("health", "med", "doctor", "clinic", "hospital", "fitness", "workout")
                .any { name.contains(it) }) {
            return "Likely a HEALTH or MEDICAL related app. Some notifications could be sensitive or important."
        }
        
        // System / OS / utilities
        if (listOf("android", "system", "settings", "update", "security")
                .any { name.contains(it) }) {
            return "Likely a SYSTEM or OS notification. Often informative; urgency depends on the message."
        }
        
        // Games
        if (listOf("game", "games", "play", "clash", "royale", "pubg", "fortnite", "league", "valorant")
                .any { name.contains(it) }) {
            return "Likely a GAME or ENTERTAINMENT app. Messages are usually low urgency unless clearly stating something time-critical."
        }
        
        // Social media
        if (listOf("instagram", "facebook", "messenger", "tiktok", "snapchat", "twitter", "reddit", "discord")
                .any { name.contains(it) }) {
            return "Likely a SOCIAL MEDIA or COMMUNITY app. Most notifications are low to medium urgency unless clearly urgent."
        }
        
        // Shopping / delivery
        if (listOf("amazon", "shop", "shopping", "delivery", "uber", "doordash", "ubereats", "instacart")
                .any { name.contains(it) }) {
            return "Likely a SHOPPING or DELIVERY app. Order/delivery status can be somewhat time-sensitive."
        }
        
        // Default fallback
        return "Unknown or generic app. Treat urgency based mainly on the message content and whether it sounds like a real person vs a system."
    }
    
    /**
     * Parse urgency score from Qwen3 response
     * The response should be just a number (1-6), but may include explanation text
     */
    private fun parseUrgencyScore(response: String, rawResponse: String): AnalysisResult {
        return try {
            val trimmedResponse = response.trim()
            println("SentimentAnalysisService: Parsing urgency score from response: '$trimmedResponse'")
            
            // Strategy 1: Check if response starts with a number 1-6 (most common case)
            val startsWithNumber = """^([1-6])""".toRegex()
            val startMatch = startsWithNumber.find(trimmedResponse)
            if (startMatch != null) {
                val score = startMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 6)
                if (score != null) {
                    return AnalysisResult(
                        urgencyScore = score,
                        sentiment = "NEUTRAL",
                        rawAiResponse = rawResponse
                    )
                }
            }
            
            // Strategy 2: Find all numbers 1-6 in the response and take the last one (most likely the final answer)
            val numberRegex = """([1-6])""".toRegex()
            val allMatches = numberRegex.findAll(trimmedResponse).toList()
            if (allMatches.isNotEmpty()) {
                // Take the last match (most likely the final answer after explanation)
                val lastMatch = allMatches.last()
                val score = lastMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 6)
                if (score != null) {
                    return AnalysisResult(
                        urgencyScore = score,
                        sentiment = "NEUTRAL",
                        rawAiResponse = rawResponse
                    )
                }
            }
            
            // Strategy 3: Fallback - find any single digit and clamp it
            val fallbackRegex = """(\d)""".toRegex()
            val fallbackMatches = fallbackRegex.findAll(trimmedResponse).toList()
            if (fallbackMatches.isNotEmpty()) {
                // Take the last digit found
                val lastDigit = fallbackMatches.last()
                val score = lastDigit.groupValues[1].toIntOrNull()?.coerceIn(1, 6)
                if (score != null) {
                    return AnalysisResult(
                        urgencyScore = score,
                        sentiment = "NEUTRAL",
                        rawAiResponse = rawResponse
                    )
                }
            }
            
            // Strategy 4: Default fallback
            println("SentimentAnalysisService: WARNING - No number found in response: '$trimmedResponse'")
            println("SentimentAnalysisService: Using default score: 3")
            AnalysisResult(
                urgencyScore = 3,
                sentiment = "NEUTRAL",
                rawAiResponse = rawResponse
            )
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error parsing urgency score: ${e.message}")
            e.printStackTrace()
            getDefaultResult()
        }
    }
    
    /**
     * Get default analysis result when analysis fails
     */
    private fun getDefaultResult(): AnalysisResult {
        return AnalysisResult(
            urgencyScore = 3, // Medium urgency
            sentiment = "NEUTRAL",
            rawAiResponse = null
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

