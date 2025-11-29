package com.example.glyph_glance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.glyph_glance.di.AppModule
import com.example.glyph_glance.service.LiveLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Cactus SDK
            LiveLogger.addLog("MainActivity: Initializing Cactus SDK context")
            com.cactus.CactusContextInitializer.initialize(this)
            LiveLogger.addLog("MainActivity: Cactus SDK context initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize Cactus SDK", e)
            LiveLogger.addLog("MainActivity: ERROR - Failed to initialize Cactus SDK context: ${e.message}")
            // Continue anyway - will use mock mode
        }
        
        try {
            // Initialize DI
            AppModule.initialize(this)
            LiveLogger.addLog("App initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize AppModule", e)
            // Don't crash - allow app to run with limited functionality
            LiveLogger.addLog("ERROR: Failed to initialize - ${e.message}")
        }
        
        // Check if notification listener permission is granted
        val needsOnboarding = !isNotificationServiceEnabled()

        setContent {
            App(
                rulesRepository = try { 
                    AppModule.getRulesRepository() 
                } catch (e: Exception) { 
                    null 
                },
                showOnboarding = needsOnboarding,
                onboardingContent = { onComplete ->
                    PermissionWizard(onAllGranted = onComplete)
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
