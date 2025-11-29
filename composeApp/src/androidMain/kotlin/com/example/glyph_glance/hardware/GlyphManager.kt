package com.example.glyph_glance.hardware

import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.example.glyph_glance.logic.GlyphPattern
import com.example.glyph_glance.service.LiveLogger
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager as NothingGlyphManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Manages Glyph hardware interface for Nothing phones.
 * Provides mock implementation for non-Nothing devices during development.
 */
class GlyphManager(private val context: Context) {

    private var nothingGlyphManager: NothingGlyphManager? = null
    private var glyphFrameBuilder: GlyphFrame.Builder? = null
    
    val isConnected = MutableStateFlow(false)
    
    private val callback = object : NothingGlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
             LiveLogger.addLog("Glyph Service Connected")
             try {
                 // Register with package name
                 nothingGlyphManager?.register(context.packageName)
                 
                 // Try to open session (some SDKs require this, some don't)
                 nothingGlyphManager?.openSession()
                 
                 isConnected.value = true
                 LiveLogger.addLog("Glyph Registered & Session Opened")
             } catch (e: Exception) {
                 LiveLogger.addLog("Failed to open session: ${e.message}")
             }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
             LiveLogger.addLog("Glyph Service Disconnected")
             isConnected.value = false
        }
    }
    
    init {
        try {
            if (isNothingPhone()) {
                nothingGlyphManager = NothingGlyphManager.getInstance(context)
                nothingGlyphManager?.init(callback)
                glyphFrameBuilder = GlyphFrame.Builder()
                LiveLogger.addLog("GlyphManager initialized (Hardware)")
            } else {
                LiveLogger.addLog("GlyphManager initialized (Mock)")
            }
        } catch (e: Exception) {
            LiveLogger.addLog("Failed to init Nothing SDK: ${e.message}")
        }
    }

    fun triggerPattern(pattern: GlyphPattern) {
        if (!isConnected.value) {
            LiveLogger.addLog("Glyph not connected")
            return
        }

        when (pattern) {
            GlyphPattern.HIGH_STROBE -> playHighStrobe()
            GlyphPattern.MEDIUM_PULSE -> playMediumPulse()
            GlyphPattern.LOW_SUBTLE -> playLowSubtle()
            GlyphPattern.AMBER_BREATHE -> playBreatheAmber()
            GlyphPattern.POSITIVE_GLOW -> playPositiveGlow()
            GlyphPattern.NONE -> turnOff()
        }
    }

    /**
     * Aggressive all-channel strobe for HIGH priority/urgent notifications.
     * 150ms on/off cycles, 6 repetitions, maximum brightness across all channels.
     */
    private fun playHighStrobe() {
        LiveLogger.addLog("Playing HIGH_STROBE: Aggressive all-channel strobe")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (nothingGlyphManager == null || glyphFrameBuilder == null) return@launch
                
                repeat(6) { cycle ->
                    LiveLogger.addLog("Strobe cycle ${cycle + 1}/6")
                    
                    // All channels ON simultaneously at max brightness
                    val frame = glyphFrameBuilder!!
                        .buildChannelA()
                        .buildChannelB()
                        .buildChannelC()
                        .buildChannelD()
                        .buildChannelE()
                        .buildPeriod(150)
                        .buildCycles(1)
                        .buildInterval(0)
                        .build()
                    
                    nothingGlyphManager?.toggle(frame)
                    delay(150)
                    nothingGlyphManager?.turnOff()
                    delay(150)
                }
                
                LiveLogger.addLog("HIGH_STROBE complete")
            } catch (e: Exception) {
                LiveLogger.addLog("Error in HIGH_STROBE: ${e.message}")
            }
        }
    }

    /**
     * Moderate pulsing effect for MEDIUM priority notifications.
     * Channels A+B, 400ms cycles, 3 repetitions.
     */
    private fun playMediumPulse() {
        LiveLogger.addLog("Playing MEDIUM_PULSE: Dual-channel pulse")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (nothingGlyphManager == null || glyphFrameBuilder == null) return@launch
                
                repeat(3) { cycle ->
                    LiveLogger.addLog("Pulse cycle ${cycle + 1}/3")
                    
                    // Channels A and B for moderate effect
                    val frame = glyphFrameBuilder!!
                        .buildChannelA()
                        .buildChannelB()
                        .buildPeriod(400)
                        .buildCycles(1)
                        .buildInterval(0)
                        .build()
                    
                    nothingGlyphManager?.toggle(frame)
                    delay(400)
                    nothingGlyphManager?.turnOff()
                    delay(400)
                }
                
                LiveLogger.addLog("MEDIUM_PULSE complete")
            } catch (e: Exception) {
                LiveLogger.addLog("Error in MEDIUM_PULSE: ${e.message}")
            }
        }
    }

    /**
     * Gentle single-channel glow for LOW priority notifications.
     * Channel A only, slow fade animation.
     */
    private fun playLowSubtle() {
        LiveLogger.addLog("Playing LOW_SUBTLE: Gentle single-channel glow")
        try {
            if (nothingGlyphManager == null || glyphFrameBuilder == null) return
            
            // Single channel with slow, gentle animation
            val frame = glyphFrameBuilder!!
                .buildChannelA()
                .buildPeriod(1500)
                .buildCycles(1)
                .buildInterval(0)
                .build()
            
            nothingGlyphManager?.animate(frame)
            LiveLogger.addLog("LOW_SUBTLE triggered")
        } catch (e: Exception) {
            LiveLogger.addLog("Error in LOW_SUBTLE: ${e.message}")
        }
    }

    private fun playBreatheAmber() {
        LiveLogger.addLog("Playing AMBER_BREATHE: Breathing pattern for negative sentiment")
        try {
            if (nothingGlyphManager != null && glyphFrameBuilder != null) {
                val frame = glyphFrameBuilder!!
                    .buildChannelA()
                    .buildPeriod(2000)
                    .buildCycles(3)
                    .buildInterval(0)
                    .build()
                nothingGlyphManager?.animate(frame)
                LiveLogger.addLog("AMBER_BREATHE triggered")
            }
        } catch (e: Exception) {
             LiveLogger.addLog("Error playing breathe: ${e.message}")
        }
    }

    /**
     * Gentle warm glow for POSITIVE sentiment notifications.
     * Soft, slow animation on channels A+C, conveying warmth and positivity.
     */
    private fun playPositiveGlow() {
        LiveLogger.addLog("Playing POSITIVE_GLOW: Gentle warm glow for positive sentiment")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (nothingGlyphManager == null || glyphFrameBuilder == null) return@launch
                
                // Soft, slow glow effect - channels A and C for a warm spread
                repeat(2) { cycle ->
                    LiveLogger.addLog("Positive glow cycle ${cycle + 1}/2")
                    
                    val frame = glyphFrameBuilder!!
                        .buildChannelA()
                        .buildChannelC()
                        .buildPeriod(1200)
                        .buildCycles(1)
                        .buildInterval(0)
                        .build()
                    
                    nothingGlyphManager?.animate(frame)
                    delay(1200)
                }
                
                LiveLogger.addLog("POSITIVE_GLOW complete")
            } catch (e: Exception) {
                LiveLogger.addLog("Error in POSITIVE_GLOW: ${e.message}")
            }
        }
    }
    
    private fun turnOff() {
        try {
            nothingGlyphManager?.turnOff()
        } catch (e: Exception) { }
    }

    private fun isNothingPhone(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return manufacturer.contains("nothing") || model.contains("nothing")
    }
}
