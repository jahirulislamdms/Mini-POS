package com.minipos.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnYellow

/** One bottom-nav destination. */
data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** Strong-blue bottom tab bar with white items (BUILD_PLAN §4). */
@Composable
fun AppBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
) {
    NavigationBar(containerColor = BrandYellow) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OnYellow,
                    selectedTextColor = OnYellow,
                    indicatorColor = OnYellow.copy(alpha = 0.18f),
                    unselectedIconColor = OnYellow.copy(alpha = 0.6f),
                    unselectedTextColor = OnYellow.copy(alpha = 0.6f),
                ),
            )
        }
    }
}
