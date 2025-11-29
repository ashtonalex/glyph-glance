package com.example.glyph_glance

import androidx.compose.runtime.Composable
import com.example.glyph_glance.data.preferences.PreferencesManager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun rememberPreferencesManager(): PreferencesManager