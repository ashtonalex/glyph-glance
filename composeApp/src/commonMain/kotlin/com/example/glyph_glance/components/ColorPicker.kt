package com.example.glyph_glance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glyph_glance.*

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    
    // Convert initial color to HSV
    LaunchedEffect(selectedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (selectedColor.red * 255).toInt(),
                (selectedColor.green * 255).toInt(),
                (selectedColor.blue * 255).toInt()
            ), 
            hsv
        )
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    
    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDarkGrey, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Color Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Selected Color", color = TextWhite, style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(currentColor, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Hue Slider
        Text("Hue", color = TextGrey, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = hue,
            onValueChange = { 
                hue = it
                onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = SurfaceBlack
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Saturation Slider
        Text("Saturation", color = TextGrey, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = saturation,
            onValueChange = { 
                saturation = it
                onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = SurfaceBlack
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brightness Slider
        Text("Brightness", color = TextGrey, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = { 
                value = it
                onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = SurfaceBlack
            )
        )
    }
}
