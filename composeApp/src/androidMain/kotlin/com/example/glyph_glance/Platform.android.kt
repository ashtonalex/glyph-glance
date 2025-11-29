package com.example.glyph_glance

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.glyph_glance.data.preferences.AndroidPreferencesManager
import com.example.glyph_glance.data.preferences.PreferencesManager
import com.example.glyph_glance.data.repository.NotificationRepository
import com.example.glyph_glance.data.repository.NotificationRepositoryImpl
import kotlinx.coroutines.delay

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun rememberHasNotificationPermission(): Boolean {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(PermissionHelper.isNotificationPermissionGranted(context)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            hasPermission = PermissionHelper.isNotificationPermissionGranted(context)
        }
    }
    
    return hasPermission
}

@Composable
actual fun rememberHasNotificationService(): Boolean {
    val context = LocalContext.current
    var hasService by remember { mutableStateOf(PermissionHelper.isNotificationServiceEnabled(context)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            hasService = PermissionHelper.isNotificationServiceEnabled(context)
        }
    }
    
    return hasService
}

actual fun openNotificationPermissionSettings() {
    // This will be handled by MainActivity's permission launcher
    // The permission screen will handle it directly
}

actual fun openNotificationServiceSettings() {
    // This will be called from the permission screen
    // The actual implementation is in PermissionScreen.android.kt
}
