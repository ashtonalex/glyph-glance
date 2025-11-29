package com.example.glyph_glance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    GlyphTheme {
        var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let gradient show through
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceBlack,
                    contentColor = TextWhite
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = getThemeMediumPriority(),
                            selectedTextColor = getThemeMediumPriority(),
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = getThemeMediumPriority().copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.LedConfig,
                        onClick = { currentScreen = Screen.LedConfig },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "LEDs") },
                        label = { Text("LEDs") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = getThemeMediumPriority(),
                            selectedTextColor = getThemeMediumPriority(),
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = getThemeMediumPriority().copy(alpha = 0.1f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // Background gradient that fades from theme colors to black
                ThemeBackgroundGradient()
                
                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen()
                    Screen.LedConfig -> LedConfigScreen()
                }
            }
        }
    }
}

@Composable
fun ThemeBackgroundGradient() {
    val themeColors = LocalThemeColors.current
    val density = LocalDensity.current
    
    // Animate color changes smoothly with decreased opacity
    val animatedHigh by animateColorAsState(
        targetValue = themeColors.highPriority.copy(alpha = 0.06f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "high"
    )
    val animatedMedium by animateColorAsState(
        targetValue = themeColors.mediumPriority.copy(alpha = 0.05f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "medium"
    )
    val animatedLow by animateColorAsState(
        targetValue = themeColors.lowPriority.copy(alpha = 0.04f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "low"
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val gradientHeightPx = screenHeight * 0.25f // Top 25% of screen for lower density
        val gradientHeight = with(density) { gradientHeightPx.toDp() }
        
        // Rectangular gradient at the top - high priority (left side)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            animatedHigh,
                            animatedHigh.copy(alpha = 0.01f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = screenWidth * 0.4f
                    )
                )
        )
        
        // Rectangular gradient at the top - medium priority (center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedMedium,
                            animatedMedium.copy(alpha = 0.01f),
                            Color.Transparent
                        ),
                        startX = screenWidth * 0.3f,
                        endX = screenWidth * 0.7f
                    )
                )
        )
        
        // Rectangular gradient at the top - low priority (right side)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedLow.copy(alpha = 0.01f),
                            animatedLow
                        ),
                        startX = screenWidth * 0.6f,
                        endX = screenWidth
                    )
                )
        )
    }
}

enum class Screen {
    Dashboard,
    LedConfig
}