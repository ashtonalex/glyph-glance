package com.example.glyph_glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.glyph_glance.data.preferences.AndroidPreferencesManager
import com.example.glyph_glance.data.preferences.PreferencesManager

@Composable
actual fun rememberPreferencesManager(): PreferencesManager {
    val context = LocalContext.current
    return remember { AndroidPreferencesManager(context) }
}
