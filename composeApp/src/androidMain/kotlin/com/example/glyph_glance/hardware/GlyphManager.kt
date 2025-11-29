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
            GlyphPattern.URGENT -> runDiagnosticSequence() 
            GlyphPattern.AMBER_BREATHE -> playBreatheAmber()
            GlyphPattern.NONE -> turnOff()
        }
    }

    private fun runDiagnosticSequence() {
        LiveLogger.addLog("STARTING DIAGNOSTIC: Testing Channels A-E")
        CoroutineScope(Dispatchers.IO).launch {
            // Channel A
            testChannel("A") { it.buildChannelA() }
            
            // Channel B
            testChannel("B") { it.buildChannelB() }
            
            // Channel C
            testChannel("C") { it.buildChannelC() }
            
            // Channel D
            testChannel("D") { it.buildChannelD() }
            
            // Channel E
            testChannel("E") { it.buildChannelE() }
            
            // Numeric Channel Scan (0-4)
            testNumericChannels()
            
            LiveLogger.addLog("Diagnostic Complete")
        }
    }

    private suspend fun testChannel(name: String, channelBuilder: (GlyphFrame.Builder) -> GlyphFrame.Builder) {
        try {
            if (nothingGlyphManager == null || glyphFrameBuilder == null) return
            
            LiveLogger.addLog("Testing Channel $name (Toggle ON)")
            
            val frame = channelBuilder(glyphFrameBuilder!!)
                .buildPeriod(1000)
                .buildCycles(1)
                .buildInterval(0)
                .build()
                
            nothingGlyphManager?.toggle(frame)
            delay(1500)
            nothingGlyphManager?.turnOff()
            delay(500)
            
        } catch (e: Exception) {
            LiveLogger.addLog("Failed Channel $name: ${e.message}")
        }
    }
    
    private suspend fun testNumericChannels() {
        try {
            if (nothingGlyphManager == null || glyphFrameBuilder == null) return
            LiveLogger.addLog("Testing Numeric Channels 0-4")
            
            for (i in 0..4) {
                LiveLogger.addLog("Testing Channel ID $i")
                val frame = glyphFrameBuilder!!
                    .buildChannel(i)
                    .buildPeriod(1000)
                    .buildCycles(1)
                    .buildInterval(0)
                    .build()
                    
                nothingGlyphManager?.toggle(frame)
                delay(1500)
                nothingGlyphManager?.turnOff()
                delay(500)
            }
        } catch(e: Exception) {
             LiveLogger.addLog("Failed Numeric Test: ${e.message}")
        }
    }

    private fun playBreatheAmber() {
        try {
            if (nothingGlyphManager != null && glyphFrameBuilder != null) {
                val frame = glyphFrameBuilder!!
                    .buildChannelA()
                    .buildPeriod(2000)
                    .buildCycles(3)
                    .buildInterval(0)
                    .build()
                nothingGlyphManager?.animate(frame)
            }
        } catch (e: Exception) {
             LiveLogger.addLog("Error playing breathe: ${e.message}")
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
