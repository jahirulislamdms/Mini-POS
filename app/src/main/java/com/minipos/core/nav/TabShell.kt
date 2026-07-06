package com.minipos.core.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minipos.core.theme.AppBackground
import com.minipos.core.ui.AppBottomBar

/** The tab shell for the active shop: bottom bar + tab nav host. */
@Composable
fun TabShell(
    shopId: Long,
    onOpenShops: () -> Unit,
    onOpenProductDetail: (Long) -> Unit,
    onOpenProductForm: (Long?) -> Unit,
    onOpenUpdateStock: (Long) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenSalesLedger: () -> Unit,
    onOpenPurchaseLedger: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenExpenseCategories: () -> Unit,
    onOpenDueLedger: () -> Unit,
    onOpenStockReport: () -> Unit,
    onOpenBusinessReport: () -> Unit,
    onOpenSaleDetail: (Long) -> Unit,
    onOpenPurchaseDetail: (Long) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCashManagement: () -> Unit,
    onOpenDailyReport: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenProductHistory: (Long) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCashReport: () -> Unit,
    onOpenBuyReport: () -> Unit,
    onOpenSalesReport: () -> Unit,
    onOpenCategoryReport: () -> Unit,
    onOpenCashDrawer: () -> Unit,
    onOpenBarcodePrint: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = AppBackground,
        // Phase 25: don't consume the status-bar inset here — each tab handles it, so screen
        // chrome (Home's yellow header / the yellow AppTopBars) extends behind the status bar.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
        bottomBar = {
            AppBottomBar(
                tabs = bottomTabs,
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        TabNavGraph(
            navController = navController,
            shopId = shopId,
            onOpenShops = onOpenShops,
            onOpenProductDetail = onOpenProductDetail,
            onOpenProductForm = onOpenProductForm,
            onOpenUpdateStock = onOpenUpdateStock,
            onOpenCategories = onOpenCategories,
            onOpenUnits = onOpenUnits,
            onOpenSalesLedger = onOpenSalesLedger,
            onOpenPurchaseLedger = onOpenPurchaseLedger,
            onOpenExpenses = onOpenExpenses,
            onOpenExpenseCategories = onOpenExpenseCategories,
            onOpenDueLedger = onOpenDueLedger,
            onOpenStockReport = onOpenStockReport,
            onOpenBusinessReport = onOpenBusinessReport,
            onOpenSaleDetail = onOpenSaleDetail,
            onOpenPurchaseDetail = onOpenPurchaseDetail,
            onOpenBackup = onOpenBackup,
            onOpenCashManagement = onOpenCashManagement,
            onOpenDailyReport = onOpenDailyReport,
            onOpenLicense = onOpenLicense,
            onOpenProductHistory = onOpenProductHistory,
            onOpenActivities = onOpenActivities,
            onOpenCashReport = onOpenCashReport,
            onOpenBuyReport = onOpenBuyReport,
            onOpenSalesReport = onOpenSalesReport,
            onOpenCategoryReport = onOpenCategoryReport,
            onOpenCashDrawer = onOpenCashDrawer,
            onOpenBarcodePrint = onOpenBarcodePrint,
            onOpenPrinterSettings = onOpenPrinterSettings,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
