package com.example.glyph_glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
                // Solid light grey background
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
    // Simple solid dark grey background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBlack)
    )
}

enum class Screen {
    Dashboard,
    LedConfig
}