package com.example.glyph_glance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Cactus SDK
            com.cactus.CactusContextInitializer.initialize(this)
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