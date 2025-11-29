package com.example.glyph_glance.hardware

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.glyph_glance.logic.GlyphPattern
import com.example.glyph_glance.service.LiveLogger
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager as NothingGlyphManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages interactions with the Nothing Glyph Interface.
 * Supports both the standard Glyph Interface (GlyphManager) and the new Glyph Matrix (GlyphMatrixManager).
 */
class GlyphManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Legacy Glyph Manager (Phone (1), Phone (2), Phone (2a))
    private var nothingGlyphManager: NothingGlyphManager? = null
    private var glyphFrameBuilder: GlyphFrame.Builder? = null

    // New Glyph Matrix Manager (Phone (3))
    private var glyphMatrixManager: GlyphMatrixManager? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val isMatrixSupported = AtomicBoolean(false)

    // Callback for Glyph Matrix
    private val matrixCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            LiveLogger.addLog("Glyph Matrix Service Connected")
            try {
                // Register for Phone 3 Glyph Matrix
                glyphMatrixManager?.register(Glyph.DEVICE_23112)
                _isConnected.value = true
                isMatrixSupported.set(true)
                LiveLogger.addLog("Glyph Matrix Registered")
            } catch (e: Exception) {
                LiveLogger.addLog("Failed to register Glyph Matrix: ${e.message}")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            LiveLogger.addLog("Glyph Matrix Service Disconnected")
            _isConnected.value = false
            isMatrixSupported.set(false)
        }
    }

    // Callback for Legacy Glyph
    private val legacyCallback = object : NothingGlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
             LiveLogger.addLog("Legacy Glyph Service Connected")
             try {
                 nothingGlyphManager?.register(context.packageName)
                 nothingGlyphManager?.openSession()
                 _isConnected.value = true
                 LiveLogger.addLog("Legacy Session Opened")
             } catch (e: Exception) {
                 LiveLogger.addLog("Failed to open legacy session: ${e.message}")
             }
        }
        override fun onServiceDisconnected(componentName: ComponentName?) {
             LiveLogger.addLog("Legacy Glyph Service Disconnected")
             if (!isMatrixSupported.get()) {
                 _isConnected.value = false
             }
        }
    }

    init {
        connect()
    }

    fun connect() {
        if (!isNothingPhone()) {
            LiveLogger.addLog("Not a Nothing phone, skipping Glyph init")
            return
        }

        // Initialize Matrix Manager
        try {
            glyphMatrixManager = GlyphMatrixManager.getInstance(context.applicationContext)
            glyphMatrixManager?.init(matrixCallback)
            LiveLogger.addLog("GlyphMatrixManager initialized")
        } catch (e: Exception) {
             LiveLogger.addLog("GlyphMatrixManager init failed (Old SDK/Device?): ${e.message}")
        }

        // Initialize Legacy Manager
        try {
            nothingGlyphManager = NothingGlyphManager.getInstance(context.applicationContext)
            nothingGlyphManager?.init(legacyCallback)
            glyphFrameBuilder = GlyphFrame.Builder()
        } catch (e: Exception) {
            LiveLogger.addLog("NothingGlyphManager init failed: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            if (isMatrixSupported.get()) {
                glyphMatrixManager?.closeAppMatrix()
                glyphMatrixManager?.unInit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error un-initing matrix: ${e.message}")
        }
        try {
            nothingGlyphManager?.closeSession()
            nothingGlyphManager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "Error un-initing legacy: ${e.message}")
        }
        _isConnected.value = false
    }

    fun triggerPattern(pattern: GlyphPattern) {
        scope.launch {
            if (!_isConnected.value) {
                LiveLogger.addLog("Not connected, attempting to reconnect...")
                connect()
                delay(500)
            }

            if (isMatrixSupported.get() && glyphMatrixManager != null) {
                handleMatrixPattern(pattern)
            } else {
                handleLegacyPattern(pattern)
            }
        }
    }

    private suspend fun handleMatrixPattern(pattern: GlyphPattern) {
        LiveLogger.addLog("Playing Matrix Pattern: $pattern")
        try {
            when (pattern) {
                GlyphPattern.MATRIX_RAIN -> playMatrixRain()
                GlyphPattern.URGENT -> playMatrixUrgent()
                GlyphPattern.AMBER_BREATHE -> playMatrixBreathe()
                GlyphPattern.NONE -> clearMatrix()
            }
        } catch (e: Exception) {
            LiveLogger.addLog("Error playing matrix pattern: ${e.message}")
        }
    }

    private suspend fun playMatrixRain() {
        // Simple falling "10101" animation
        try {
            // 1. Create text object
            val rainTextBuilder = GlyphMatrixObject.Builder()
            // 2. Animate falling
            for (y in -25..25 step 2) {
                if (!currentCoroutineContext().isActive) break // Check coroutine cancellation
                
                val rainObject = GlyphMatrixObject.Builder()
                    .setText("10101") 
                    .setPosition(0, y)
                    .build()
                    
                val frame = GlyphMatrixFrame.Builder()
                    .addTop(rainObject)
                    .build(context)
                
                // Use setAppMatrixFrame for app-driven control
                glyphMatrixManager?.setAppMatrixFrame(frame.render())
                delay(50) 
            }
            clearMatrix()
        } catch (e: Exception) {
            LiveLogger.addLog("Matrix Rain failed: ${e.message}")
        }
    }

    private suspend fun playMatrixUrgent() {
        try {
            // Rapid flashing
            val fullFrame = GlyphMatrixFrame.Builder()
                // Use scaled up text for maximum coverage
                .addTop(GlyphMatrixObject.Builder().setText("!!!").setScale(200).build()) 
                .build(context)

            val emptyFrame = GlyphMatrixFrame.Builder().build(context)

            repeat(5) {
                glyphMatrixManager?.setAppMatrixFrame(fullFrame.render())
                delay(150)
                glyphMatrixManager?.setAppMatrixFrame(emptyFrame.render())
                delay(150)
            }
        } catch (e: Exception) {
            LiveLogger.addLog("Matrix Urgent failed: ${e.message}")
        }
    }
    
    private suspend fun playMatrixBreathe() {
        // Pulsing brightness
        try {
            val textObj = GlyphMatrixObject.Builder().setText("OOO").build()
            
            // Breathe in
            for (b in 0..255 step 25) {
                val obj = GlyphMatrixObject.Builder()
                    .setText("OOO")
                    .setBrightness(b)
                    .build()
                val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context)
                glyphMatrixManager?.setAppMatrixFrame(frame.render())
                delay(50)
            }
            // Breathe out
            for (b in 255 downTo 0 step 25) {
                val obj = GlyphMatrixObject.Builder()
                    .setText("OOO")
                    .setBrightness(b)
                    .build()
                val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context)
                glyphMatrixManager?.setAppMatrixFrame(frame.render())
                delay(50)
            }
            clearMatrix()
        } catch (e: Exception) {
             LiveLogger.addLog("Matrix Breathe failed: ${e.message}")
        }
    }

    private fun clearMatrix() {
         try {
             val frame = GlyphMatrixFrame.Builder().build(context)
             glyphMatrixManager?.setAppMatrixFrame(frame.render())
         } catch(e: Exception) {
             LiveLogger.addLog("Error clearing matrix: ${e.message}")
         }
    }

    private suspend fun handleLegacyPattern(pattern: GlyphPattern) {
        LiveLogger.addLog("Playing Legacy Pattern: $pattern")
        if (nothingGlyphManager == null || glyphFrameBuilder == null) return

        try {
            when (pattern) {
                GlyphPattern.URGENT -> {
                    // Flash Channel C (Rear)
                    val frame = glyphFrameBuilder!!.buildChannelC().buildPeriod(100).buildCycles(5).build()
                    nothingGlyphManager?.animate(frame)
                }
                GlyphPattern.AMBER_BREATHE -> {
                    // Breathe Channel C
                    val frame = glyphFrameBuilder!!.buildChannelC().buildPeriod(2000).buildCycles(1).build()
                    nothingGlyphManager?.animate(frame)
                }
                GlyphPattern.MATRIX_RAIN -> {
                    // Fallback to a complex pattern on C
                    val frame = glyphFrameBuilder!!.buildChannelC().buildPeriod(300).buildCycles(3).build()
                    nothingGlyphManager?.animate(frame)
                }
                GlyphPattern.NONE -> {
                    nothingGlyphManager?.turnOff()
                }
            }
        } catch (e: Exception) {
            LiveLogger.addLog("Legacy pattern failed: ${e.message}")
        }
    }

    private fun isNothingPhone(): Boolean {
        return Build.MANUFACTURER.contains("nothing", ignoreCase = true)
    }

    companion object {
        private const val TAG = "GlyphManager"
    }
}
