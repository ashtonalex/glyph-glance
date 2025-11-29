package com.example.glyph_glance

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
actual fun PermissionScreen(
    onNotificationPermissionClick: () -> Unit,
    onNotificationServiceClick: () -> Unit,
    hasNotificationPermission: Boolean,
    hasNotificationService: Boolean
) {
    val context = LocalContext.current
    var notificationPermission by remember { mutableStateOf(hasNotificationPermission) }
    var notificationService by remember { mutableStateOf(hasNotificationService) }
    
    // Poll for permission changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            notificationPermission = PermissionHelper.isNotificationPermissionGranted(context)
            notificationService = PermissionHelper.isNotificationServiceEnabled(context)
        }
    }
    
    // Update the click handlers to use the actual context
    val actualOnNotificationServiceClick = {
        PermissionHelper.openNotificationListenerSettings(context)
    }
    
    // Request notification permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result will be picked up by the polling mechanism
    }
    
    val actualOnNotificationPermissionClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = getThemeMediumPriority()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Permissions Required",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Glyph Glance needs the following permissions to function:",
            fontSize = 16.sp,
            color = TextGrey,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Notification Permission Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notificationPermission) 
                    getThemeMediumPriority().copy(alpha = 0.1f) 
                else 
                    Color(0xFF2A2A2A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (notificationPermission) getThemeMediumPriority() else TextGrey,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Permission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = if (notificationPermission) 
                            "✓ Granted" 
                        else 
                            "Required to receive notifications",
                        fontSize = 14.sp,
                        color = if (notificationPermission) getThemeMediumPriority() else TextGrey
                    )
                }
                
                if (!notificationPermission) {
                    Button(
                        onClick = actualOnNotificationPermissionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getThemeMediumPriority()
                        )
                    ) {
                        Text("Grant")
                    }
                }
            }
        }
        
        // Notification Listener Service Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notificationService) 
                    getThemeMediumPriority().copy(alpha = 0.1f) 
                else 
                    Color(0xFF2A2A2A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (notificationService) getThemeMediumPriority() else TextGrey,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Access",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = if (notificationService) 
                            "✓ Enabled" 
                        else 
                            "Required to read notifications",
                        fontSize = 14.sp,
                        color = if (notificationService) getThemeMediumPriority() else TextGrey
                    )
                }
                
                if (!notificationService) {
                    Button(
                        onClick = actualOnNotificationServiceClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getThemeMediumPriority()
                        )
                    ) {
                        Text("Enable")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (notificationPermission && notificationService) {
            Text(
                text = "All permissions granted! The app is ready to use.",
                fontSize = 14.sp,
                color = getThemeMediumPriority(),
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "Please grant all permissions to continue",
                fontSize = 14.sp,
                color = TextGrey
            )
        }
    }
}
