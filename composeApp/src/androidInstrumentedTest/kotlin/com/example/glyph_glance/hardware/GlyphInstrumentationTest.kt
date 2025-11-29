package com.example.glyph_glance.hardware

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.glyph_glance.logic.GlyphPattern
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlyphInstrumentationTest {

    @Test
    fun testGlyphPatterns() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val glyphManager = GlyphManager(appContext)

            println("Waiting for connection...")
            val connected = withTimeoutOrNull(5000) {
                glyphManager.isConnected.first { it }
            }
            
            if (connected != true) {
                println("WARNING: Not connected.")
            } 

            // Run the diagnostic sequence
            println("Running Diagnostic Sequence (Watch your phone!)...")
            glyphManager.triggerPattern(GlyphPattern.URGENT)
            
            // Wait enough time for the sequence (5 channels * 1.5s = ~7.5s)
            delay(8000)
            
            println("Test complete.")
        }
    }
}
