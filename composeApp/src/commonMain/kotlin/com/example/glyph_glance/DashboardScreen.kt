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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glyph_glance.data.models.AppRule
import com.example.glyph_glance.data.models.KeywordRule
import com.example.glyph_glance.data.models.NotificationPriority
import com.example.glyph_glance.data.models.UserProfile
import kotlinx.coroutines.launch

enum class ActivityFilter {
    ALL,
    HIGH,
    MEDIUM,
    LOW
}

// Sample notification data structure for display
data class ActivityNotification(
    val title: String,
    val message: String,
    val time: String,
    val minutesAgo: Int,
    val priority: NotificationPriority,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val sentiment: String? = null,
    val urgencyScore: Int? = null
)

@Composable
fun DashboardScreen() {
    val themeColors = LocalThemeColors.current
    val prefsManager = rememberPreferencesManager()
    val coroutineScope = rememberCoroutineScope()
    
    // Recent Activity state
    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }
    var filterExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    
    // Keywords state
    var keywords by remember { mutableStateOf(listOf<KeywordRule>()) }
    var keywordsExpanded by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    
    // Apps state
    var appRules by remember { mutableStateOf(listOf<AppRule>()) }
    var appsExpanded by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    
    // Load keywords and apps on first composition
    LaunchedEffect(Unit) {
        keywords = prefsManager.getKeywordRules()
        appRules = prefsManager.getAppRules()
    }
    
    // Sample notification data with timestamps
    val allNotifications = remember {
        listOf(
            ActivityNotification("Message from Alex", "Hey, are we still on for tonight?", "1m ago", 1, NotificationPriority.HIGH, Icons.Default.Star, "POSITIVE", 2),
            ActivityNotification("Email from Work", "Meeting reminder at 3 PM", "3m ago", 3, NotificationPriority.MEDIUM, Icons.Default.Person, "NEUTRAL", 3),
            ActivityNotification("Social Update", "You have 5 new followers", "5m ago", 5, NotificationPriority.LOW, Icons.Default.Notifications, "POSITIVE", 1),
            ActivityNotification("Urgent Alert", "System update required", "7m ago", 7, NotificationPriority.HIGH, Icons.Default.Star, "NEUTRAL", 4),
            ActivityNotification("News Update", "Breaking news in your area", "10m ago", 10, NotificationPriority.MEDIUM, Icons.Default.Person, "NEUTRAL", 3),
            ActivityNotification("Weather Alert", "Rain expected this afternoon", "12m ago", 12, NotificationPriority.LOW, Icons.Default.Notifications, "NEUTRAL", 2),
            ActivityNotification("Important Message", "Payment due tomorrow", "15m ago", 15, NotificationPriority.HIGH, Icons.Default.Star, "NEGATIVE", 4),
            ActivityNotification("Calendar Event", "Team meeting in 30 minutes", "18m ago", 18, NotificationPriority.MEDIUM, Icons.Default.Person, "NEUTRAL", 3),
            ActivityNotification("App Notification", "New feature available", "20m ago", 20, NotificationPriority.LOW, Icons.Default.Notifications, "POSITIVE", 1),
            ActivityNotification("Critical Update", "Security patch available", "25m ago", 25, NotificationPriority.HIGH, Icons.Default.Star, "NEUTRAL", 5)
        )
    }
    
    // Filter and sort notifications
    val filteredNotifications = remember(selectedFilter, allNotifications) {
        val filtered = when (selectedFilter) {
            ActivityFilter.ALL -> allNotifications
            ActivityFilter.HIGH -> allNotifications.filter { it.priority == NotificationPriority.HIGH }
            ActivityFilter.MEDIUM -> allNotifications.filter { it.priority == NotificationPriority.MEDIUM }
            ActivityFilter.LOW -> allNotifications.filter { it.priority == NotificationPriority.LOW }
        }
        filtered.sortedByDescending { it.minutesAgo }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav
    ) {
        // Header
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Summary Cards
        item {
            SummarySection(notifications = allNotifications)
            Spacer(modifier = Modifier.height(24.dp))
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
                            Text(
                                text = "Recent Activity",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${filteredNotifications.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGrey
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                                    tint = TextWhite
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
                                NotificationItem(
                                    title = notification.title,
                                    message = notification.message,
                                    time = notification.time,
                                    importanceColor = importanceColor,
                                    icon = notification.icon,
                                    sentiment = notification.sentiment,
                                    urgencyScore = notification.urgencyScore
                                )
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
                onAdd = { packageName, appName, priority ->
                    appRules = appRules + AppRule(packageName, appName, priority)
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
            onAdd = { packageName: String, appName: String, priority: NotificationPriority ->
                appRules = appRules + AppRule(packageName, appName, priority)
                coroutineScope.launch {
                    prefsManager.saveAppRules(appRules)
                }
                showAddAppDialog = false
            }
        )
    }
}

@Composable
fun HeaderSection() {
    val prefsManager = rememberPreferencesManager()
    val coroutineScope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Load user profile on first composition
    LaunchedEffect(Unit) {
        userProfile = prefsManager.getUserProfile()
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
                color = TextGrey
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
                    Text(
                        text = "Keywords",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${keywords.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
    onAdd: (String, String, NotificationPriority) -> Unit
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
                        text = "Apps",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${appRules.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
            .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.appName,
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
    onAdd: (String, String, NotificationPriority) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.HIGH) }
    
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    placeholder = { Text("e.g., com.whatsapp") },
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
            TextButton(
                onClick = { 
                    if (appName.isNotBlank() && packageName.isNotBlank()) {
                        onAdd(packageName, appName, selectedPriority)
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
