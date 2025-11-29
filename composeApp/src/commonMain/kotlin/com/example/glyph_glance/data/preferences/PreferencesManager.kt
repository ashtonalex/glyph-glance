package com.example.glyph_glance.data.preferences

import com.example.glyph_glance.data.models.AppRule
import com.example.glyph_glance.data.models.KeywordRule
import com.example.glyph_glance.data.models.LedTheme
import com.example.glyph_glance.data.models.ThemePreset
import com.example.glyph_glance.data.models.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manager for storing and retrieving app preferences
 * This is a common interface - platform-specific implementations will use DataStore
 */
interface PreferencesManager {
    suspend fun saveKeywordRules(rules: List<KeywordRule>)
    suspend fun getKeywordRules(): List<KeywordRule>
    
    suspend fun saveAppRules(rules: List<AppRule>)
    suspend fun getAppRules(): List<AppRule>
    
    suspend fun saveLedTheme(theme: LedTheme)
    suspend fun getLedTheme(): LedTheme
    
    suspend fun saveCustomPresets(presets: List<ThemePreset>)
    suspend fun getCustomPresets(): List<ThemePreset>
    
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun getUserProfile(): UserProfile
}

/**
 * In-memory implementation for common code
 * Android will have its own DataStore implementation
 */
class InMemoryPreferencesManager : PreferencesManager {
    private var keywordRules = mutableListOf<KeywordRule>()
    private var appRules = mutableListOf<AppRule>()
    private var ledTheme = LedTheme()
    private var customPresets = mutableListOf<ThemePreset>()
    private var userProfile = UserProfile()
    
    override suspend fun saveKeywordRules(rules: List<KeywordRule>) {
        keywordRules.clear()
        keywordRules.addAll(rules)
    }
    
    override suspend fun getKeywordRules(): List<KeywordRule> = keywordRules.toList()
    
    override suspend fun saveAppRules(rules: List<AppRule>) {
        appRules.clear()
        appRules.addAll(rules)
    }
    
    override suspend fun getAppRules(): List<AppRule> = appRules.toList()
    
    override suspend fun saveLedTheme(theme: LedTheme) {
        ledTheme = theme
    }
    
    override suspend fun getLedTheme(): LedTheme = ledTheme
    
    override suspend fun saveCustomPresets(presets: List<ThemePreset>) {
        customPresets.clear()
        customPresets.addAll(presets)
    }
    
    override suspend fun getCustomPresets(): List<ThemePreset> = customPresets.toList()
    
    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfile = profile
    }
    
    override suspend fun getUserProfile(): UserProfile = userProfile
}
