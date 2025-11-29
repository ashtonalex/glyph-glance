package com.example.glyph_glance

import androidx.compose.runtime.Composable
import com.example.glyph_glance.data.preferences.PreferencesManager
import com.example.glyph_glance.data.repository.NotificationRepository

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun rememberPreferencesManager(): PreferencesManager

@Composable
expect fun rememberNotificationRepository(): NotificationRepository?

@Composable
expect fun rememberHasNotificationPermission(): Boolean

@Composable
expect fun rememberHasNotificationService(): Boolean

expect fun openNotificationPermissionSettings()

expect fun openNotificationServiceSettings()