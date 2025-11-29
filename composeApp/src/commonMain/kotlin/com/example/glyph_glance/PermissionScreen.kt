package com.example.glyph_glance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
expect fun PermissionScreen(
    onNotificationPermissionClick: () -> Unit,
    onNotificationServiceClick: () -> Unit,
    hasNotificationPermission: Boolean,
    hasNotificationService: Boolean
)
