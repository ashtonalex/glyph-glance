package com.example.glyph_glance.di

import android.content.Context
import androidx.room.Room
import com.example.glyph_glance.database.AppDatabase
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import com.example.glyph_glance.logic.SemanticMatcher
import com.example.glyph_glance.logic.SentimentAnalysisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppModule {
    private var database: AppDatabase? = null
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

    fun getIntelligenceEngine(): GlyphIntelligenceEngine {
        return intelligenceEngine ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getSentimentAnalysisService(): SentimentAnalysisService {
        return sentimentAnalysisService ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("AppModule not initialized")
    }
    
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
