package com.example.glyph_glance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glyph_glance.components.ColorPicker
import com.example.glyph_glance.data.models.*
import kotlinx.coroutines.launch

@Composable
fun LedConfigScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Configuration", "LED Colors")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Text(
            text = "LED Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceBlack,
            contentColor = getThemeMediumPriority()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    selectedContentColor = getThemeMediumPriority(),
                    unselectedContentColor = TextGrey
                )
            }
        }
        
        // Tab Content
        when (selectedTab) {
            0 -> LedConfigurationTab()
            1 -> LedColorsTab()
        }
    }
}

@Composable
fun LedConfigurationTab() {
    val themeColors = LocalThemeColors.current
    var brightness by remember { mutableStateOf(0.8f) }
    var pulseSpeed by remember { mutableStateOf(0.5f) }
    var syncWithMusic by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Phone Back Visualizer with theme colors
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                themeColors.highPriority.copy(alpha = 0.20f),
                                themeColors.mediumPriority.copy(alpha = 0.15f),
                                themeColors.lowPriority.copy(alpha = 0.12f),
                                SurfaceBlack
                            ),
                            center = Offset(width * 0.5f, height * 0.3f),
                            radius = width.coerceAtLeast(height) * 1.2f
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        1.dp,
                        themeColors.mediumPriority.copy(alpha = 0.3f),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LedVisualizer(themeColors = themeColors)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controls with theme colors
        ConfigOption(
            title = "Brightness",
            value = brightness,
            onValueChange = { brightness = it },
            themeColor = themeColors.highPriority
        )
        Spacer(modifier = Modifier.height(16.dp))
        ConfigOption(
            title = "Pulse Speed",
            value = pulseSpeed,
            onValueChange = { pulseSpeed = it },
            themeColor = themeColors.mediumPriority
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sync with Music", color = TextWhite, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = syncWithMusic,
                onCheckedChange = { syncWithMusic = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeColors.lowPriority,
                    checkedTrackColor = themeColors.lowPriority.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGrey,
                    uncheckedTrackColor = SurfaceDarkGrey
                )
            )
        }
    }
}

@Composable
fun LedColorsTab() {
    val prefsManager = rememberPreferencesManager()
    val coroutineScope = rememberCoroutineScope()
    var ledTheme by remember { mutableStateOf(LedTheme()) }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.HIGH) }
    var customPresets by remember { mutableStateOf<List<ThemePreset>>(emptyList()) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    
    // Memoize all presets list
    val allPresets = remember(customPresets) {
        ThemePresets.ALL_PRESETS + customPresets
    }
    
    // Memoize current theme colors to avoid recalculation
    val priorityColors = remember(ledTheme) {
        NotificationPriority.values().associateWith { ledTheme.getColorForPriority(it) }
    }
    
    // Track if theme was loaded to avoid saving on initial load
    var isInitialLoad by remember { mutableStateOf(true) }
    
    // Load LED theme and custom presets on first composition
    LaunchedEffect(Unit) {
        ledTheme = prefsManager.getLedTheme()
        customPresets = prefsManager.getCustomPresets()
        isInitialLoad = false
    }
    
    // Save theme when it changes (debounced, but not on initial load)
    LaunchedEffect(ledTheme) {
        if (!isInitialLoad) {
            kotlinx.coroutines.delay(500) // Debounce saves
            prefsManager.saveLedTheme(ledTheme)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Choose a preset or customize your colors",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        }
        
        // Preset Themes Section
        item {
            Text(
                text = "Preset Themes",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(
                    allPresets,
                    key = { preset -> preset.name + preset.isCustom }
                ) { preset ->
                    if (preset.isCustom) {
                        AnimatedDeleteWrapper(
                            onDelete = {
                                customPresets = customPresets.filter { it.name != preset.name }
                                coroutineScope.launch {
                                    prefsManager.saveCustomPresets(customPresets)
                                }
                            }
                        ) { deleteHandler ->
                            PresetThemeItem(
                                preset = preset,
                                isSelected = ledTheme.highPriorityColor == preset.theme.highPriorityColor &&
                                        ledTheme.mediumPriorityColor == preset.theme.mediumPriorityColor &&
                                        ledTheme.lowPriorityColor == preset.theme.lowPriorityColor,
                                onClick = {
                                    ledTheme = preset.theme
                                },
                                onDelete = deleteHandler
                            )
                        }
                    } else {
                        PresetThemeItem(
                            preset = preset,
                            isSelected = ledTheme.highPriorityColor == preset.theme.highPriorityColor &&
                                    ledTheme.mediumPriorityColor == preset.theme.mediumPriorityColor &&
                                    ledTheme.lowPriorityColor == preset.theme.lowPriorityColor,
                            onClick = {
                                ledTheme = preset.theme
                            },
                            onDelete = null
                        )
                    }
                }
            }
        }
        
        item {
            Divider(color = TextGrey.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
        }
        
        // Customization Section
        item {
            Text(
                text = "Customize Colors",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Priority Selector
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NotificationPriority.values().forEach { priority ->
                    val priorityColor = remember(priorityColors, priority) {
                        priorityColors[priority] ?: Color.White
                    }
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priorityColor.copy(alpha = 0.3f),
                            selectedLabelColor = priorityColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Color Picker
        item {
            val selectedColor = remember(priorityColors, selectedPriority) {
                priorityColors[selectedPriority] ?: Color.White
            }
            ColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { color ->
                    val hexColor = LedTheme.colorToHex(color)
                    
                    ledTheme = when (selectedPriority) {
                        NotificationPriority.HIGH -> ledTheme.copy(highPriorityColor = hexColor)
                        NotificationPriority.MEDIUM -> ledTheme.copy(mediumPriorityColor = hexColor)
                        NotificationPriority.LOW -> ledTheme.copy(lowPriorityColor = hexColor)
                    }
                }
            )
        }
        
        // Action Buttons
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassyButton(
                    text = "Save Theme",
                    onClick = {
                        coroutineScope.launch {
                            prefsManager.saveLedTheme(ledTheme)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    color = getThemeMediumPriority()
                )
                
                GlassyButton(
                    text = "Save as Preset",
                    onClick = { showSavePresetDialog = true },
                    modifier = Modifier.weight(1f),
                    color = getThemeLowPriority()
                )
            }
        }
    }
    
    if (showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSave = { name ->
                val newPreset = ThemePreset(name = name, theme = ledTheme, isCustom = true)
                customPresets = customPresets + newPreset
                coroutineScope.launch {
                    prefsManager.saveCustomPresets(customPresets)
                    prefsManager.saveLedTheme(ledTheme)
                }
                showSavePresetDialog = false
            }
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
fun PresetThemeItem(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Memoize colors to avoid recalculation on every recomposition
    val colors = remember(preset.theme) {
        Triple(
            preset.theme.getColorForPriority(NotificationPriority.HIGH),
            preset.theme.getColorForPriority(NotificationPriority.MEDIUM),
            preset.theme.getColorForPriority(NotificationPriority.LOW)
        )
    }
    
    // Read theme colors directly (can't use remember with @Composable functions)
    val themeColors = LocalThemeColors.current
    val backgroundColor = if (isSelected) themeColors.mediumPriority.copy(alpha = 0.2f) else SurfaceDarkGrey.copy(alpha = 0.6f)
    val borderColor = if (isSelected) themeColors.mediumPriority.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(colors.first, colors.second, colors.third).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    )
                }
            }
            
            Column {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                if (preset.isCustom) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGrey
                    )
                }
            }
        }
        
        if (preset.isCustom && onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = getThemeHighPriority())
            }
        }
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Preset", color = TextWhite) },
        text = {
            Column {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g., My Custom Theme") },
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
                    if (presetName.isNotBlank()) {
                        onSave(presetName)
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
fun LedVisualizer(themeColors: ThemeColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Central Ring - using medium priority color
        drawCircle(
            color = themeColors.mediumPriority.copy(alpha = 0.9f),
            radius = 40.dp.toPx(),
            center = Offset(centerX, centerY),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Top Right Strip - using high priority color
        drawLine(
            color = themeColors.highPriority.copy(alpha = 0.9f),
            start = Offset(centerX + 60.dp.toPx(), centerY - 60.dp.toPx()),
            end = Offset(centerX + 100.dp.toPx(), centerY - 120.dp.toPx()),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Bottom Left Strip - using low priority color
        drawLine(
            color = themeColors.lowPriority.copy(alpha = 0.9f),
            start = Offset(centerX - 60.dp.toPx(), centerY + 60.dp.toPx()),
            end = Offset(centerX - 100.dp.toPx(), centerY + 120.dp.toPx()),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Additional accent lines for more visual interest
        // Top Left accent
        drawLine(
            color = themeColors.highPriority.copy(alpha = 0.6f),
            start = Offset(centerX - 60.dp.toPx(), centerY - 60.dp.toPx()),
            end = Offset(centerX - 100.dp.toPx(), centerY - 120.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Bottom Right accent
        drawLine(
            color = themeColors.lowPriority.copy(alpha = 0.6f),
            start = Offset(centerX + 60.dp.toPx(), centerY + 60.dp.toPx()),
            end = Offset(centerX + 100.dp.toPx(), centerY + 120.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// Helper for white neon since it wasn't in Color.kt
val NeonWhite = Color(0xFFFFFFFF)

@Composable
fun ConfigOption(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    themeColor: Color
) {
    Column {
        Text(title, color = TextGrey, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = themeColor,
                activeTrackColor = themeColor,
                inactiveTrackColor = SurfaceDarkGrey
            )
        )
    }
}
