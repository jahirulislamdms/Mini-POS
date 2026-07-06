package com.minipos.core.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.minipos.feature.buy.BuyScreen
import com.minipos.feature.home.HomeActions
import com.minipos.feature.home.HomeScreen
import com.minipos.feature.product.ProductListScreen
import com.minipos.feature.report.ReportScreen
import com.minipos.feature.sell.SellScreen
import com.minipos.feature.settings.SettingsScreen

/** Inner tab navigation graph (scoped to the active [shopId]). New routes are added per phase. */
@Composable
fun TabNavGraph(
    navController: NavHostController,
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
    modifier: Modifier = Modifier,
) {
    // Switch the bottom-tab selection (used by Home/Settings to open the Products tab).
    val switchTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val onOpenProductsTab = { switchTab(Routes.PRODUCTS) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                shopId = shopId,
                actions = HomeActions(
                    onOpenShops = onOpenShops,
                    onSell = { switchTab(Routes.SELL) },
                    onBuy = { navController.navigate(Routes.BUY) { launchSingleTop = true } },
                    onOpenCashDrawer = onOpenCashDrawer,
                    onOpenSalesLedger = onOpenSalesLedger,
                    onOpenProducts = onOpenProductsTab,
                    onOpenDailyReport = onOpenDailyReport,
                    onOpenBuyReport = onOpenBuyReport,
                    onOpenDueLedger = onOpenDueLedger,
                    onOpenExpenses = onOpenExpenses,
                ),
            )
        }
        composable(Routes.SELL) { SellScreen(shopId = shopId, onOpenSalesLedger = onOpenSalesLedger) }
        composable(Routes.PRODUCTS) {
            ProductListScreen(
                shopId = shopId,
                onAddProduct = { onOpenProductForm(null) },
                onProductDetail = onOpenProductDetail,
                onUpdateStock = onOpenUpdateStock,
                onProductHistory = onOpenProductHistory,
            )
        }
        composable(Routes.BUY) { BuyScreen(shopId = shopId, onOpenPurchaseLedger = onOpenPurchaseLedger) }
        composable(Routes.REPORTS) {
            ReportScreen(
                onOpenStockReport = onOpenStockReport,
                onOpenBusinessReport = onOpenBusinessReport,
                onOpenDailyReport = onOpenDailyReport,
                onOpenCashReport = onOpenCashReport,
                onOpenSalesReport = onOpenSalesReport,
                onOpenBuyReport = onOpenBuyReport,
                onOpenExpenses = onOpenExpenses,
                onOpenCategoryReport = onOpenCategoryReport,
                onOpenDueLedger = onOpenDueLedger,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                shopId = shopId,
                onOpenShops = onOpenShops,
                onOpenProducts = onOpenProductsTab,
                onOpenCategories = onOpenCategories,
                onOpenUnits = onOpenUnits,
                onOpenExpenseCategories = onOpenExpenseCategories,
                onOpenCashManagement = onOpenCashManagement,
                onOpenBackup = onOpenBackup,
                onOpenLicense = onOpenLicense,
                onOpenActivities = onOpenActivities,
                onOpenBarcodePrint = onOpenBarcodePrint,
                onOpenPrinterSettings = onOpenPrinterSettings,
            )
        }
    }
}
