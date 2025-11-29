package com.example.glyph_glance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glyph_glance.logging.LiveLogger
import com.example.glyph_glance.logging.LogEntry
import com.example.glyph_glance.logging.LogStatus
import com.example.glyph_glance.logging.LogType

@Composable
fun ActivityLogsScreen() {
    val themeColors = LocalThemeColors.current
    
    // Collect state from LiveLogger
    val logs by LiveLogger.logs.collectAsState()
    val cactusInitialized by LiveLogger.cactusInitialized.collectAsState()
    val modelDownloaded by LiveLogger.modelDownloaded.collectAsState()
    val serviceRunning by LiveLogger.serviceRunning.collectAsState()
    val notificationAccessEnabled by LiveLogger.notificationAccessEnabled.collectAsState()
    
    // Section expansion states
    var statusExpanded by remember { mutableStateOf(true) }
    var cactusExpanded by remember { mutableStateOf(true) }
    var notificationsExpanded by remember { mutableStateOf(true) }
    var aiResponseExpanded by remember { mutableStateOf(true) }
    var glyphExpanded by remember { mutableStateOf(false) }
    var errorsExpanded by remember { mutableStateOf(true) }
    
    // Filter logs by type
    val cactusLogs = logs.filter { it.type == LogType.CACTUS_INIT || it.type == LogType.MODEL_DOWNLOAD }
    val notificationLogs = logs.filter { it.type == LogType.NOTIFICATION_RECEIVED || it.type == LogType.BUFFER_QUEUE }
    val aiLogs = logs.filter { it.type == LogType.AI_RESPONSE }
    val glyphLogs = logs.filter { it.type == LogType.GLYPH_INTERACTION }
    val errorLogs = logs.filter { it.type == LogType.ERROR }
    val serviceLogs = logs.filter { it.type == LogType.SERVICE_STATUS }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitor system activity and diagnostics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                }
                
                // Clear logs button
                IconButton(
                    onClick = { LiveLogger.clear() },
                    modifier = Modifier
                        .background(SurfaceDarkGrey.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = themeColors.highPriority
                    )
                }
            }
        }
        
        // System Status Section
        item {
            SystemStatusSection(
                cactusInitialized = cactusInitialized,
                modelDownloaded = modelDownloaded,
                serviceRunning = serviceRunning,
                notificationAccessEnabled = notificationAccessEnabled,
                isExpanded = statusExpanded,
                onExpandedChange = { statusExpanded = it }
            )
        }
        
        // Cactus/Model Logs Section
        item {
            LogSection(
                title = "Cactus AI Status",
                icon = Icons.Default.Psychology,
                iconColor = themeColors.mediumPriority,
                logs = cactusLogs,
                isExpanded = cactusExpanded,
                onExpandedChange = { cactusExpanded = it },
                emptyMessage = "No Cactus AI activity yet"
            )
        }
        
        // Notifications Section
        item {
            LogSection(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                iconColor = themeColors.lowPriority,
                logs = notificationLogs,
                isExpanded = notificationsExpanded,
                onExpandedChange = { notificationsExpanded = it },
                emptyMessage = "No notifications received yet"
            )
        }
        
        // AI Response Section
        item {
            LogSection(
                title = "AI Analysis",
                icon = Icons.Default.AutoAwesome,
                iconColor = Color(0xFF9C27B0),
                logs = aiLogs,
                isExpanded = aiResponseExpanded,
                onExpandedChange = { aiResponseExpanded = it },
                emptyMessage = "No AI analysis performed yet"
            )
        }
        
        // Glyph Interaction Section
        item {
            LogSection(
                title = "Glyph Interactions",
                icon = Icons.Default.Lightbulb,
                iconColor = Color(0xFFFFD700),
                logs = glyphLogs,
                isExpanded = glyphExpanded,
                onExpandedChange = { glyphExpanded = it },
                emptyMessage = "No glyph patterns triggered yet"
            )
        }
        
        // Errors Section (only show if there are errors)
        if (errorLogs.isNotEmpty()) {
            item {
                LogSection(
                    title = "Errors",
                    icon = Icons.Default.Error,
                    iconColor = themeColors.highPriority,
                    logs = errorLogs,
                    isExpanded = errorsExpanded,
                    onExpandedChange = { errorsExpanded = it },
                    emptyMessage = "No errors"
                )
            }
        }
    }
}

@Composable
fun SystemStatusSection(
    cactusInitialized: Boolean,
    modelDownloaded: Boolean,
    serviceRunning: Boolean,
    notificationAccessEnabled: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val themeColors = LocalThemeColors.current
    
    GlyphCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = themeColors.mediumPriority,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "System Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "status_rotation"
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp).rotate(rotationAngle)
                    )
                }
                
                // Overall status indicator
                val allGood = cactusInitialized && modelDownloaded && serviceRunning && notificationAccessEnabled
                val statusColor = when {
                    allGood -> Color(0xFF4CAF50)
                    notificationAccessEnabled || serviceRunning -> themeColors.mediumPriority
                    else -> themeColors.highPriority
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, CircleShape)
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusItem(
                        label = "Notification Access",
                        isActive = notificationAccessEnabled,
                        description = if (notificationAccessEnabled) "Enabled" else "Not granted - tap to enable"
                    )
                    StatusItem(
                        label = "Notification Service",
                        isActive = serviceRunning,
                        description = if (serviceRunning) "Running" else "Not running"
                    )
                    StatusItem(
                        label = "Model Downloaded",
                        isActive = modelDownloaded,
                        description = if (modelDownloaded) "Qwen3-0.6 ready" else "Pending download"
                    )
                    StatusItem(
                        label = "Cactus AI Initialized",
                        isActive = cactusInitialized,
                        description = if (cactusInitialized) "Ready for analysis" else "Not initialized"
                    )
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    isActive: Boolean,
    description: String
) {
    val themeColors = LocalThemeColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDarkGrey.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey
            )
        }
        
        // Status indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isActive) Color(0xFF4CAF50).copy(alpha = 0.2f) 
                    else themeColors.highPriority.copy(alpha = 0.2f),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isActive) Color(0xFF4CAF50).copy(alpha = 0.5f) 
                    else themeColors.highPriority.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isActive) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isActive) Color(0xFF4CAF50) else themeColors.highPriority,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LogSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    logs: List<LogEntry>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    emptyMessage: String
) {
    GlyphCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${logs.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "${title}_rotation"
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp).rotate(rotationAngle)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGrey,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        logs.take(20).forEach { log ->
                            LogEntryItem(log = log)
                        }
                        if (logs.size > 20) {
                            Text(
                                text = "... and ${logs.size - 20} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGrey,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val themeColors = LocalThemeColors.current
    
    val statusColor = when (log.status) {
        LogStatus.SUCCESS -> Color(0xFF4CAF50)
        LogStatus.ERROR -> themeColors.highPriority
        LogStatus.WARNING -> themeColors.mediumPriority
        LogStatus.PENDING -> themeColors.lowPriority
        LogStatus.INFO -> TextGrey
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDarkGrey.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .background(statusColor, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )
            if (log.details != null) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGrey,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextGrey.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 1000 -> "Just now"
        diff < 60_000 -> "${diff / 1000}s ago"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

