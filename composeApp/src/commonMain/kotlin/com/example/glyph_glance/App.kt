package com.example.glyph_glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
            containerColor = Color.Black,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black,
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
                    NavigationBarItem(
                        selected = currentScreen == Screen.ActivityLogs,
                        onClick = { currentScreen = Screen.ActivityLogs },
                        icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                        label = { Text("Logs") },
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
                    Screen.ActivityLogs -> ActivityLogsScreen()
                }
            }
        }
    }
}

@Composable
fun ThemeBackgroundGradient() {
    // Simple solid black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

enum class Screen {
    Dashboard,
    LedConfig,
    ActivityLogs
}