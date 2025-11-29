package com.example.glyph_glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.glyph_glance.logic.RulesRepository
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main App composable with navigation between screens.
 * 
 * @param rulesRepository Optional rules repository for the Rules screen (null in mock/preview mode)
 * @param showOnboarding Whether to show the onboarding/permission wizard first
 * @param onboardingContent Composable content for the onboarding screen (platform-specific)
 */
@Composable
@Preview
fun App(
    rulesRepository: RulesRepository? = null,
    showOnboarding: Boolean = false,
    onboardingContent: (@Composable (onComplete: () -> Unit) -> Unit)? = null
) {
    var onboardingComplete by remember { mutableStateOf(!showOnboarding) }
    
    GlyphTheme {
        if (!onboardingComplete && onboardingContent != null) {
            onboardingContent { onboardingComplete = true }
        } else {
            MainAppContent(rulesRepository = rulesRepository)
        }
    }
}

@Composable
private fun MainAppContent(rulesRepository: RulesRepository? = null) {
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
                    selected = currentScreen == Screen.Terminal,
                    onClick = { currentScreen = Screen.Terminal },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        unselectedIconColor = TextGrey,
                        unselectedTextColor = TextGrey,
                        indicatorColor = NeonGreen.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Rules,
                    onClick = { currentScreen = Screen.Rules },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = "Rules") },
                    label = { Text("Rules") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonPurple,
                        selectedTextColor = NeonPurple,
                        unselectedIconColor = TextGrey,
                        unselectedTextColor = TextGrey,
                        indicatorColor = NeonPurple.copy(alpha = 0.1f)
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
                Screen.Terminal -> TerminalScreen()
                Screen.Rules -> RulesScreen(rulesRepository = rulesRepository)
                Screen.LedConfig -> LedConfigScreen()
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
    Terminal,
    Rules,
    LedConfig
}
    LedConfig,
    ActivityLogs
}
