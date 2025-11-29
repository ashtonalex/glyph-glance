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

/**
 * Instrumented test to verify Glyph hardware connectivity on Nothing phones.
 * Run this test on a real Nothing Phone to see the Glyph LEDs light up.
 * 
 * Usage: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.glyph_glance.hardware.GlyphInstrumentationTest
 */
@RunWith(AndroidJUnit4::class)
class GlyphInstrumentationTest {

    @Test
    fun testGlyphConnection() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val glyphManager = GlyphManager(appContext)

            println("=== GLYPH CONNECTION TEST ===")
            println("Waiting for Glyph service connection...")
            
            val connected = withTimeoutOrNull(5000) {
                glyphManager.isConnected.first { it }
            }
            
            if (connected == true) {
                println("SUCCESS: Glyph service connected!")
            } else {
                println("WARNING: Glyph not connected (expected on non-Nothing phones)")
            }
            
            // Small delay to ensure connection is stable
            delay(500)
            
            println("=== TEST COMPLETE ===")
        }
    }

    @Test
    fun testAllGlyphPatterns() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val glyphManager = GlyphManager(appContext)

            println("=== GLYPH PATTERN TEST ===")
            println("Waiting for connection...")
            
            val connected = withTimeoutOrNull(5000) {
                glyphManager.isConnected.first { it }
            }
            
            if (connected != true) {
                println("WARNING: Glyph not connected - patterns will be logged but not displayed")
            }

            // Test each pattern with visual breaks
            println("\n[1/5] Testing HIGH_STROBE pattern...")
            glyphManager.triggerPattern(GlyphPattern.HIGH_STROBE)
            delay(3000) // Allow pattern to complete
            
            println("\n[2/5] Testing MEDIUM_PULSE pattern...")
            glyphManager.triggerPattern(GlyphPattern.MEDIUM_PULSE)
            delay(3000)
            
            println("\n[3/5] Testing LOW_SUBTLE pattern...")
            glyphManager.triggerPattern(GlyphPattern.LOW_SUBTLE)
            delay(2000)
            
            println("\n[4/5] Testing AMBER_BREATHE pattern (negative sentiment)...")
            glyphManager.triggerPattern(GlyphPattern.AMBER_BREATHE)
            delay(3000)
            
            println("\n[5/5] Testing POSITIVE_GLOW pattern (positive sentiment)...")
            glyphManager.triggerPattern(GlyphPattern.POSITIVE_GLOW)
            delay(3000)
            
            // Turn off
            println("\nTurning off Glyph...")
            glyphManager.triggerPattern(GlyphPattern.NONE)
            delay(500)
            
            println("=== ALL PATTERNS TESTED ===")
        }
    }
}
