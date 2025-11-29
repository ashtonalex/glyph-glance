package com.example.glyph_glance.di

import android.content.Context
import androidx.room.Room
import com.example.glyph_glance.database.AppDatabase
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import com.example.glyph_glance.logic.SentimentAnalysisService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppModule {
    private var database: AppDatabase? = null
    private var sentimentAnalysisService: SentimentAnalysisService? = null
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

        if (intelligenceEngine == null) {
            intelligenceEngine = GlyphIntelligenceEngine(
                sentimentAnalysisService = sentimentAnalysisService!!,
                ruleDao = database!!.ruleDao(),
                contactDao = database!!.contactDao()
            )
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
    
    fun getApplicationContext(): Context {
        return applicationContext ?: throw IllegalStateException("AppModule not initialized")
    }
}
