package com.example.glyph_glance

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glyph_glance.service.LiveLogger

/**
 * Matrix-style terminal screen that displays real-time logs from the background service.
 * This provides transparency to the user about what the app is doing.
 */
@Composable
fun TerminalScreen() {
    val logs by LiveLogger.logs.collectAsState()
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header with title and controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLYPH_GLANCE_DAEMON_V1.0",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonRed,
                    fontSize = 16.sp
                )
                Text(
                    text = "Real-time Activity Monitor",
                    fontFamily = FontFamily.Monospace,
                    color = TextGrey,
                    fontSize = 12.sp
                )
            }
            
            // Clear logs button
            IconButton(
                onClick = { LiveLogger.clear() },
                modifier = Modifier
                    .background(SurfaceDarkGrey.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear Logs",
                    tint = NeonRed
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Animated status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(NeonGreen, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LISTENING FOR NOTIFICATIONS",
                fontFamily = FontFamily.Monospace,
                color = NeonGreen,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "[${logs.size} entries]",
                fontFamily = FontFamily.Monospace,
                color = TextGrey,
                fontSize = 10.sp
            )
        }
        
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Terminal output area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "> Waiting for activity...",
                        fontFamily = FontFamily.Monospace,
                        color = TextGrey,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notifications will appear here",
                        fontFamily = FontFamily.Monospace,
                        color = TextGrey.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        LogEntry(log)
                    }
                    
                    // Blinking cursor at the end
                    item {
                        Row {
                            Text(
                                text = "> ",
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "_",
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.alpha(cursorAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntry(log: String) {
    // Parse log type and colorize accordingly
    val (color, prefix) = when {
        log.contains("ERROR", ignoreCase = true) -> NeonRed to "ERR"
        log.contains("MOCK", ignoreCase = true) -> NeonPurple to "MOC"
        log.contains("GLYPH", ignoreCase = true) -> NeonBlue to "GLY"
        log.contains("Buffer", ignoreCase = true) -> Color(0xFFFFAA00) to "BUF"
        log.contains("Intercepted", ignoreCase = true) -> NeonGreen to "INT"
        log.contains("Flush", ignoreCase = true) -> Color(0xFFFF6B6B) to "FLS"
        else -> TextWhite to "LOG"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Log type badge
        Text(
            text = "[$prefix]",
            fontFamily = FontFamily.Monospace,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        // Log content
        Text(
            text = log,
            fontFamily = FontFamily.Monospace,
            color = TextWhite.copy(alpha = 0.9f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

