package com.example.glyph_glance.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manager for controlling the Glyph Matrix LED display.
 * Handles initialization and flashing patterns.
 */
class GlyphManager private constructor(private val context: Context) {
    
    private var glyphMatrixManager: Any? = null
    private var isInitialized = false
    private var isInitializing = false
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "GlyphManager"
        private const val DEVICE_23112 = "23112" // Phone 3 device ID
        private const val FLASH_DURATION_MS = 200L // Duration of each flash
        private const val FLASH_INTERVAL_MS = 150L // Interval between flashes
        private const val MATRIX_SIZE = 25 // 25x25 LED matrix
        
        @Volatile
        private var INSTANCE: GlyphManager? = null
        
        fun getInstance(context: Context): GlyphManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlyphManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize the Glyph Matrix Manager
     */
    fun initialize() {
        if (isInitialized || isInitializing) {
            Log.d(TAG, "GlyphManager already initialized or initializing")
            return
        }
        
        isInitializing = true
        
        try {
            // Use reflection to access the SDK classes since they're in the AAR
            val managerClass = Class.forName("com.nothing.glyph.matrix.GlyphMatrixManager")
            val getInstanceMethod = managerClass.getMethod("getInstance", Context::class.java)
            glyphMatrixManager = getInstanceMethod.invoke(null, context)
            
            // Get the Callback interface
            val callbackClass = Class.forName("com.nothing.glyph.matrix.GlyphMatrixManager\$Callback")
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onServiceConnected" -> {
                        Log.d(TAG, "Glyph service connected")
                        registerDevice()
                        isInitialized = true
                        isInitializing = false
                    }
                    "onServiceDisconnected" -> {
                        Log.d(TAG, "Glyph service disconnected")
                        isInitialized = false
                    }
                    else -> null
                }
            }
            
            // Call init with callback
            val initMethod = managerClass.getMethod("init", callbackClass)
            initMethod.invoke(glyphMatrixManager, callback)
            
            Log.d(TAG, "GlyphManager initialization started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GlyphManager", e)
            isInitializing = false
        }
    }
    
    /**
     * Register the device with the Glyph service
     */
    private fun registerDevice() {
        try {
            val glyphClass = Class.forName("com.nothing.glyph.Glyph")
            val deviceField = glyphClass.getField("DEVICE_23112")
            val deviceId = deviceField.get(null) as String
            
            val registerMethod = glyphMatrixManager?.javaClass?.getMethod("register", String::class.java)
            registerMethod?.invoke(glyphMatrixManager, deviceId)
            
            Log.d(TAG, "Device registered: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
        }
    }
    
    /**
     * Flash the glyph 3 times with a bright pattern
     */
    fun flashThreeTimes() {
        if (!isInitialized) {
            Log.w(TAG, "GlyphManager not initialized, attempting to initialize...")
            initialize()
            // Wait a bit for initialization
            managerScope.launch {
                delay(1000)
                if (isInitialized) {
                    performFlash()
                } else {
                    Log.e(TAG, "Failed to initialize GlyphManager for flashing")
                }
            }
            return
        }
        
        performFlash()
    }
    
    private fun performFlash() {
        managerScope.launch {
            try {
                // Create a bright white bitmap for flashing
                val flashBitmap = createFlashBitmap()
                
                // Flash 3 times
                for (i in 1..3) {
                    Log.d(TAG, "Flash $i of 3")
                    
                    // Turn on
                    setGlyphFrame(flashBitmap, brightness = 255)
                    delay(FLASH_DURATION_MS)
                    
                    // Turn off
                    setGlyphFrame(null, brightness = 0)
                    if (i < 3) {
                        delay(FLASH_INTERVAL_MS)
                    }
                }
                
                // Ensure it's off after flashing
                closeGlyph()
                
                Log.d(TAG, "3-flash sequence completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during flash sequence", e)
            }
        }
    }
    
    /**
     * Create a bright bitmap for flashing (all LEDs on)
     */
    private fun createFlashBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, MATRIX_SIZE.toFloat(), MATRIX_SIZE.toFloat(), paint)
        return bitmap
    }
    
    /**
     * Set a glyph frame using the SDK
     */
    private fun setGlyphFrame(bitmap: Bitmap?, brightness: Int) {
        try {
            if (bitmap == null || brightness == 0) {
                closeGlyph()
                return
            }
            
            // Create GlyphMatrixObject
            val objectBuilderClass = Class.forName("com.nothing.glyph.matrix.GlyphMatrixObject\$Builder")
            val objectBuilder = objectBuilderClass.getConstructor().newInstance()
            
            // Set image source
            val setImageSourceMethod = objectBuilderClass.getMethod("setImageSource", Any::class.java)
            setImageSourceMethod.invoke(objectBuilder, bitmap)
            
            // Set position
            val setPositionMethod = objectBuilderClass.getMethod("setPosition", Int::class.java, Int::class.java)
            setPositionMethod.invoke(objectBuilder, 0, 0)
            
            // Set brightness
            val setBrightnessMethod = objectBuilderClass.getMethod("setBrightness", Int::class.java)
            setBrightnessMethod.invoke(objectBuilder, brightness)
            
            // Set scale (100 = original size)
            val setScaleMethod = objectBuilderClass.getMethod("setScale", Int::class.java)
            setScaleMethod.invoke(objectBuilder, 100)
            
            // Build the object
            val buildMethod = objectBuilderClass.getMethod("build")
            val glyphObject = buildMethod.invoke(objectBuilder)
            
            // Create GlyphMatrixFrame
            val frameBuilderClass = Class.forName("com.nothing.glyph.matrix.GlyphMatrixFrame\$Builder")
            val frameBuilder = frameBuilderClass.getConstructor().newInstance()
            
            // Add to top section
            val addTopMethod = frameBuilderClass.getMethod("addTop", 
                Class.forName("com.nothing.glyph.matrix.GlyphMatrixObject"))
            addTopMethod.invoke(frameBuilder, glyphObject)
            
            // Build the frame
            val frameBuildMethod = frameBuilderClass.getMethod("build", Context::class.java)
            val frame = frameBuildMethod.invoke(frameBuilder, context)
            
            // Render the frame
            val renderMethod = frame.javaClass.getMethod("render")
            val renderedFrame = renderMethod.invoke(frame)
            
            // Set the frame using setAppMatrixFrame (for standard apps, not toys)
            val setAppMatrixFrameMethod = glyphMatrixManager?.javaClass?.getMethod(
                "setAppMatrixFrame", 
                Class.forName("com.nothing.glyph.matrix.GlyphMatrixFrame")
            )
            setAppMatrixFrameMethod?.invoke(glyphMatrixManager, renderedFrame)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting glyph frame", e)
        }
    }
    
    /**
     * Close/turn off the glyph display
     */
    private fun closeGlyph() {
        try {
            val closeMethod = glyphMatrixManager?.javaClass?.getMethod("closeAppMatrix")
            closeMethod?.invoke(glyphMatrixManager)
        } catch (e: Exception) {
            Log.e(TAG, "Error closing glyph", e)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            closeGlyph()
            
            if (glyphMatrixManager != null) {
                val unInitMethod = glyphMatrixManager?.javaClass?.getMethod("unInit")
                unInitMethod?.invoke(glyphMatrixManager)
            }
            
            isInitialized = false
            isInitializing = false
            Log.d(TAG, "GlyphManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
