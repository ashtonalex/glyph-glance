package com.example.glyph_glance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.glyph_glance.di.AppModule
import com.example.glyph_glance.logic.GlyphPattern
import com.example.glyph_glance.service.LiveLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Cactus SDK
            com.cactus.CactusContextInitializer.initialize(this)
            LiveLogger.addLog("MainActivity: Cactus SDK context initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize Cactus SDK", e)
            // Continue anyway - will use mock mode
        }
        
        try {
            // Initialize DI
            com.example.glyph_glance.di.AppModule.initialize(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize AppModule", e)
            // Crash with proper error message
            throw RuntimeException("Failed to initialize app: ${e.message}", e)
        }
        
        // Check if notification listener permission is granted
        val needsOnboarding = !isNotificationServiceEnabled()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
