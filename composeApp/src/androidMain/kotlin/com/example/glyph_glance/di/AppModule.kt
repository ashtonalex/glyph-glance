package com.example.glyph_glance.di

import android.content.Context
import androidx.room.Room
import com.example.glyph_glance.ai.CactusManager
import com.example.glyph_glance.database.AppDatabase
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppModule {
    private var database: AppDatabase? = null
    private var cactusManager: CactusManager? = null
    private var intelligenceEngine: GlyphIntelligenceEngine? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "glyph-db"
            ).build()
        }

        if (cactusManager == null) {
            cactusManager = CactusManager()
            // Initialize model download in background
            CoroutineScope(Dispatchers.IO).launch {
                cactusManager?.initialize()
            }
        }

        if (intelligenceEngine == null) {
            intelligenceEngine = GlyphIntelligenceEngine(
                cactusManager = cactusManager!!,
                ruleDao = database!!.ruleDao(),
                contactDao = database!!.contactDao()
            )
        }
    }

    fun getIntelligenceEngine(): GlyphIntelligenceEngine {
        return intelligenceEngine ?: throw IllegalStateException("AppModule not initialized")
    }
    
    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("AppModule not initialized")
    }
}
