package app.romanmarinov.dochintkmp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    data object AiScanner : Screen("ai_scanner", "AI Сканер", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner)
    data object OfflineScanner : Screen("offline_scanner", "Оффлайн", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner)
    data object Documents : Screen("documents", "Документы", Icons.Filled.Home, Icons.Outlined.Home)
    data object Settings : Screen("settings", "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val items = listOf(AiScanner, OfflineScanner, Documents, Settings)
    }
}
