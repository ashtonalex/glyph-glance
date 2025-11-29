package com.example.glyph_glance

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Check if notification listener permission is enabled for this app.
 */
fun Context.isNotificationServiceEnabled(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    return flat?.contains(packageName) == true
}

/**
 * Permission wizard screen for requesting notification listener access.
 * This permission is required for the app to intercept notifications.
 */
@Composable
fun PermissionWizard(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(context.isNotificationServiceEnabled()) }
    
    // Poll for permission change
    LaunchedEffect(Unit) {
        while (!isPermissionGranted) {
            delay(1000) // Check every second
            isPermissionGranted = context.isNotificationServiceEnabled()
            if (isPermissionGranted) {
                delay(500) // Brief delay to show success state
                onAllGranted()
            }
        }
    }
    
    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Icon with pulse animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (!isPermissionGranted) scale else 1f)
                    .background(
                        if (isPermissionGranted) NeonGreen.copy(alpha = 0.15f) 
                        else NeonPurple.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        if (isPermissionGranted) NeonGreen else NeonPurple,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isPermissionGranted) NeonGreen else NeonPurple,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = if (isPermissionGranted) "Permission Granted!" else "Notification Access Required",
                style = MaterialTheme.typography.headlineSmall,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = if (isPermissionGranted) {
                    "Glyph-Glance can now monitor your notifications and provide intelligent alerts."
                } else {
                    "Glyph-Glance needs access to read your notifications to analyze them and trigger smart Glyph patterns."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isPermissionGranted) {
                // Privacy notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFFAA00).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFFFFAA00).copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFAA00),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your notifications are processed locally on-device. No data is sent to external servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFAA00)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Grant permission button
                Button(
                    onClick = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Grant Notification Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Skip button (for development)
                TextButton(
                    onClick = onAllGranted
                ) {
                    Text(
                        text = "Skip for now (limited functionality)",
                        color = TextGrey
                    )
                }
            } else {
                // Success state - button to continue
                Button(
                    onClick = onAllGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Continue to App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

