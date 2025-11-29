package com.example.glyph_glance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.glyph_glance.data.models.NotificationPriority

@Composable
fun GlyphCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDarkGrey.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        content = content
    )
}

@Composable
fun GlassyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonBlue
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun NotificationItem(
    title: String,
    message: String,
    time: String,
    importanceColor: Color,
    icon: ImageVector? = null,
    sentiment: String? = null,
    urgencyScore: Int? = null,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceBlack.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Importance Indicator / Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(importanceColor.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, importanceColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = importanceColor)
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(importanceColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey,
                maxLines = 1
            )
            // Sentiment and urgency display
            if (sentiment != null || urgencyScore != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sentiment?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (it.uppercase()) {
                                "POSITIVE" -> Color(0xFF34C759) // Green
                                "NEGATIVE" -> Color(0xFFFF3B30) // Red
                                else -> TextGrey
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                    urgencyScore?.let {
                        Text(
                            text = "Urgency: $it/6",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGrey
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = TextGrey
            )
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = importanceColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog showing detailed analysis information for a notification.
 * Displays the AI thought process, urgency score breakdown, and sentiment analysis.
 */
@Composable
fun NotificationDetailsDialog(
    title: String,
    message: String,
    priority: NotificationPriority,
    sentiment: String?,
    urgencyScore: Int?,
    rawAiResponse: String?,
    onDismiss: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        containerColor = SurfaceBlack,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analysis Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextGrey
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notification Info Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Notification",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGrey,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGrey
                        )
                    }
                }
                
                // Analysis Summary Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Analysis Summary",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGrey,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Priority
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Priority",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGrey
                            )
                            val priorityColor = when (priority) {
                                NotificationPriority.HIGH -> themeColors.highPriority
                                NotificationPriority.MEDIUM -> themeColors.mediumPriority
                                NotificationPriority.LOW -> themeColors.lowPriority
                            }
                            Text(
                                text = priority.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Urgency Score
                        if (urgencyScore != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Urgency Score",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGrey
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val urgencyColor = when (urgencyScore) {
                                        6 -> themeColors.highPriority
                                        5 -> themeColors.highPriority.copy(alpha = 0.9f)
                                        4 -> themeColors.mediumPriority
                                        3 -> themeColors.mediumPriority.copy(alpha = 0.8f)
                                        2 -> themeColors.lowPriority.copy(alpha = 0.9f)
                                        else -> themeColors.lowPriority
                                    }
                                    Text(
                                        text = "$urgencyScore/6",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = urgencyColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Visual indicator dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(6) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        if (index < urgencyScore) urgencyColor else TextGrey.copy(alpha = 0.3f),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Sentiment
                        if (sentiment != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sentiment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGrey
                                )
                                val sentimentColor = when (sentiment.uppercase()) {
                                    "POSITIVE" -> Color(0xFF34C759)
                                    "NEGATIVE" -> themeColors.highPriority
                                    else -> themeColors.mediumPriority
                                }
                                Text(
                                    text = sentiment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = sentimentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // AI Thought Process Section
                if (rawAiResponse != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .border(1.dp, themeColors.mediumPriority.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(themeColors.mediumPriority, CircleShape)
                                )
                                Text(
                                    text = "AI Thought Process",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = themeColors.mediumPriority,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.Black.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = rawAiResponse,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    // No AI response available
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkGrey.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI thought process not available for this notification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGrey
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = themeColors.mediumPriority)
            }
        }
    )
}

