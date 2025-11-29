package com.example.glyph_glance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.glyph_glance.logic.GlyphPattern
import com.example.glyph_glance.service.LiveLogger

// ... (other imports remain same)

@Composable
fun DashboardScreen() {
    LazyColumn(
        // ... existing modifier ...
    ) {
        // ... existing Header ...
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // DEBUG SECTION
        item {
            androidx.compose.material3.Button(
                onClick = { 
                    // This will be intercepted by Platform specific code or handled via shared logic
                    // But for now, we just log it. The button in MainActivity handles the hardware trigger directly.
                    LiveLogger.addLog("Dashboard Button Clicked (No-op here, use MainActivity button)")
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("This is the Common UI - Use the Button Below Header in Main App")
            }
        }

        // ... existing Summary ...        // Summary Cards
        item {
            SummarySection()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Activity
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(10) { index ->
            NotificationItem(
                title = "Message from Alex",
                message = "Hey, are we still on for tonight?",
                time = "${index + 1}m ago",
                importanceColor = if (index % 3 == 0) NeonRed else if (index % 3 == 1) NeonBlue else NeonGreen,
                icon = if (index % 3 == 0) Icons.Default.Star else if (index % 3 == 1) Icons.Default.Person else Icons.Default.Notifications
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, User",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        }
        
        // Profile Icon or similar
        Box(
// ... other imports
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceDarkGrey, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, NeonBlue, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextWhite)
        }
    }
}

@Composable
fun SummarySection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            title = "Important",
            count = "5",
            color = NeonRed,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Social",
            count = "12",
            color = NeonBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlyphCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        }
    }
}
