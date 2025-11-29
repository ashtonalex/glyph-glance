package com.example.glyph_glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.glyph_glance.data.preferences.InMemoryPreferencesManager
import com.example.glyph_glance.data.preferences.PreferencesManager
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun rememberPreferencesManager(): PreferencesManager {
    return remember { InMemoryPreferencesManager() }
}

@Composable
actual fun rememberNotificationRepository(): NotificationRepository? {
    // iOS implementation - return null for now
    return remember { null }
}