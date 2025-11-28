Here is the comprehensive developer guide for **Track 2: The Body**. This file is designed to be handed directly to the team member responsible for Android Services, Background Lifecycle, and Hardware Integration.

-----

# Track 2: The Body (System & Hardware Integration)

**Owner:** [Developer Name]
**Focus:** Android OS APIs, Background Services, Notification Lifecycle, and Hardware Control.
**Dependencies:** Consumes `IntelligenceEngine` (Track 1); Feeds `LiveLogger` (Track 3).

-----

## 1\. Overview

You are building the "nervous system" of Glyph-Glance. Your code lives in the background, intercepting every notification the phone receives. You decide whether to pass it immediately to the brain or buffer it (for split-texters). Finally, you are the only one allowed to touch the physical hardware (The Glyph Interface) to signal the user.

## 2\. Technical Stack & Dependencies

Add these to your module-level `build.gradle.kts`:

  * **Nothing SDK:** `com.nothing.kgu:glyph-integration` (or the provided hackathon `.aar`).
  * **Coroutines:** `org.jetbrains.kotlinx:kotlinx-coroutines-android`.
  * **Lifecycle:** `androidx.lifecycle:lifecycle-service`.

### Manifest Requirements

You must declare these permissions and services in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="com.nothing.kgu.permission.GLYPH_INTERFACE" />

<application>
    <service android:name=".service.GlyphNotificationListener"
             android:label="Glyph-Glance Listener"
             android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
             android:exported="true">
        <intent-filter>
            <action android:name="android.service.notification.NotificationListenerService" />
        </intent-filter>
    </service>
</application>
```

-----

## 3\. Core Component A: The Notification Listener

This service is the entry point for all data.

### `GlyphNotificationListener.kt`

Extend `NotificationListenerService`. This runs continuously.

**Key Responsibilities:**

1.  **Filtering:** Ignore system notifications (`android`, `com.android.systemui`) or ongoing downloads.
2.  **Extraction:** Pull the text safely from the `StatusBarNotification` extras.
3.  **Connection:** Pass data to the `BufferEngine`.

<!-- end list -->

```kotlin
class GlyphNotificationListener : NotificationListenerService() {

    private val bufferEngine = BufferEngine() // Or injected via DI
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            if (shouldIgnore(it)) return
            
            val title = it.notification.extras.getString("android.title") ?: ""
            val text = it.notification.extras.getString("android.text") ?: ""
            val senderPackage = it.packageName
            
            // Log to UI (Track 3)
            LiveLogger.addLog("Intercepted: $senderPackage - ${text.take(10)}...")
            
            // Pass to Buffer Engine
            bufferEngine.handleIncoming(senderPackage, "$title: $text")
        }
    }

    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        // Logic to ignore ongoing music players, system alerts, or own app
        return sbn.isOngoing || sbn.packageName == packageName
    }
}
```

-----

## 4\. Core Component B: The "Split-Text" Buffer Engine

This is the state machine that handles the "Split Texter" logic defined in the PRD.

### `BufferEngine.kt`

This class manages a queue and a timer.

**Logic:**

  * **Map:** `private val buffers = mutableMapOf<String, SenderBuffer>()` (Key = PackageName or SenderID).
  * **Thresholds:** `SPLIT_THRESHOLD = 10s`, `FLUSH_TIMEOUT = 20s`.

<!-- end list -->

```kotlin
class BufferEngine {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val intelligenceEngine: IntelligenceEngine by lazy { /* Inject Track 1 implementation */ }
    private val glyphManager = GlyphManager()

    fun handleIncoming(senderId: String, text: String) {
        // 1. Check if we already have an active buffer for this sender
        val buffer = buffers.getOrPut(senderId) { SenderBuffer() }
        
        // 2. Add text to queue
        buffer.messages.add(text)
        
        // 3. Reset/Start Flush Timer
        buffer.flushJob?.cancel()
        buffer.flushJob = scope.launch {
            delay(20_000) // 20 seconds wait
            flush(senderId)
        }
        
        // 4. Immediate Flush Override (e.g. queue too long)
        if (buffer.messages.size >= 5) {
            buffer.flushJob?.cancel()
            flush(senderId)
        }
    }

    private suspend fun flush(senderId: String) {
        val buffer = buffers[senderId] ?: return
        val combinedText = buffer.messages.joinToString("\n")
        buffer.messages.clear()
        
        LiveLogger.addLog("Flushing Buffer for $senderId: $combinedText")
        
        // CALL TRACK 1 (THE BRAIN)
        val decision = intelligenceEngine.processNotification(combinedText, senderId)
        
        // EXECUTE DECISION
        if (decision.shouldLightUp) {
            glyphManager.triggerPattern(decision.pattern)
        }
    }
    
    data class SenderBuffer(
        val messages: MutableList<String> = mutableListOf(),
        var flushJob: Job? = null
    )
}
```

-----

## 5\. Core Component C: Glyph Controller (Hardware)

This interacts with the Nothing Phone SDK.

### `GlyphManager.kt`

**Responsibilities:**

  * Wrap the proprietary SDK code.
  * Provide a safe "Mock" implementation so the app doesn't crash on non-Nothing phones during dev.

<!-- end list -->

```kotlin
class GlyphManager(private val context: Context) {

    private var glyphFrameBuilder: GlyphFrame.Builder? = null
    
    init {
        // Initialize Nothing SDK Manager here
        // glyphManager = GlyphManager.getInstance(context)
    }

    fun triggerPattern(pattern: GlyphPattern) {
        try {
            if (!isNothingPhone()) {
                LiveLogger.addLog("MOCK GLYPH: Playing $pattern")
                return
            }

            // Real Hardware Logic
            when (pattern) {
                GlyphPattern.URGENT -> playStrobeRed()
                GlyphPattern.AMBER_BREATHE -> playBreatheAmber()
                else -> { /* Do nothing */ }
            }
        } catch (e: Exception) {
            LiveLogger.addLog("Glyph Error: ${e.message}")
        }
    }

    private fun playStrobeRed() {
        // Example SDK usage (Pseudo-code based on Nothing docs)
        // val frame = glyphFrameBuilder.buildChannel(C_REAR).buildPeriod(100).buildCycles(5).build()
        // glyphManager.display(frame)
    }
}
```

-----

## 6\. Integration Layer (The Glue)

### Connecting to Track 1 (The Brain)

You do not implement the logic inside `IntelligenceEngine`; you just call it.

  * **Dependency:** You need access to the `IntelligenceEngine` interface defined in the Shared Module.
  * **DI:** In your Service `onCreate`, obtain the singleton instance of `IntelligenceEngine`.

### Connecting to Track 3 (The UI Logger)

You need to feed the UI so the user knows the service is working.

  * **Object:** `LiveLogger` (Singleton).
  * **Usage:** Call `LiveLogger.addLog("Buffer started...")` whenever state changes.

-----

## 7\. Development Checklist (Day 1-2)

1.  [ ] **Service Setup:** Create the `GlyphNotificationListener` class and register it in `AndroidManifest`.
2.  [ ] **Permissions:** Run the app and manually grant "Notification Access" in Android Settings (this is required for the listener to work).
3.  [ ] **Log Loop:** Verify you can print incoming notification titles to `Logcat`.
4.  [ ] **Buffer Logic:** Write a unit test for `BufferEngine`. Simulate calling `handleIncoming` 3 times in 1 second and ensure `flush` is only called once after 20 seconds.
5.  [ ] **Hardware Mock:** create the `GlyphManager` and ensure it logs to the console when `triggerPattern` is called.
6.  [ ] **Integration:** Connect `BufferEngine` to a *Mock* `IntelligenceEngine` (that just returns `true`) to test the full flow from "Notification" -\> "Buffer" -\> "Glyph".

-----

## 8\. Interfaces to Consume (Copy from Track 1)

You rely on this interface existing. If Track 1 hasn't finished it, copy this into your project temporarily to keep working.

```kotlin
interface IntelligenceEngine {
    suspend fun processNotification(text: String, sender: String): DecisionResult
}

data class DecisionResult(
    val shouldLightUp: Boolean,
    val pattern: GlyphPattern 
)

enum class GlyphPattern {
    URGENT, 
    AMBER_BREATHE, 
    NONE
}
```