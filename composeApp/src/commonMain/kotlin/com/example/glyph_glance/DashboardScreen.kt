package com.example.glyph_glance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glyph_glance.data.models.AppRule
import com.example.glyph_glance.data.models.KeywordRule
import com.example.glyph_glance.data.models.NotificationPriority
import com.example.glyph_glance.data.models.UserProfile
import com.example.glyph_glance.data.models.Notification as DbNotification
import com.example.glyph_glance.logic.AnalysisResult
import com.example.glyph_glance.logic.SentimentAnalysisService
import com.example.glyph_glance.logic.calculatePriority
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class ActivityFilter {
    ALL,
    HIGH,
    MEDIUM,
    LOW
}

enum class SortOption {
    RECENT,      // Sort by time (most recent first)
    ALPHABETICAL // Sort alphabetically by title
}

// Sample notification data structure for display
data class ActivityNotification(
    val id: Long = 0, // Database ID for deletion
    val title: String,
    val message: String,
    val time: String,
    val minutesAgo: Int,
    val priority: NotificationPriority,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val sentiment: String? = null,
    val urgencyScore: Int? = null,
    val rawAiResponse: String? = null // Raw AI thought process for analysis details
)

@Composable
fun DashboardScreen() {
    val themeColors = LocalThemeColors.current
    val prefsManager = rememberPreferencesManager()
    val notificationRepository = rememberNotificationRepository()
    val coroutineScope = rememberCoroutineScope()
    
    // Recent Activity state
    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }
    var filterExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(SortOption.RECENT) }
    var sortExpanded by remember { mutableStateOf(false) }
    
    // Keywords state
    var keywords by remember { mutableStateOf(listOf<KeywordRule>()) }
    var keywordsExpanded by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    
    // Apps state
    var appRules by remember { mutableStateOf(listOf<AppRule>()) }
    var appsExpanded by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    
    // Developer mode state
    var developerMode by remember { mutableStateOf(false) }
    
    // Sentiment Analysis Test state
    val sentimentAnalysisService = remember { SentimentAnalysisService() }
    var testExpanded by remember { mutableStateOf(false) }
    var testInput by remember { mutableStateOf("") }
    var testSender by remember { mutableStateOf("Test App") }
    var testResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var calculatedPriority by remember { mutableStateOf<NotificationPriority?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }
    var showRawAiResponse by remember { mutableStateOf(false) }
    var finalUrgencyScore by remember { mutableStateOf<Int?>(null) }
    var rulesMatched by remember { mutableStateOf(false) }
    
    // Notification details dialog state
    var selectedNotification by remember { mutableStateOf<ActivityNotification?>(null) }
    var showNotificationDetails by remember { mutableStateOf(false) }
    
    // Track notification processing state for UI refresh
    // Uses the same state check logic as the NotificationStatusCard
    val isProcessing by com.example.glyph_glance.logging.LiveLogger.isProcessingNotification.collectAsState()
    var wasProcessing by remember { mutableStateOf(false) }
    
    // Helper function to format time ago
    fun formatTimeAgo(minutesAgo: Int): String {
        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "${minutesAgo}m ago"
            minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
            else -> "${minutesAgo / 1440}d ago"
        }
    }
    
    // Helper function to get icon for app
    fun getIconForApp(appName: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when {
            appName.contains("Message", ignoreCase = true) || appName.contains("WhatsApp", ignoreCase = true) -> Icons.Default.Star
            appName.contains("Email", ignoreCase = true) || appName.contains("Mail", ignoreCase = true) -> Icons.Default.Person
            else -> Icons.Default.Notifications
        }
    }
    
    // Load notifications from database
    val allNotifications = remember { mutableStateListOf<ActivityNotification>() }
    var dbNotificationsList by remember { mutableStateOf<List<DbNotification>>(emptyList()) }
    
    // Update notifications when database changes
    LaunchedEffect(notificationRepository) {
        notificationRepository?.getAllNotifications()?.collect { notifications ->
            dbNotificationsList = notifications
            val currentTime = System.currentTimeMillis()
            allNotifications.clear()
            allNotifications.addAll(notifications.map { dbNotif ->
                val minutesAgo = ((currentTime - dbNotif.timestamp) / (1000 * 60)).toInt()
                ActivityNotification(
                    id = dbNotif.id,
                    title = dbNotif.title,
                    message = dbNotif.message,
                    time = formatTimeAgo(minutesAgo),
                    minutesAgo = minutesAgo,
                    priority = dbNotif.priority,
                    icon = getIconForApp(dbNotif.appName),
                    sentiment = dbNotif.sentiment,
                    urgencyScore = dbNotif.urgencyScore,
                    rawAiResponse = dbNotif.rawAiResponse
                )
            })
        }
    }
    
    // Refresh dashboard when notification analysis completes (isProcessing: true -> false)
    // Uses the same state check logic as the NotificationStatusCard
    LaunchedEffect(isProcessing) {
        if (wasProcessing && !isProcessing) {
            // Analysis just completed - trigger UI refresh by re-fetching notifications
            notificationRepository?.getAllNotifications()?.collect { notifications ->
                dbNotificationsList = notifications
                val currentTime = System.currentTimeMillis()
                allNotifications.clear()
                allNotifications.addAll(notifications.map { dbNotif ->
                    val minutesAgo = ((currentTime - dbNotif.timestamp) / (1000 * 60)).toInt()
                    ActivityNotification(
                        id = dbNotif.id,
                        title = dbNotif.title,
                        message = dbNotif.message,
                        time = formatTimeAgo(minutesAgo),
                        minutesAgo = minutesAgo,
                        priority = dbNotif.priority,
                        icon = getIconForApp(dbNotif.appName),
                        sentiment = dbNotif.sentiment,
                        urgencyScore = dbNotif.urgencyScore,
                        rawAiResponse = dbNotif.rawAiResponse
                    )
                })
            }
        }
        wasProcessing = isProcessing
    }
    
    // Load keywords, apps, and developer mode on first composition
    LaunchedEffect(Unit) {
        keywords = prefsManager.getKeywordRules()
        appRules = prefsManager.getAppRules()
        developerMode = prefsManager.getDeveloperMode()
    }
    
    // Filter and sort notifications
    // Use allNotifications.size as a key to detect changes to the list
    val filteredNotifications = remember(selectedFilter, selectedSort, allNotifications.size) {
        // First, create indexed list from allNotifications to preserve original order
        val indexedNotifications = allNotifications.mapIndexed { index, notification -> 
            index to notification 
        }
        
        // Filter based on priority
        val filtered = when (selectedFilter) {
            ActivityFilter.ALL -> indexedNotifications
            ActivityFilter.HIGH -> indexedNotifications.filter { it.second.priority == NotificationPriority.HIGH }
            ActivityFilter.MEDIUM -> indexedNotifications.filter { it.second.priority == NotificationPriority.MEDIUM }
            ActivityFilter.LOW -> indexedNotifications.filter { it.second.priority == NotificationPriority.LOW }
        }
        
        when (selectedSort) {
            // For RECENT: sort by minutesAgo ascending (0 = just now, most recent first)
            // Preserve original insertion order (index) for items with same minutesAgo
            // This ensures newly added items (at index 0) appear first when they have the same minutesAgo
            SortOption.RECENT -> filtered
                .sortedWith(compareBy<Pair<Int, ActivityNotification>> { it.second.minutesAgo }
                    .thenBy { it.first }) // Lower index (newer items added at index 0) come first
                .map { it.second }
            SortOption.ALPHABETICAL -> filtered
                .map { it.second }
                .sortedBy { it.title.lowercase() }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav
    ) {
        // Header
        item {
            HeaderSection(
                onDeveloperModeToggle = { enabled ->
                    developerMode = enabled
                    coroutineScope.launch {
                        prefsManager.saveDeveloperMode(enabled)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Summary Cards
        item {
            SummarySection(notifications = allNotifications)
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Notification Processing Status Card
        item {
            NotificationStatusCard()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sentiment Analysis Test Section (only visible in developer mode)
        if (developerMode) {
            item {
                SentimentTestSection(
                    isExpanded = testExpanded,
                    onExpandedChange = { testExpanded = it },
                    testInput = testInput,
                    onTestInputChange = { testInput = it },
                    testSender = testSender,
                    onTestSenderChange = { testSender = it },
                    testResult = testResult,
                    calculatedPriority = calculatedPriority,
                    isAnalyzing = isAnalyzing,
                    testError = testError,
                    testSuccess = testSuccess,
                    showRawAiResponse = showRawAiResponse,
                    onShowRawAiResponseChange = { showRawAiResponse = it },
                    finalUrgencyScore = finalUrgencyScore,
                    onAnalyze = {
                        coroutineScope.launch {
                            isAnalyzing = true
                            testError = null
                            testResult = null
                            calculatedPriority = null
                            testSuccess = false
                            showRawAiResponse = false
                            finalUrgencyScore = null
                            rulesMatched = false
                            try {
                                // Step 1: Check keyword and app rules first
                                val keywordRules = prefsManager.getKeywordRules()
                                val appRules = prefsManager.getAppRules()
                                
                                // Check if this app is ignored - skip AI and assign urgency 1
                                val isAppIgnored = appRules.any { rule ->
                                    rule.isIgnored && rule.appName.equals(testSender, ignoreCase = true)
                                }
                                
                                val analysisResult: AnalysisResult?
                                
                                if (isAppIgnored) {
                                    // Skip AI analysis for ignored apps
                                    analysisResult = AnalysisResult(
                                        urgencyScore = 1,
                                        sentiment = "NEUTRAL",
                                        triggeredRuleId = null,
                                        rawAiResponse = "[App Ignored - AI analysis skipped]"
                                    )
                                } else {
                                    // Step 2: Perform sentiment analysis (only for non-ignored apps)
                                    analysisResult = sentimentAnalysisService.analyzeNotification(
                                        content = testInput,
                                        senderId = testSender
                                    )
                                }
                                testResult = analysisResult
                                
                                // Step 3: Calculate priority based on rules
                                // Pass both package name and app name so app rules can match by either
                                val ruleBasedPriority = calculatePriority(
                                    text = testInput,
                                    appPackage = "com.test.${testSender.lowercase().replace(" ", "")}",
                                    appName = testSender,
                                    keywordRules = keywordRules,
                                    appRules = appRules,
                                    basePriority = NotificationPriority.MEDIUM // Default base priority for tests
                                )
                                
                                // Step 4: Determine final urgency score and priority
                                val basePriority = NotificationPriority.MEDIUM // Default base priority for tests
                                val aiUrgencyScore = analysisResult?.urgencyScore ?: 3
                                val rulesMatchedValue = ruleBasedPriority != basePriority
                                rulesMatched = rulesMatchedValue
                                
                                val finalUrgencyScoreValue = if (isAppIgnored) {
                                    // Ignored apps always get urgency 1
                                    1
                                } else if (rulesMatchedValue) {
                                    // If rules matched, use the maximum of AI score and rule-based score
                                    // This ensures AI score is never lowered, but rules can increase it
                                    val ruleBasedUrgencyScore = ruleBasedPriority.toUrgencyScore()
                                    maxOf(aiUrgencyScore, ruleBasedUrgencyScore)
                                } else {
                                    // If no rules matched, use AI urgency score directly
                                    aiUrgencyScore
                                }
                                finalUrgencyScore = finalUrgencyScoreValue
                                
                                // Final priority should match the final urgency score
                                val finalPriorityFromUrgency = NotificationPriority.fromUrgencyScore(finalUrgencyScoreValue)
                                calculatedPriority = finalPriorityFromUrgency
                                
                                val testNotification = ActivityNotification(
                                    id = 0, // Test notifications don't have database IDs
                                    title = "Test: $testSender",
                                    message = testInput,
                                    time = "Just now",
                                    minutesAgo = 0,
                                    priority = finalPriorityFromUrgency,
                                    icon = getIconForApp(testSender),
                                    sentiment = analysisResult?.sentiment,
                                    urgencyScore = finalUrgencyScoreValue,
                                    rawAiResponse = analysisResult?.rawAiResponse
                                )
                                
                                // Add to the beginning of the list
                                // Using add(0, ...) on mutableStateListOf will trigger recomposition
                                allNotifications.add(0, testNotification)
                                testSuccess = true
                                
                            } catch (e: Exception) {
                                testError = e.message ?: "Unknown error occurred"
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recent Activity with Collapsible Box
        item {
            GlyphCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Header Row - Clickable to toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activityExpanded = !activityExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (activityExpanded) 180f else 0f,
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                label = "icon_rotation"
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = if (activityExpanded) "Collapse" else "Expand",
                                tint = TextWhite,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationAngle)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Activity",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${filteredNotifications.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGrey
                            )
                        }
                    }
                    
                    // Filter and Sort Controls - Only visible when expanded
                    AnimatedVisibility(
                        visible = activityExpanded,
                        enter = expandVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sort Dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { sortExpanded = true }
                                        .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Sort,
                                        contentDescription = "Sort",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (selectedSort) {
                                            SortOption.RECENT -> "Recent"
                                            SortOption.ALPHABETICAL -> "A-Z"
                                        },
                                        color = TextWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Sort",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = sortExpanded,
                                    onDismissRequest = { sortExpanded = false },
                                    modifier = Modifier.background(SurfaceBlack, RoundedCornerShape(8.dp))
                                ) {
                                    SortOption.values().forEach { sort ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (sort) {
                                                        SortOption.RECENT -> "Recent First"
                                                        SortOption.ALPHABETICAL -> "Alphabetical (A-Z)"
                                                    },
                                                    color = if (selectedSort == sort) {
                                                        themeColors.mediumPriority
                                                    } else TextWhite
                                                )
                                            },
                                            onClick = {
                                                selectedSort = sort
                                                sortExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = TextWhite
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // Filter Dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { filterExpanded = true }
                                        .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (selectedFilter) {
                                            ActivityFilter.ALL -> "All"
                                            ActivityFilter.HIGH -> "High"
                                            ActivityFilter.MEDIUM -> "Medium"
                                            ActivityFilter.LOW -> "Low"
                                        },
                                        color = TextWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Filter",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = filterExpanded,
                                    onDismissRequest = { filterExpanded = false },
                                    modifier = Modifier.background(SurfaceBlack, RoundedCornerShape(8.dp))
                                ) {
                                    ActivityFilter.values().forEach { filter ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (filter) {
                                                        ActivityFilter.ALL -> "All"
                                                        ActivityFilter.HIGH -> "High Priority"
                                                        ActivityFilter.MEDIUM -> "Medium Priority"
                                                        ActivityFilter.LOW -> "Low Priority"
                                                    },
                                                    color = if (selectedFilter == filter) {
                                                        when (filter) {
                                                            ActivityFilter.HIGH -> themeColors.highPriority
                                                            ActivityFilter.MEDIUM -> themeColors.mediumPriority
                                                            ActivityFilter.LOW -> themeColors.lowPriority
                                                            else -> TextWhite
                                                        }
                                                    } else TextWhite
                                                )
                                            },
                                            onClick = {
                                                selectedFilter = filter
                                                filterExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = TextWhite
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Notification Items - Animated visibility
                    AnimatedVisibility(
                        visible = activityExpanded,
                        enter = expandVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            filteredNotifications.forEachIndexed { index, notification ->
                                val importanceColor = when (notification.priority) {
                                    NotificationPriority.HIGH -> themeColors.highPriority
                                    NotificationPriority.MEDIUM -> themeColors.mediumPriority
                                    NotificationPriority.LOW -> themeColors.lowPriority
                                }
                                AnimatedDeleteWrapper(
                                    onDelete = {
                                        if (notification.id > 0 && notificationRepository != null) {
                                            coroutineScope.launch {
                                                notificationRepository.deleteNotification(notification.id)
                                            }
                                        }
                                    }
                                ) { deleteHandler ->
                                    NotificationItem(
                                        title = notification.title,
                                        message = notification.message,
                                        time = notification.time,
                                        importanceColor = importanceColor,
                                        icon = notification.icon,
                                        sentiment = notification.sentiment,
                                        urgencyScore = notification.urgencyScore,
                                        onDelete = deleteHandler,
                                        onClick = {
                                            selectedNotification = notification
                                            showNotificationDetails = true
                                        }
                                    )
                                }
                                if (index < filteredNotifications.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Keywords Section with Collapsible Box
        item {
            Spacer(modifier = Modifier.height(24.dp))
            KeywordsSection(
                keywords = keywords,
                isExpanded = keywordsExpanded,
                onExpandedChange = { keywordsExpanded = it },
                onAddClick = { showAddKeywordDialog = true },
                onDelete = { rule ->
                    keywords = keywords - rule
                    coroutineScope.launch {
                        prefsManager.saveKeywordRules(keywords)
                    }
                },
                onAdd = { keyword, priority ->
                    keywords = keywords + KeywordRule(keyword, priority)
                    coroutineScope.launch {
                        prefsManager.saveKeywordRules(keywords)
                    }
                }
            )
        }
        
        // Apps Section with Collapsible Box
        item {
            Spacer(modifier = Modifier.height(24.dp))
            AppsSection(
                appRules = appRules,
                isExpanded = appsExpanded,
                onExpandedChange = { appsExpanded = it },
                onAddClick = { showAddAppDialog = true },
                onDelete = { rule ->
                    appRules = appRules - rule
                    coroutineScope.launch {
                        prefsManager.saveAppRules(appRules)
                    }
                },
                onAdd = { packageName, appName, priority, isIgnored ->
                    appRules = appRules + AppRule(packageName, appName, priority, isIgnored)
                    coroutineScope.launch {
                        prefsManager.saveAppRules(appRules)
                    }
                }
            )
        }
    }
    
    // Dialogs
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onAdd = { keyword: String, priority: NotificationPriority ->
                keywords = keywords + KeywordRule(keyword, priority)
                coroutineScope.launch {
                    prefsManager.saveKeywordRules(keywords)
                }
                showAddKeywordDialog = false
            }
        )
    }
    
    if (showAddAppDialog) {
        AddAppDialog(
            onDismiss = { showAddAppDialog = false },
            onAdd = { packageName: String, appName: String, priority: NotificationPriority, isIgnored: Boolean ->
                appRules = appRules + AppRule(packageName, appName, priority, isIgnored)
                coroutineScope.launch {
                    prefsManager.saveAppRules(appRules)
                }
                showAddAppDialog = false
            }
        )
    }
    
    // Notification Details Dialog
    if (showNotificationDetails && selectedNotification != null) {
        NotificationDetailsDialog(
            title = selectedNotification!!.title,
            message = selectedNotification!!.message,
            priority = selectedNotification!!.priority,
            sentiment = selectedNotification!!.sentiment,
            urgencyScore = selectedNotification!!.urgencyScore,
            rawAiResponse = selectedNotification!!.rawAiResponse,
            onDismiss = {
                showNotificationDetails = false
                selectedNotification = null
            }
        )
    }
}

@Composable
fun HeaderSection(
    onDeveloperModeToggle: (Boolean) -> Unit = {}
) {
    val prefsManager = rememberPreferencesManager()
    val coroutineScope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var developerMode by remember { mutableStateOf(false) }
    
    // Load user profile and developer mode on first composition
    LaunchedEffect(Unit) {
        userProfile = prefsManager.getUserProfile()
        developerMode = prefsManager.getDeveloperMode()
    }
    
    // Reset tap count after 2 seconds of no taps
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            kotlinx.coroutines.delay(2000)
            if (System.currentTimeMillis() - lastTapTime > 2000) {
                tapCount = 0
            }
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, ${userProfile.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userProfile.greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey,
                modifier = Modifier.clickable {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 1000) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime
                    
                    // Enable developer mode after 7 taps
                    if (tapCount >= 7) {
                        val newMode = !developerMode
                        developerMode = newMode
                        coroutineScope.launch {
                            prefsManager.saveDeveloperMode(newMode)
                            onDeveloperModeToggle(newMode)
                        }
                        tapCount = 0
                    }
                }
            )
        }
        
        // Profile Icon - clickable to edit
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceDarkGrey, CircleShape)
                .border(1.dp, LocalThemeColors.current.mediumPriority, CircleShape)
                .clickable { showEditDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Edit Profile", tint = TextWhite)
        }
    }
    
    if (showEditDialog) {
        EditProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { profile ->
                userProfile = profile
                coroutineScope.launch {
                    prefsManager.saveUserProfile(profile)
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var greeting by remember { mutableStateOf(currentProfile.greeting) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = TextWhite) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter your name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = getThemeMediumPriority(),
                        unfocusedBorderColor = TextGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("Greeting") },
                    placeholder = { Text("e.g., Welcome back") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = getThemeMediumPriority(),
                        unfocusedBorderColor = TextGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onSave(UserProfile(name = name.trim(), greeting = greeting.trim().ifBlank { "Welcome back" }))
                    }
                }
            ) {
                Text("Save", color = getThemeMediumPriority())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGrey)
            }
        },
        containerColor = SurfaceBlack
    )
}

@Composable
fun SummarySection(notifications: List<ActivityNotification>) {
    val themeColors = LocalThemeColors.current
    
    // Calculate counts by priority
    val highCount = notifications.count { it.priority == NotificationPriority.HIGH }
    val mediumCount = notifications.count { it.priority == NotificationPriority.MEDIUM }
    val lowCount = notifications.count { it.priority == NotificationPriority.LOW }
    
    // Outer box with solid black background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBlack, RoundedCornerShape(24.dp))
    ) {
        // Inner box with inset shadow effect using border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High Priority
                PriorityCountItem(
                    count = highCount.toString(),
                    label = "High",
                    color = themeColors.highPriority
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                
                // Medium Priority
                PriorityCountItem(
                    count = mediumCount.toString(),
                    label = "Medium",
                    color = themeColors.mediumPriority
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                
                // Low Priority
                PriorityCountItem(
                    count = lowCount.toString(),
                    label = "Low",
                    color = themeColors.lowPriority
                )
            }
        }
    }
}

@Composable
fun PriorityCountItem(
    count: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.headlineLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NotificationStatusCard() {
    val themeColors = LocalThemeColors.current
    val isProcessing by com.example.glyph_glance.logging.LiveLogger.isProcessingNotification.collectAsState()
    
    GlyphCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = themeColors.mediumPriority,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.mediumPriority,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "No pending notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGrey
                )
            }
        }
    }
}

@Composable
private fun AnimatedDeleteWrapper(
    onDelete: () -> Unit,
    content: @Composable (() -> Unit) -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    val transition = updateTransition(targetState = isVisible, label = "delete")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) },
        label = "alpha"
    ) { if (it) 1f else 0f }
    val scale by transition.animateFloat(
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) },
        label = "scale"
    ) { if (it) 1f else 0.7f }
    val offsetX by transition.animateDp(
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) },
        label = "offsetX"
    ) { if (it) 0.dp else (-400).dp }
    
    Box(
        modifier = Modifier
            .alpha(alpha)
            .scale(scale)
            .offset(x = offsetX)
    ) {
        val deleteHandler: () -> Unit = {
            isVisible = false
            coroutineScope.launch {
                kotlinx.coroutines.delay(300) // Wait for animation to complete
                onDelete()
            }
        }
        content(deleteHandler)
    }
}

@Composable
fun KeywordsSection(
    keywords: List<KeywordRule>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (KeywordRule) -> Unit,
    onAdd: (String, NotificationPriority) -> Unit
) {
    val themeColors = LocalThemeColors.current
    
    GlyphCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header Row - Clickable to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "keywords_icon_rotation"
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keywords",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${keywords.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                }
                
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Keyword",
                        tint = themeColors.mediumPriority
                    )
                }
            }
            
            // Keywords List - Animated visibility
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    if (keywords.isEmpty()) {
                        Text(
                            text = "No keywords added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGrey,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        keywords.forEachIndexed { index, rule ->
                            AnimatedDeleteWrapper(
                                onDelete = { onDelete(rule) }
                            ) { deleteHandler ->
                                KeywordRuleItem(
                                    rule = rule,
                                    onDelete = deleteHandler
                                )
                            }
                            if (index < keywords.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppsSection(
    appRules: List<AppRule>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (AppRule) -> Unit,
    onAdd: (String, String, NotificationPriority, Boolean) -> Unit
) {
    val themeColors = LocalThemeColors.current
    
    GlyphCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header Row - Clickable to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "apps_icon_rotation"
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${appRules.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                }
                
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add App",
                        tint = themeColors.mediumPriority
                    )
                }
            }
            
            // Apps List - Animated visibility
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    if (appRules.isEmpty()) {
                        Text(
                            text = "No app rules added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGrey,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        appRules.forEachIndexed { index, rule ->
                            AnimatedDeleteWrapper(
                                onDelete = { onDelete(rule) }
                            ) { deleteHandler ->
                                AppRuleItem(
                                    rule = rule,
                                    onDelete = deleteHandler
                                )
                            }
                            if (index < appRules.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeywordRuleItem(rule: KeywordRule, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.keyword,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Priority: ${rule.priority.name}",
                style = MaterialTheme.typography.bodySmall,
                color = when (rule.priority) {
                    NotificationPriority.HIGH -> getThemeHighPriority()
                    NotificationPriority.MEDIUM -> getThemeMediumPriority()
                    NotificationPriority.LOW -> getThemeLowPriority()
                }
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = getThemeHighPriority())
        }
    }
}

@Composable
fun AppRuleItem(rule: AppRule, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (rule.isIgnored) Color(0xFF2A1A1A) else SurfaceDarkGrey.copy(alpha = 0.6f), 
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp, 
                if (rule.isIgnored) Color(0xFFFF3B30).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f), 
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rule.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rule.isIgnored) TextGrey else TextWhite,
                    fontWeight = FontWeight.Bold
                )
                if (rule.isIgnored) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF3B30).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IGNORED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = if (rule.isIgnored) "All notifications blocked" else "Priority: ${rule.priority.name}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rule.isIgnored) TextGrey else when (rule.priority) {
                    NotificationPriority.HIGH -> getThemeHighPriority()
                    NotificationPriority.MEDIUM -> getThemeMediumPriority()
                    NotificationPriority.LOW -> getThemeLowPriority()
                }
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = getThemeHighPriority())
        }
    }
}

@Composable
fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onAdd: (String, NotificationPriority) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.HIGH) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Keyword", color = TextWhite) },
        text = {
            Column {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Keyword") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = getThemeMediumPriority(),
                        unfocusedBorderColor = TextGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Priority Level:", color = TextGrey)
                Row(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    NotificationPriority.values().forEach { priority ->
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (priority) {
                                    NotificationPriority.HIGH -> getThemeHighPriority().copy(alpha = 0.3f)
                                    NotificationPriority.MEDIUM -> getThemeMediumPriority().copy(alpha = 0.3f)
                                    NotificationPriority.LOW -> getThemeLowPriority().copy(alpha = 0.3f)
                                },
                                selectedLabelColor = when (priority) {
                                    NotificationPriority.HIGH -> getThemeHighPriority()
                                    NotificationPriority.MEDIUM -> getThemeMediumPriority()
                                    NotificationPriority.LOW -> getThemeLowPriority()
                                }
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (keyword.isNotBlank()) onAdd(keyword, selectedPriority) }) {
                Text("Add", color = getThemeMediumPriority())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGrey)
            }
        },
        containerColor = SurfaceBlack
    )
}

@Composable
fun AddAppDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, NotificationPriority, Boolean) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.HIGH) }
    var isIgnored by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App Rule", color = TextWhite) },
        text = {
            Column {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    placeholder = { Text("e.g., WhatsApp") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = getThemeMediumPriority(),
                        unfocusedBorderColor = TextGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Ignore toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ignore this app", color = TextWhite)
                        Text(
                            "Completely block all notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGrey
                        )
                    }
                    Switch(
                        checked = isIgnored,
                        onCheckedChange = { isIgnored = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = Color(0xFFFF3B30), // Red for ignore
                            uncheckedThumbColor = TextGrey,
                            uncheckedTrackColor = SurfaceDarkGrey
                        )
                    )
                }
                
                // Only show priority selection if not ignored
                if (!isIgnored) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Priority Level:", color = TextGrey)
                    Row(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        NotificationPriority.values().forEach { priority ->
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = { Text(priority.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (priority) {
                                        NotificationPriority.HIGH -> getThemeHighPriority().copy(alpha = 0.3f)
                                        NotificationPriority.MEDIUM -> getThemeMediumPriority().copy(alpha = 0.3f)
                                        NotificationPriority.LOW -> getThemeLowPriority().copy(alpha = 0.3f)
                                    },
                                    selectedLabelColor = when (priority) {
                                        NotificationPriority.HIGH -> getThemeHighPriority()
                                        NotificationPriority.MEDIUM -> getThemeMediumPriority()
                                        NotificationPriority.LOW -> getThemeLowPriority()
                                    }
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (appName.isNotBlank()) {
                        onAdd("", appName, selectedPriority, isIgnored)
                    }
                }
            ) {
                Text("Add", color = getThemeMediumPriority())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGrey)
            }
        },
        containerColor = SurfaceBlack
    )
}

@Composable
fun SentimentTestSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    testInput: String,
    onTestInputChange: (String) -> Unit,
    testSender: String,
    onTestSenderChange: (String) -> Unit,
    testResult: AnalysisResult?,
    calculatedPriority: NotificationPriority?,
    isAnalyzing: Boolean,
    testError: String?,
    testSuccess: Boolean,
    showRawAiResponse: Boolean,
    onShowRawAiResponseChange: (Boolean) -> Unit,
    finalUrgencyScore: Int?,
    onAnalyze: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    
    GlyphCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header Row - Clickable to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Sentiment Test",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "test_icon_rotation"
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
            
            // Test Content - Animated visibility
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sender input
                    OutlinedTextField(
                        value = testSender,
                        onValueChange = onTestSenderChange,
                        label = { Text("Sender/App Name") },
                        placeholder = { Text("e.g., WhatsApp, Email, etc.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = getThemeMediumPriority(),
                            unfocusedBorderColor = TextGrey,
                            focusedLabelColor = getThemeMediumPriority(),
                            unfocusedLabelColor = TextGrey
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Message input
                    OutlinedTextField(
                        value = testInput,
                        onValueChange = onTestInputChange,
                        label = { Text("Notification Message") },
                        placeholder = { Text("Enter a notification message to test...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = getThemeMediumPriority(),
                            unfocusedBorderColor = TextGrey,
                            focusedLabelColor = getThemeMediumPriority(),
                            unfocusedLabelColor = TextGrey
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    // Analyze button
                    Button(
                        onClick = onAnalyze,
                        enabled = !isAnalyzing && testInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getThemeMediumPriority(),
                            contentColor = TextWhite,
                            disabledContainerColor = TextGrey.copy(alpha = 0.3f),
                            disabledContentColor = TextGrey
                        )
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TextWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing...")
                        } else {
                            Text("Analyze with AI")
                        }
                    }
                    
                    // Results display
                    if (testResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SurfaceDarkGrey.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Analysis Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Urgency Score (AI Analysis) - 1 to 6
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Urgency Score:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextGrey
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Score display with color (1-2 low, 3-4 medium, 5-6 high)
                                        val aiUrgencyColor = when (testResult.urgencyScore) {
                                            6 -> themeColors.highPriority
                                            5 -> themeColors.highPriority.copy(alpha = 0.9f)
                                            4 -> themeColors.mediumPriority
                                            3 -> themeColors.mediumPriority.copy(alpha = 0.8f)
                                            2 -> themeColors.lowPriority.copy(alpha = 0.9f)
                                            else -> themeColors.lowPriority
                                        }
                                        Text(
                                            text = "${testResult.urgencyScore}/6",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = aiUrgencyColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Visual indicator
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            repeat(6) { index ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (index < testResult.urgencyScore) aiUrgencyColor else TextGrey.copy(alpha = 0.3f),
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Raw AI Response (for debugging) - clickable to expand
                                testResult.rawAiResponse?.let { rawResponse ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onShowRawAiResponseChange(!showRawAiResponse) },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Raw AI Response:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextGrey,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = if (showRawAiResponse) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = if (showRawAiResponse) "Collapse" else "Expand",
                                                tint = TextGrey,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        AnimatedVisibility(
                                            visible = showRawAiResponse,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        SurfaceDarkGrey.copy(alpha = 0.5f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = rawResponse,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextWhite,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Final Urgency Score (after rules) - 1 to 6
                                if (calculatedPriority != null && testResult != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Final Urgency Score:",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextGrey
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Use the stored final urgency score, or calculate it if not available
                                            val displayFinalUrgencyScore = finalUrgencyScore ?: testResult.urgencyScore
                                            
                                            val finalUrgencyColor = when (displayFinalUrgencyScore) {
                                                6 -> themeColors.highPriority
                                                5 -> themeColors.highPriority.copy(alpha = 0.9f)
                                                4 -> themeColors.mediumPriority
                                                3 -> themeColors.mediumPriority.copy(alpha = 0.8f)
                                                2 -> themeColors.lowPriority.copy(alpha = 0.9f)
                                                else -> themeColors.lowPriority
                                            }
                                            Text(
                                                text = "$displayFinalUrgencyScore/6",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = finalUrgencyColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // Visual indicator
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                repeat(5) { index ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                if (index < displayFinalUrgencyScore) finalUrgencyColor else TextGrey.copy(alpha = 0.3f),
                                                                CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Sentiment
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sentiment:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextGrey
                                    )
                                    val sentimentColor = when (testResult.sentiment) {
                                        "POSITIVE" -> Color(0xFF4CAF50)
                                        "NEGATIVE" -> themeColors.highPriority
                                        else -> themeColors.mediumPriority
                                    }
                                    Text(
                                        text = testResult.sentiment,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = sentimentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Final Priority (matches final urgency score)
                                if (calculatedPriority != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Final Priority:",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextGrey
                                        )
                                        val priorityColor = when (calculatedPriority) {
                                            NotificationPriority.HIGH -> themeColors.highPriority
                                            NotificationPriority.MEDIUM -> themeColors.mediumPriority
                                            NotificationPriority.LOW -> themeColors.lowPriority
                                        }
                                        Text(
                                            text = calculatedPriority.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = priorityColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Success message
                    if (testSuccess && testResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF4CAF50).copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "✓ Notification added to Recent Activity with priority based on keyword/app rules",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Error display
                    if (testError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    themeColors.highPriority.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    themeColors.highPriority.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = themeColors.highPriority,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = testError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
