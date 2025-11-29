package com.example.glyph_glance.logic

import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import com.example.glyph_glance.logging.LiveLogger
import com.example.glyph_glance.logging.LogStatus
import com.example.glyph_glance.logging.LogType
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
    private var _isInitialized = false
    private var _isModelDownloaded = false
    private var _isModelLoadedInMemory = false
    
    /** Whether Cactus LM is initialized and ready */
    val isInitialized: Boolean get() = _isInitialized
    
    /** Whether the model has been downloaded */
    val isModelDownloaded: Boolean get() = _isModelDownloaded
    
    /** Whether the model is loaded into memory */
    val isModelLoadedInMemory: Boolean get() = _isModelLoadedInMemory
    
    companion object {
        private const val MODEL_SLUG = "qwen3-0.6"
        private const val CONTEXT_SIZE = 2048
        private const val PREFS_NAME = "cactus_model_prefs"
        private const val KEY_MODEL_DOWNLOADED = "cactus_model_downloaded"
    }
    
    /**
     * Check if the model has been downloaded persistently (survives app restarts).
     * This uses platform-specific SharedPreferences on Android.
     * 
     * @param context Android Context (passed as Any for KMP compatibility)
     * @return true if model was previously downloaded
     */
    fun isModelDownloadedPersistent(context: Any): Boolean {
        return try {
            val androidContext = context as android.content.Context
            val prefs = androidContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_MODEL_DOWNLOADED, false)
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error checking persistent model status: ${e.message}")
            false
        }
    }
    
    /**
     * Mark the model as downloaded in persistent storage.
     * 
     * @param context Android Context (passed as Any for KMP compatibility)
     */
    private fun setModelDownloadedPersistent(context: Any, downloaded: Boolean) {
        try {
            val androidContext = context as android.content.Context
            val prefs = androidContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_MODEL_DOWNLOADED, downloaded).apply()
            println("SentimentAnalysisService: Persistent model download status set to: $downloaded")
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error setting persistent model status: ${e.message}")
        }
    }
    
    /**
     * Download the model if it hasn't been downloaded yet.
     * This should only be called ONCE after app installation.
     * Uses persistent storage to track download status across app restarts.
     * 
     * @param context Android Context (passed as Any for KMP compatibility)
     * @return true if model is downloaded (either now or previously)
     */
    suspend fun downloadModelIfNeeded(context: Any): Boolean {
        // Check if already downloaded persistently
        if (isModelDownloadedPersistent(context)) {
            println("SentimentAnalysisService: Model already downloaded (persistent check)")
            _isModelDownloaded = true
            LiveLogger.setModelDownloaded(true)
            return true
        }
        
        LiveLogger.addLog(
            type = LogType.MODEL_DOWNLOAD,
            message = "Downloading $MODEL_SLUG model (one-time)...",
            status = LogStatus.PENDING
        )
        
        return try {
            val lm = CactusLM()
            
            println("SentimentAnalysisService: Downloading $MODEL_SLUG model (one-time download)...")
            val downloadResult = lm.downloadModel(MODEL_SLUG)
            println("SentimentAnalysisService: Download result: $downloadResult")
            
            // Convert result to Boolean (SDK may return Any in KMP)
            val success = downloadResult as? Boolean ?: (downloadResult != null)
            
            if (success) {
                // Mark as downloaded persistently so we don't download again
                setModelDownloadedPersistent(context, true)
                _isModelDownloaded = true
                LiveLogger.setModelDownloaded(true)
                
                LiveLogger.addLog(
                    type = LogType.MODEL_DOWNLOAD,
                    message = "$MODEL_SLUG model downloaded successfully",
                    status = LogStatus.SUCCESS
                )
                
                println("SentimentAnalysisService: Model download completed and marked as persistent")
            }
            
            success
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error downloading model: ${e.message}")
            e.printStackTrace()
            LiveLogger.logError("Model Download", e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Load the model into memory for inference.
     * This should be called each time the app process starts.
     * The model must be downloaded first via downloadModelIfNeeded().
     * 
     * @return true if model is loaded and ready for inference
     */
    suspend fun loadModelIntoMemory(): Boolean {
        if (_isModelLoadedInMemory && cactusLM != null) {
            println("SentimentAnalysisService: Model already loaded in memory")
            return true
        }
        
        LiveLogger.addLog(
            type = LogType.CACTUS_INIT,
            message = "Loading $MODEL_SLUG model into memory...",
            status = LogStatus.PENDING
        )
        
        return try {
            val lm = CactusLM()
            
            println("SentimentAnalysisService: Initializing $MODEL_SLUG model into memory...")
            val initResult = lm.initializeModel(
                CactusInitParams(
                    model = MODEL_SLUG,
                    contextSize = CONTEXT_SIZE
                )
            )
            println("SentimentAnalysisService: Initialization result: $initResult")
            
            // Convert result to Boolean (SDK may return Any in KMP)
            val success = initResult as? Boolean ?: (initResult != null)
            
            if (success) {
                cactusLM = lm
                _isModelLoadedInMemory = true
                _isInitialized = true
                LiveLogger.setCactusInitialized(true)
                
                LiveLogger.addLog(
                    type = LogType.CACTUS_INIT,
                    message = "$MODEL_SLUG model loaded and ready",
                    status = LogStatus.SUCCESS
                )
                
                println("SentimentAnalysisService: Model loaded into memory successfully")
            }
            
            success
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error loading model into memory: ${e.message}")
            e.printStackTrace()
            LiveLogger.logError("Cactus Initialization", e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Initialize the Cactus LM with Qwen model (legacy method for compatibility).
     * This combines download and load - prefer using downloadModelIfNeeded() + loadModelIntoMemory()
     * for better control over when downloads happen.
     * 
     * @deprecated Use downloadModelIfNeeded() and loadModelIntoMemory() instead
     */
    suspend fun initialize(): Boolean {
        if (_isInitialized) return true
        
        LiveLogger.addLog(
            type = LogType.CACTUS_INIT,
            message = "Starting Cactus LM initialization...",
            status = LogStatus.PENDING
        )
        
        return try {
            val lm = CactusLM()
            
            // Download model (this may re-download if persistent check isn't available)
            println("SentimentAnalysisService: Downloading $MODEL_SLUG model...")
            LiveLogger.addLog(
                type = LogType.MODEL_DOWNLOAD,
                message = "Downloading $MODEL_SLUG model...",
                status = LogStatus.PENDING
            )
            
            val downloadResult = lm.downloadModel(MODEL_SLUG)
            println("SentimentAnalysisService: Download result: $downloadResult")
            
            _isModelDownloaded = true
            LiveLogger.setModelDownloaded(true)
            
            println("SentimentAnalysisService: Initializing $MODEL_SLUG model...")
            LiveLogger.addLog(
                type = LogType.CACTUS_INIT,
                message = "Initializing $MODEL_SLUG model...",
                status = LogStatus.PENDING
            )
            
            val initResult = lm.initializeModel(
                CactusInitParams(
                    model = MODEL_SLUG,
                    contextSize = CONTEXT_SIZE
                )
            )
            println("SentimentAnalysisService: Initialization result: $initResult")
            
            cactusLM = lm
            _isInitialized = true
            _isModelLoadedInMemory = true
            LiveLogger.setCactusInitialized(true)
            
            println("SentimentAnalysisService: Qwen3 model initialized successfully")
            true
        } catch (e: Exception) {
            println("SentimentAnalysisService: Error initializing Cactus LM: ${e.message}")
            e.printStackTrace()
            LiveLogger.logError("Cactus Initialization", e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Ensure the model is ready for inference.
     * Downloads if needed (using context) and loads into memory.
     * 
     * @param context Android Context for persistent download tracking
     * @return true if model is ready for inference
     */
    suspend fun ensureReady(context: Any): Boolean {
        // Step 1: Download if needed (one-time, persisted)
        if (!_isModelDownloaded && !isModelDownloadedPersistent(context)) {
            val downloaded = downloadModelIfNeeded(context)
            if (!downloaded) {
                return false
            }
        } else {
            _isModelDownloaded = true
        }
        
        // Step 2: Load into memory if needed (each process start)
        if (!_isModelLoadedInMemory || cactusLM == null) {
            return loadModelIntoMemory()
        }
        
        return true
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
        if (!_isInitialized || cactusLM == null) {
            // Try to load model into memory (assumes download was done at app start)
            val loadSuccess = loadModelIntoMemory()
            if (!loadSuccess) {
                return getDefaultResult()
            }
        }
        
        val lm = cactusLM ?: return getDefaultResult()
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val appContext = inferAppContext(senderId)
            // Simplified prompt for faster responses - just need a number
            // Explicitly state this is a standalone request with no previous context
            val systemPrompt = "You are an urgency evaluator. Each request is independent with no conversation history. Output ONLY a number 1-6. No explanation."
            
            val userPrompt = """
                Rate notification urgency 1(lowest)-6(critical).
Scale: 1-2=Ignore/Casual, 3-4=Timely, 5-6=Immediate/Emergency.
App: $appContext
Sender: "$senderId"
Msg: "$content"
Output ONLY the integer.
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
            
            val processingTime = System.currentTimeMillis() - startTime
            
            if (result?.success == true && responseText != null && responseText.isNotEmpty()) {
                val analysisResult = parseUrgencyScore(responseText, responseText) // Pass raw response for debugging
                
                // Log the AI response
                LiveLogger.logAIResponse(
                    urgencyScore = analysisResult.urgencyScore,
                    sentiment = analysisResult.sentiment,
                    processingTime = processingTime
                )
                
                analysisResult
            } else {
                println("SentimentAnalysisService: Failed to get analysis result from Cactus")
                LiveLogger.logError("AI Analysis", "Failed to get analysis result from Cactus")
                getDefaultResult()
            }
        } catch (e: Exception) {
            println("Error analyzing notification: ${e.message}")
            e.printStackTrace()
            LiveLogger.logError("AI Analysis", e.message ?: "Unknown error")
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
        _isInitialized = false
        _isModelDownloaded = false
        _isModelLoadedInMemory = false
        LiveLogger.setCactusInitialized(false)
        LiveLogger.setModelDownloaded(false)
    }
}
