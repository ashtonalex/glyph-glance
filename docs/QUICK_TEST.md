# Quick Testing Guide

## 🚀 Quick Start Testing in Android Studio

### Step 1: Build & Run
1. Click **Run** (▶️) or press `Shift+F10`
2. Select your device/emulator
3. Wait for app to install

### Step 2: Grant Notification Access (REQUIRED!)
**Option A: Via Settings (Recommended)**
- Settings → Apps → Special app access → Notification access
- Find "Glyph-Glance" → Toggle ON

**Option B: Via ADB**
```bash
adb shell cmd notification allow_listener com.example.glyph_glance/.service.GlyphNotificationListener
```

### Step 3: Verify Service is Connected
Open **Logcat** in Android Studio and filter for "Glyph":
- ✅ Look for: `"GlyphNotificationListener: Connected to notification service"`
- ❌ If you see: `"Disconnected"` → Re-check notification access

### Step 4: Test Notification Interception

**Method 1: Use Test Helper (Easiest)**
Add this to your code temporarily (e.g., in MainActivity or a test button):

```kotlin
// In MainActivity.onCreate() or a button click handler
import com.example.glyph_glance.test.NotificationTestHelper

// Send a single test notification
NotificationTestHelper.sendTestNotification(
    context = this,
    title = "Test Message",
    text = "This is a test notification"
)

// Or send multiple rapid notifications to test buffering
NotificationTestHelper.sendRapidTestNotifications(
    context = this,
    count = 3,
    delayMs = 1000
)
```

**Method 2: Use Real Apps**
- Send yourself an SMS
- Send a WhatsApp message
- Send a Telegram message
- Any app notification will work!

### Step 5: Check Logcat Output

You should see this sequence in Logcat:

```
✅ GlyphNotificationListener: Service created
✅ GlyphNotificationListener: BufferEngine connected  
✅ GlyphNotificationListener: Connected to notification service
✅ Intercepted: [package] - [message preview]...
✅ Buffer: Added message from [package] (queue size: 1)
⏳ [Wait 20 seconds or send 5+ messages]
✅ Flushing Buffer for [package]: [combined text]...
✅ Glyph triggered: URGENT (or NONE)
✅ MOCK GLYPH: Playing URGENT
```

## 🧪 Test Scenarios

### Test 1: Single Notification
1. Send 1 notification
2. Wait 20 seconds
3. Should see "Flushing Buffer" in Logcat

### Test 2: Split-Text Buffering
1. Send 3-4 notifications quickly (within 5 seconds)
2. Check Logcat - should see queue size increasing
3. After 20 seconds, all should flush together

### Test 3: Immediate Flush
1. Send 5+ notifications rapidly
2. Should flush immediately (no 20-second wait)

### Test 4: Filtering
- Play music → Should NOT be intercepted (ongoing notification)
- System update → Should NOT be intercepted (system notification)
- Regular app notification → SHOULD be intercepted

## 🔍 Monitoring Logs

### In Android Studio Logcat:
1. Open **Logcat** tab (bottom of screen)
2. Filter by: `GlyphNotificationListener` or search for "Glyph"
3. Watch for real-time logs

### Filter Commands:
```
# Show only Glyph-related logs
tag:GlyphNotificationListener

# Or search for keywords
"Intercepted" OR "Buffer" OR "Flushing"
```

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| No logs appearing | Check notification access is granted |
| Service not connecting | Re-enable notification access in Settings |
| Notifications not intercepted | Use real messaging apps (SMS, WhatsApp) |
| Buffer not flushing | Wait 20 seconds or send 5+ messages |
| App crashes | Check Logcat for stack trace, verify AppModule.initialize() called |

## 📱 ADB Commands for Testing

```bash
# Check notification access status
adb shell settings get secure enabled_notification_listeners

# Enable notification access (if you have root)
adb shell cmd notification allow_listener com.example.glyph_glance/.service.GlyphNotificationListener

# Monitor logs in real-time
adb logcat | grep -i glyph

# Clear app data (if needed)
adb shell pm clear com.example.glyph_glance
```

## ✅ Success Checklist

- [ ] App builds and runs
- [ ] Notification access granted
- [ ] Service connects (see "Connected" log)
- [ ] Test notification intercepted
- [ ] Buffer engine working (see queue size logs)
- [ ] Buffer flushes after 20 seconds
- [ ] IntelligenceEngine processes notifications
- [ ] GlyphManager receives commands (see "MOCK GLYPH" logs)

## 💡 Pro Tips

1. **Keep Logcat open** while testing to see real-time feedback
2. **Use the test helper** for consistent, repeatable tests
3. **Test with real apps** to verify production behavior
4. **Check LiveLogger** if Track 3 UI is implemented
5. **Use ADB commands** for quick permission management during development

