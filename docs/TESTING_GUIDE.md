# Testing Guide for Glyph-Glance

This guide will help you test Track 2 (The Body) implementation in Android Studio.

## Prerequisites

1. Android Studio with an Android device/emulator (API 24+)
2. The app built and installed on the device
3. At least one messaging app installed (e.g., SMS, WhatsApp, Telegram)

## Step 1: Build and Install the App

### Build the app:
```bash
# On Windows
.\gradlew.bat :composeApp:assembleDebug

# On macOS/Linux
./gradlew :composeApp:assembleDebug
```

### Install via Android Studio:
1. Click the "Run" button (green play icon) or press `Shift+F10`
2. Select your device/emulator
3. Wait for the app to install and launch

## Step 2: Grant Notification Access

**This is CRITICAL - the app won't work without this!**

### Method 1: Via Android Settings (Manual)
1. Open **Settings** on your device
2. Go to **Apps** → **Special app access** (or **Apps & notifications** → **Special app access**)
3. Tap **Notification access** (or **Notification listener access**)
4. Find **"Glyph-Glance"** or **"Glyph-Glance Listener"** in the list
5. **Toggle it ON**

### Method 2: Via ADB (For Testing)
```bash
# Enable notification access via ADB
adb shell cmd notification allow_listener com.example.glyph_glance/.service.GlyphNotificationListener
```

### Verify Access:
- Check Logcat for: `"GlyphNotificationListener: Connected to notification service"`
- If you see this, access is granted ✅

## Step 3: Monitor Logs

### In Android Studio:
1. Open **Logcat** (bottom panel)
2. Filter by tag: `GlyphNotificationListener` or search for "Glyph"
3. You should see logs like:
   - `"GlyphNotificationListener: Service created"`
   - `"GlyphNotificationListener: Connected to notification service"`
   - `"Intercepted: [package] - [message preview]..."`

### Via LiveLogger (In-App):
- The app's UI should show logs in real-time (if Track 3 UI is implemented)
- Look for the terminal/log view

## Step 4: Test Notification Interception

### Test 1: Single Notification
1. Send yourself a test SMS or message from another app
2. Check Logcat for:
   ```
   Intercepted: com.android.mms - Test message...
   Buffer: Added message from com.android.mms (queue size: 1)
   ```
3. Wait 20 seconds - you should see:
   ```
   Flushing Buffer for com.android.mms: Test message...
   ```

### Test 2: Split-Text Buffering (Multiple Rapid Messages)
1. Send yourself **3-4 messages quickly** (within 5 seconds) from the same app
2. Check Logcat - you should see:
   ```
   Buffer: Added message from [package] (queue size: 1)
   Buffer: Added message from [package] (queue size: 2)
   Buffer: Added message from [package] (queue size: 3)
   ```
3. After 20 seconds, all messages should be flushed together:
   ```
   Flushing Buffer for [package]: Message 1\nMessage 2\nMessage 3
   ```

### Test 3: Immediate Flush (5+ Messages)
1. Send yourself **5 or more messages quickly**
2. The buffer should flush immediately (no 20-second wait):
   ```
   Buffer: Added message from [package] (queue size: 5)
   Flushing Buffer for [package]: [combined messages]
   ```

## Step 5: Test IntelligenceEngine Integration

After a buffer flush, you should see:
```
Flushing Buffer for [package]: [text]...
Glyph triggered: URGENT
```
or
```
No glyph trigger (decision: NONE)
```

This confirms the flow: **Notification → Buffer → IntelligenceEngine → GlyphManager**

## Step 6: Test GlyphManager (Mock)

Since we're using a mock implementation, check for:
```
MOCK GLYPH: Playing URGENT
```
or
```
MOCK GLYPH: Playing AMBER_BREATHE
```

On a real Nothing phone, this would trigger the actual hardware.

## Step 7: Verify Filtering

The app should **ignore**:
- System notifications (`android`, `com.android.systemui`)
- Ongoing notifications (music players, downloads)
- Empty notifications (no title or text)
- The app's own notifications

Test by:
1. Playing music - should NOT be intercepted
2. System updates - should NOT be intercepted
3. Regular app notifications - SHOULD be intercepted

## Step 8: Test Service Lifecycle

### Test Reconnection:
1. Revoke notification access in Settings
2. Check Logcat: `"GlyphNotificationListener: Disconnected from notification service"`
3. Re-enable access
4. Check Logcat: `"GlyphNotificationListener: Connected to notification service"`

## Troubleshooting

### Issue: No notifications intercepted
- **Check:** Is notification access granted? (Settings → Apps → Special access → Notification access)
- **Check:** Are you testing with a real messaging app? (SMS, WhatsApp, etc.)
- **Check:** Is the service running? Look for "Connected to notification service" in Logcat

### Issue: Buffer not flushing
- **Check:** Wait at least 20 seconds after the last message
- **Check:** Logcat for any errors in BufferEngine
- **Check:** Is IntelligenceEngine initialized? (AppModule.initialize called in MainActivity)

### Issue: App crashes on startup
- **Check:** Is AppModule.initialize() called in MainActivity?
- **Check:** Are all dependencies added to build.gradle.kts?
- **Check:** Logcat for stack traces

### Issue: Service not starting
- **Check:** AndroidManifest.xml has the service declaration
- **Check:** Notification access is granted
- **Check:** Service permission: `BIND_NOTIFICATION_LISTENER_SERVICE`

## Quick Test Checklist

- [ ] App builds and installs successfully
- [ ] Notification access is granted
- [ ] Service connects (see "Connected" log)
- [ ] Single notification is intercepted
- [ ] Multiple rapid notifications are buffered
- [ ] Buffer flushes after 20 seconds
- [ ] 5+ messages trigger immediate flush
- [ ] IntelligenceEngine processes notifications
- [ ] GlyphManager receives pattern commands
- [ ] System notifications are filtered out

## ADB Commands for Testing

```bash
# Check if notification access is enabled
adb shell settings get secure enabled_notification_listeners

# Enable notification access (requires root or manual grant)
adb shell cmd notification allow_listener com.example.glyph_glance/.service.GlyphNotificationListener

# Send a test notification (requires root)
adb shell service call notification 1 s16 "com.example.glyph_glance" i32 1 s16 "Test" s16 "Test notification"

# Monitor logcat for Glyph-related logs
adb logcat | grep -i glyph
```

## Expected Log Flow

When everything works, you should see this sequence in Logcat:

```
1. GlyphNotificationListener: Service created
2. GlyphNotificationListener: BufferEngine connected
3. GlyphNotificationListener: Connected to notification service
4. Intercepted: [package] - [message]...
5. Buffer: Added message from [package] (queue size: 1)
6. [Wait 20 seconds or send 5+ messages]
7. Flushing Buffer for [package]: [combined text]...
8. Glyph triggered: URGENT (or NONE)
9. MOCK GLYPH: Playing URGENT (if shouldLightUp = true)
```

