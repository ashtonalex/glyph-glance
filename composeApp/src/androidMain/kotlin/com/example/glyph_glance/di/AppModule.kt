package com.example.glyph_glance.di

import android.content.Context
import androidx.room.Room
import com.example.glyph_glance.ai.CactusManager
import com.example.glyph_glance.database.AppDatabase
import com.example.glyph_glance.hardware.GlyphManager
import com.example.glyph_glance.logic.AppRulesRepository
import com.example.glyph_glance.logic.GlyphIntelligenceEngine
import com.example.glyph_glance.logic.IntelligenceEngine
import com.example.glyph_glance.logic.MockIntelligenceEngine
import com.example.glyph_glance.logic.RulesRepository
import com.example.glyph_glance.service.BufferEngine
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
    private var useMockEngine = false

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
            // Don't throw - allow app to continue with mock implementations
            android.util.Log.w("AppModule", "Continuing without database - will use mock IntelligenceEngine")
        }

        if (cactusManager == null) {
            cactusManager = CactusManager()
            // Initialize model download in background (only if not using mock)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    cactusManager?.initialize()
                } catch (e: Exception) {
                    android.util.Log.e("AppModule", "Failed to initialize CactusManager", e)
                    // Continue with mock mode
                }
            }
        }

        try {
            if (intelligenceEngine == null) {
                // Try to create real IntelligenceEngine, fall back to mock if database is not available
                if (database != null && cactusManager != null) {
                    try {
                        intelligenceEngine = GlyphIntelligenceEngine(
                            cactusManager = cactusManager!!,
                            ruleDao = database!!.ruleDao(),
                            contactDao = database!!.contactDao()
                        )
                        android.util.Log.d("AppModule", "Using real GlyphIntelligenceEngine")
                    } catch (e: Exception) {
                        android.util.Log.w("AppModule", "Failed to create GlyphIntelligenceEngine, using mock: ${e.message}")
                        intelligenceEngine = MockIntelligenceEngine()
                        useMockEngine = true
                    }
                } else {
                    android.util.Log.w("AppModule", "Database or CactusManager not available, using MockIntelligenceEngine")
                    intelligenceEngine = MockIntelligenceEngine()
                    useMockEngine = true
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
            // Don't throw - try to use mock instead
            if (intelligenceEngine == null) {
                intelligenceEngine = MockIntelligenceEngine()
                useMockEngine = true
                glyphManager = GlyphManager(context.applicationContext)
                bufferEngine = BufferEngine(
                    intelligenceEngine = intelligenceEngine!!,
                    glyphManager = glyphManager!!
                )
                android.util.Log.w("AppModule", "Using mock implementations due to initialization failure")
            } else {
                throw RuntimeException("Failed to initialize services: ${e.message}", e)
            }
        }
    }

    fun getIntelligenceEngine(): IntelligenceEngine {
        return intelligenceEngine ?: throw IllegalStateException("AppModule not initialized")
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
    }
}
