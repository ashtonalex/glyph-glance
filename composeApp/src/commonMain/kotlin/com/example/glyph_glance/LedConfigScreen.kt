package com.example.glyph_glance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LedConfigScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "LED Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Phone Back Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(SurfaceBlack, RoundedCornerShape(32.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LedVisualizer()
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controls
        ConfigOption(title = "Brightness", value = 0.8f)
        Spacer(modifier = Modifier.height(16.dp))
        ConfigOption(title = "Pulse Speed", value = 0.5f)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sync with Music", color = TextWhite, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = true,
                onCheckedChange = {},
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonBlue,
                    checkedTrackColor = NeonBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun LedVisualizer() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Central Ring
        drawCircle(
            color = NeonWhite.copy(alpha = 0.8f),
            radius = 40.dp.toPx(),
            center = Offset(centerX, centerY),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Top Right Strip
        drawLine(
            color = NeonRed.copy(alpha = 0.8f),
            start = Offset(centerX + 60.dp.toPx(), centerY - 60.dp.toPx()),
            end = Offset(centerX + 100.dp.toPx(), centerY - 120.dp.toPx()),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Bottom Left Strip
        drawLine(
            color = NeonBlue.copy(alpha = 0.8f),
            start = Offset(centerX - 60.dp.toPx(), centerY + 60.dp.toPx()),
            end = Offset(centerX - 100.dp.toPx(), centerY + 120.dp.toPx()),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// Helper for white neon since it wasn't in Color.kt
val NeonWhite = Color(0xFFFFFFFF)

@Composable
fun ConfigOption(title: String, value: Float) {
    Column {
        Text(title, color = TextGrey, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = {},
            colors = SliderDefaults.colors(
                thumbColor = NeonBlue,
                activeTrackColor = NeonBlue,
                inactiveTrackColor = SurfaceDarkGrey
            )
        )
    }
}
