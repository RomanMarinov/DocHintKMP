package app.romanmarinov.dochintkmp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.OcrAiScannerScreen
import app.romanmarinov.dochintkmp.presentation.documents_screen.ResultsScreen
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerScreen
import app.romanmarinov.dochintkmp.presentation.settings_screen.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.iconFilled else screen.iconOutlined,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AiScanner.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Screen.AiScanner.route) { OcrAiScannerScreen() }
            composable(Screen.OfflineScanner.route) { OcrOfflineScannerScreen() }
            composable(Screen.Documents.route) { ResultsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
