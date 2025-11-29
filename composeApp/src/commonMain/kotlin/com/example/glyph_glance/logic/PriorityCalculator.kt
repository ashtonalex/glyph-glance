package com.example.glyph_glance.logic

import com.example.glyph_glance.data.models.AppRule
import com.example.glyph_glance.data.models.KeywordRule
import com.example.glyph_glance.data.models.NotificationPriority

/**
 * Calculates the priority of a notification based on keyword and app rules.
 * High priority rules override lower priority rules.
 * 
 * @param text The notification text (title + message)
 * @param appPackage The package name of the app that sent the notification
 * @param appName The display name of the app that sent the notification (optional, for matching app rules)
 * @param keywordRules List of keyword rules to check against
 * @param appRules List of app rules to check against
 * @param basePriority The base priority from the notification system
 * @return The calculated priority (highest priority found, or basePriority if no rules match)
 */
fun calculatePriority(
    text: String,
    appPackage: String,
    appName: String? = null,
    keywordRules: List<KeywordRule>,
    appRules: List<AppRule>,
    basePriority: NotificationPriority
): NotificationPriority {
    val textLower = text.lowercase()
    var highestPriority: NotificationPriority? = null
    
    // Helper function to update highest priority
    fun updatePriority(newPriority: NotificationPriority) {
        highestPriority = when {
            highestPriority == null -> newPriority
            highestPriority == NotificationPriority.HIGH -> NotificationPriority.HIGH // HIGH can't be overridden
            newPriority == NotificationPriority.HIGH -> NotificationPriority.HIGH // HIGH overrides everything
            highestPriority == NotificationPriority.MEDIUM && newPriority == NotificationPriority.MEDIUM -> NotificationPriority.MEDIUM
            highestPriority == NotificationPriority.MEDIUM -> NotificationPriority.MEDIUM // MEDIUM can't be downgraded
            else -> newPriority // LOW or upgrading from LOW
        }
    }
    
    // Check keyword rules
    keywordRules.forEach { rule ->
        val keywordLower = rule.keyword.lowercase()
        if (textLower.contains(keywordLower)) {
            updatePriority(rule.priority)
        }
    }
    
    // Check app rules (case-insensitive) - check both package name and app name
    val appPackageLower = appPackage.lowercase()
    val appNameLower = appName?.lowercase()
    appRules.forEach { rule ->
        val rulePackageLower = rule.packageName.lowercase()
        val ruleAppNameLower = rule.appName.lowercase()
        
        // Match by package name or app name
        if (rulePackageLower == appPackageLower || 
            (appNameLower != null && ruleAppNameLower == appNameLower)) {
            updatePriority(rule.priority)
        }
    }
    
    // Return the highest priority found, or base priority if no rules matched
    return highestPriority ?: basePriority
}

