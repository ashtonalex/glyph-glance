package com.example.glyph_glance.di

import android.content.Context
import androidx.room.Room
import com.example.glyph_glance.database.AppDatabase
import com.example.glyph_glance.hardware.GlyphManager
import com.example.glyph_glance.logic.AppRulesRepository
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import com.example.glyph_glance.logic.IntelligenceEngine
import com.example.glyph_glance.logic.RulesRepository
import com.example.glyph_glance.service.BufferEngine
import com.example.glyph_glance.service.LiveLogger
import com.example.glyph_glance.logic.SemanticMatcher
import com.example.glyph_glance.logic.SentimentAnalysisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppModule {
    private var database: AppDatabase? = null
    private var cactusManager: CactusManager? = null
    private var intelligenceEngine: IntelligenceEngine? = null
    private var glyphManager: GlyphManager? = null
    private var bufferEngine: BufferEngine? = null
    private var rulesRepository: RulesRepository? = null

    fun initialize(context: Context) {
        try {
            if (database == null) {
                database = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glyph-db"
                ).build()
                android.util.Log.d("AppModule", "Database initialized successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppModule", "Failed to initialize database", e)
            throw RuntimeException("Failed to initialize database: ${e.message}", e)
        }

        if (cactusManager == null) {
            LiveLogger.addLog("AppModule: Creating CactusManager instance")
            cactusManager = CactusManager()
            // Initialize model download in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    LiveLogger.addLog("AppModule: Starting CactusManager initialization in background")
                    cactusManager?.initialize()
                    
                    // Log status after initialization
                    val status = cactusManager?.getStatus()
                    if (status != null) {
                        LiveLogger.addLog("AppModule: Cactus status - Initialized=${status.isInitialized}, Downloaded=${status.isModelDownloaded}, Ready=${status.isReady}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppModule", "Failed to initialize CactusManager", e)
                    LiveLogger.addLog("AppModule: ERROR - Failed to initialize CactusManager: ${e.message}")
                }
    private var sentimentAnalysisService: SentimentAnalysisService? = null
    private var semanticMatcher: SemanticMatcher? = null
    private var intelligenceEngine: GlyphIntelligenceEngine? = null
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "glyph-db"
            ).build()
        }

        if (sentimentAnalysisService == null) {
            sentimentAnalysisService = SentimentAnalysisService()
            // Note: Model download is now handled separately via downloadModelIfNeeded()
            // which is called from GlyphGlanceApplication.onCreate()
            // Here we just load the model into memory (assumes download was done at app start)
            CoroutineScope(Dispatchers.IO).launch {
                sentimentAnalysisService?.loadModelIntoMemory()
            }
        }
        
        if (semanticMatcher == null) {
            semanticMatcher = SemanticMatcher()
            // Load semantic keywords from assets
            loadSemanticKeywords(context.applicationContext)
        }

        try {
            if (intelligenceEngine == null) {
                if (database != null && cactusManager != null) {
                    intelligenceEngine = GlyphIntelligenceEngine(
                        cactusManager = cactusManager!!,
                        ruleDao = database!!.ruleDao(),
                        contactDao = database!!.contactDao()
                    )
                    android.util.Log.d("AppModule", "Using GlyphIntelligenceEngine")
                } else {
                    throw RuntimeException("Database or CactusManager not available")
                }
            }

            if (glyphManager == null) {
                glyphManager = GlyphManager(context.applicationContext)
            }

            if (bufferEngine == null) {
                bufferEngine = BufferEngine(
                    intelligenceEngine = intelligenceEngine!!,
                    glyphManager = glyphManager!!
                )
            }
            
            // Initialize RulesRepository for UI
            if (rulesRepository == null && database != null && cactusManager != null) {
                rulesRepository = AppRulesRepository(
                    cactusManager = cactusManager!!,
                    ruleDao = database!!.ruleDao()
                )
                android.util.Log.d("AppModule", "RulesRepository initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppModule", "Failed to initialize services", e)
            throw RuntimeException("Failed to initialize services: ${e.message}", e)
        if (intelligenceEngine == null) {
            intelligenceEngine = GlyphIntelligenceEngine(
                sentimentAnalysisService = sentimentAnalysisService!!,
                semanticMatcher = semanticMatcher!!,
                ruleDao = database!!.ruleDao(),
                contactDao = database!!.contactDao()
            )
        }
    }
    
    /**
     * Load semantic keywords from the bundled JSON file.
     */
    private fun loadSemanticKeywords(context: Context) {
        try {
            // Read from assets (copied from composeResources/files/)
            val jsonString = context.assets.open("files/semantic_keywords.json")
                .bufferedReader()
                .use { it.readText() }
            
            val success = semanticMatcher?.loadFromJson(jsonString) ?: false
            if (success) {
                println("AppModule: Loaded semantic keywords successfully")
            } else {
                println("AppModule: Failed to parse semantic keywords JSON")
            }
        } catch (e: Exception) {
            println("AppModule: Error loading semantic keywords: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getIntelligenceEngine(): IntelligenceEngine {
        return intelligenceEngine ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getSentimentAnalysisService(): SentimentAnalysisService {
        return sentimentAnalysisService ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getGlyphManager(): GlyphManager {
        return glyphManager ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getBufferEngine(): BufferEngine {
        return bufferEngine ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getRulesRepository(): RulesRepository {
        return rulesRepository ?: throw IllegalStateException("AppModule not initialized or database unavailable")
    }
    
    fun getCactusManager(): CactusManager {
        return cactusManager ?: throw IllegalStateException("AppModule not initialized")
    }
    
    /**
     * Check if AppModule has been initialized.
     */
    fun isInitialized(): Boolean {
        return database != null && intelligenceEngine != null
    fun getSemanticMatcher(): SemanticMatcher {
        return semanticMatcher ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getApplicationContext(): Context {
        return applicationContext ?: throw IllegalStateException("AppModule not initialized")
    }
    
    /**
     * Reload semantic keywords from JSON string.
     * Useful for updating keywords at runtime.
     */
    fun reloadSemanticKeywords(jsonString: String): Boolean {
        return semanticMatcher?.loadFromJson(jsonString) ?: false
    }
}
