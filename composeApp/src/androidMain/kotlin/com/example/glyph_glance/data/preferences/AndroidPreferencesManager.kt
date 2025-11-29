package com.example.glyph_glance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.glyph_glance.data.models.AppRule
import com.example.glyph_glance.data.models.KeywordRule
import com.example.glyph_glance.data.models.LedTheme
import com.example.glyph_glance.data.models.ThemePreset
import com.example.glyph_glance.data.models.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glyph_glance_prefs")

class AndroidPreferencesManager(private val context: Context) : PreferencesManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private val KEYWORD_RULES_KEY = stringPreferencesKey("keyword_rules")
        private val APP_RULES_KEY = stringPreferencesKey("app_rules")
        private val LED_THEME_KEY = stringPreferencesKey("led_theme")
        private val CUSTOM_PRESETS_KEY = stringPreferencesKey("custom_presets")
        private val USER_PROFILE_KEY = stringPreferencesKey("user_profile")
        private val DEVELOPER_MODE_KEY = booleanPreferencesKey("developer_mode")
    }
    
    override suspend fun saveKeywordRules(rules: List<KeywordRule>) {
        context.dataStore.edit {  prefs ->
            prefs[KEYWORD_RULES_KEY] = json.encodeToString(rules)
        }
    }
    
    override suspend fun getKeywordRules(): List<KeywordRule> {
        return context.dataStore.data.map { prefs ->
            prefs[KEYWORD_RULES_KEY]?.let { jsonString ->
                json.decodeFromString<List<KeywordRule>>(jsonString)
            } ?: emptyList()
        }.first()
    }
    
    override suspend fun saveAppRules(rules: List<AppRule>) {
        context.dataStore.edit { prefs ->
            prefs[APP_RULES_KEY] = json.encodeToString(rules)
        }
    }
    
    override suspend fun getAppRules(): List<AppRule> {
        return context.dataStore.data.map { prefs ->
            prefs[APP_RULES_KEY]?.let { jsonString ->
                json.decodeFromString<List<AppRule>>(jsonString)
            } ?: emptyList()
        }.first()
    }
    
    override suspend fun saveLedTheme(theme: LedTheme) {
        context.dataStore.edit { prefs ->
            prefs[LED_THEME_KEY] = json.encodeToString(theme)
        }
    }
    
    override suspend fun getLedTheme(): LedTheme {
        return context.dataStore.data.map { prefs ->
            prefs[LED_THEME_KEY]?.let { jsonString ->
                json.decodeFromString<LedTheme>(jsonString)
            } ?: LedTheme()
        }.first()
    }
    
    override suspend fun saveCustomPresets(presets: List<ThemePreset>) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_PRESETS_KEY] = json.encodeToString(presets)
        }
    }
    
    override suspend fun getCustomPresets(): List<ThemePreset> {
        return context.dataStore.data.map { prefs ->
            prefs[CUSTOM_PRESETS_KEY]?.let { jsonString ->
                json.decodeFromString<List<ThemePreset>>(jsonString)
            } ?: emptyList()
        }.first()
    }
    
    override suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[USER_PROFILE_KEY] = json.encodeToString(profile)
        }
    }
    
    override suspend fun getUserProfile(): UserProfile {
        return context.dataStore.data.map { prefs ->
            prefs[USER_PROFILE_KEY]?.let { jsonString ->
                json.decodeFromString<UserProfile>(jsonString)
            } ?: UserProfile()
        }.first()
    }
    
    override suspend fun saveDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DEVELOPER_MODE_KEY] = enabled
        }
    }
    
    override suspend fun getDeveloperMode(): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[DEVELOPER_MODE_KEY] ?: false
        }.first()
    }
}
