package com.example.glyph_glance

import android.app.Application
import android.util.Log
import com.cactus.CactusContextInitializer
import com.example.glyph_glance.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for GlyphGlance.
 * 
 * Initializes the Cactus SDK context and triggers one-time model download
 * at app startup, before any Activity or Service is created.
 */
class GlyphGlanceApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "GlyphGlanceApplication onCreate - initializing Cactus SDK")
        
        // Initialize Cactus SDK context first (required before any Cactus operations)
        CactusContextInitializer.initialize(this)
        
        // Initialize AppModule (creates singletons for database, services, etc.)
        AppModule.initialize(this)
        
        // Trigger one-time model download in background if not already downloaded
        applicationScope.launch {
            try {
                val sentimentService = AppModule.getSentimentAnalysisService()
                sentimentService.downloadModelIfNeeded(this@GlyphGlanceApplication)
                Log.d(TAG, "Model download check completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during model download check", e)
            }
        }
        
        Log.d(TAG, "GlyphGlanceApplication initialization complete")
    }
    
    companion object {
        private const val TAG = "GlyphGlanceApp"
    }
}

