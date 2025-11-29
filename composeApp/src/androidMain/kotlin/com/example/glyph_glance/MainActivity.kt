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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            com.cactus.CactusContextInitializer.initialize(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize Cactus SDK", e)
        }
        
        try {
            AppModule.initialize(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize AppModule", e)
        }

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // Original App UI
                App()
                
                // DEBUG BUTTON - FLOATING ON TOP CENTER
                Button(
                    onClick = { 
                        try {
                            val manager = AppModule.getGlyphManager()
                            val isConnected = manager.isConnected.value
                            
                            if (isConnected) {
                                Toast.makeText(this@MainActivity, "Triggering Glyph...", Toast.LENGTH_SHORT).show()
                                manager.triggerPattern(GlyphPattern.URGENT)
                            } else {
                                Toast.makeText(this@MainActivity, "Glyph NOT Connected! Re-initializing...", Toast.LENGTH_LONG).show()
                                // Attempt manual re-init
                                manager.triggerPattern(GlyphPattern.URGENT)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            android.util.Log.e("GlyphTest", "Failed", e)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp) // Avoid status bar
                        .zIndex(10f) // Force on top
                ) {
                    Text("TEST GLYPH")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
