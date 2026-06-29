package com.minipos.core.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minipos.ServiceLocator
import com.minipos.feature.backup.BackupScreen
import com.minipos.feature.buy.BuyScreen
import com.minipos.feature.cash.CashManagementScreen
import com.minipos.feature.category.CategoryScreen
import com.minipos.feature.category.UnitScreen
import com.minipos.feature.due.DueLedgerScreen
import com.minipos.feature.due.PartyDetailScreen
import com.minipos.feature.expense.ExpenseCategoryScreen
import com.minipos.feature.expense.ExpenseScreen
import com.minipos.feature.product.ProductDetailScreen
import com.minipos.feature.product.ProductFormScreen
import com.minipos.feature.product.UpdateStockScreen
import com.minipos.feature.sell.SellScreen
import com.minipos.feature.purchaseledger.PurchaseDetailScreen
import com.minipos.feature.report.BusinessReportScreen
import com.minipos.feature.report.DailyReportScreen
import com.minipos.feature.report.StockReportScreen
import com.minipos.feature.purchaseledger.PurchaseLedgerScreen
import com.minipos.feature.salesledger.SaleDetailScreen
import com.minipos.feature.salesledger.SalesLedgerScreen
import com.minipos.feature.shop.FirstRunSetupScreen
import com.minipos.feature.shop.ShopFormScreen
import com.minipos.feature.shop.ShopSwitcherScreen

/** First-run gate: until a shop is selected, the app shows onboarding instead of the shell. */
private sealed interface ShopGate {
    data object Loading : ShopGate
    data object None : ShopGate
    data class Selected(val shopId: Long) : ShopGate
}

/**
 * App root. Invariant (maintained by ShopViewModel): currentShopId is null IFF there are zero
 * shops. So a null id means first run -> onboarding; otherwise we show the shell for that shop.
 */
@Composable
fun AppRoot() {
    val gate by produceState<ShopGate>(ShopGate.Loading) {
        ServiceLocator.currentShopManager.currentShopId.collect { id ->
            value = if (id == null) ShopGate.None else ShopGate.Selected(id)
        }
    }

    when (val g = gate) {
        ShopGate.Loading -> LoadingScreen()
        ShopGate.None -> FirstRunSetupScreen()
        is ShopGate.Selected -> key(g.shopId) { MainScaffold(shopId = g.shopId) }
    }
}

/** Root nav graph for the selected shop: the tab shell plus the shop switcher & form. */
@Composable
private fun MainScaffold(shopId: Long) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            TabShell(
                shopId = shopId,
                onOpenShops = { navController.navigate(Routes.SHOPS) },
                onOpenProductDetail = { id -> navController.navigate(Routes.productDetail(id)) },
                onOpenProductForm = { id -> navController.navigate(Routes.productForm(id)) },
                onOpenUpdateStock = { id -> navController.navigate(Routes.updateStock(id)) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onOpenUnits = { navController.navigate(Routes.UNITS) },
                onOpenSalesLedger = { navController.navigate(Routes.SALES_LEDGER) },
                onOpenPurchaseLedger = { navController.navigate(Routes.PURCHASE_LEDGER) },
                onOpenExpenses = { navController.navigate(Routes.EXPENSES) },
                onOpenExpenseCategories = { navController.navigate(Routes.EXPENSE_CATEGORIES) },
                onOpenDueLedger = { navController.navigate(Routes.DUE_LEDGER) },
                onOpenStockReport = { navController.navigate(Routes.STOCK_REPORT) },
                onOpenBusinessReport = { navController.navigate(Routes.BUSINESS_REPORT) },
                onOpenSaleDetail = { id -> navController.navigate(Routes.saleDetail(id)) },
                onOpenPurchaseDetail = { id -> navController.navigate(Routes.purchaseDetail(id)) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenCashManagement = { navController.navigate(Routes.CASH_MANAGEMENT) },
                onOpenDailyReport = { navController.navigate(Routes.DAILY_REPORT) },
            )
        }
        composable(Routes.SHOPS) {
            ShopSwitcherScreen(
                onBack = { navController.popBackStack() },
                onAddShop = { navController.navigate(Routes.shopForm(null)) },
                onEditShop = { id -> navController.navigate(Routes.shopForm(id)) },
            )
        }
        composable(
            route = "${Routes.SHOP_FORM}?${Routes.ARG_SHOP_ID}={${Routes.ARG_SHOP_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_SHOP_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val editingId = entry.arguments?.getString(Routes.ARG_SHOP_ID)?.toLongOrNull()
            ShopFormScreen(
                editingId = editingId,
                firstRun = false,
                onClose = { navController.popBackStack() },
            )
        }

        composable(
            route = "${Routes.PRODUCT_DETAIL}/{${Routes.ARG_PRODUCT_ID}}",
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.LongType }),
        ) { entry ->
            val productId = entry.arguments?.getLong(Routes.ARG_PRODUCT_ID) ?: return@composable
            ProductDetailScreen(
                shopId = shopId,
                productId = productId,
                onBack = { navController.popBackStack() },
                onSell = { navController.navigate(Routes.sellForProduct(productId)) },
                onBuy = { navController.navigate(Routes.buyForProduct(productId)) },
                onEdit = { navController.navigate(Routes.productForm(productId)) },
            )
        }
        composable(
            route = "${Routes.PRODUCT_FORM}?${Routes.ARG_PRODUCT_ID}={${Routes.ARG_PRODUCT_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_PRODUCT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val productId = entry.arguments?.getString(Routes.ARG_PRODUCT_ID)?.toLongOrNull()
            ProductFormScreen(
                shopId = shopId,
                editingId = productId,
                onClose = { navController.popBackStack() },
                // After delete, also pop the now-stale Product Details page beneath the form.
                onDeleted = {
                    navController.popBackStack()
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "${Routes.UPDATE_STOCK}/{${Routes.ARG_PRODUCT_ID}}",
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.LongType }),
        ) { entry ->
            val productId = entry.arguments?.getLong(Routes.ARG_PRODUCT_ID) ?: return@composable
            UpdateStockScreen(
                shopId = shopId,
                productId = productId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "${Routes.SELL}?${Routes.ARG_PRODUCT_ID}={${Routes.ARG_PRODUCT_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_PRODUCT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val pid = entry.arguments?.getString(Routes.ARG_PRODUCT_ID)?.toLongOrNull()
            SellScreen(
                shopId = shopId,
                onOpenSalesLedger = { navController.navigate(Routes.SALES_LEDGER) },
                onBack = { navController.popBackStack() },
                initialProductId = pid,
            )
        }
        composable(
            route = "${Routes.BUY}?${Routes.ARG_PRODUCT_ID}={${Routes.ARG_PRODUCT_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_PRODUCT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val pid = entry.arguments?.getString(Routes.ARG_PRODUCT_ID)?.toLongOrNull()
            BuyScreen(
                shopId = shopId,
                onOpenPurchaseLedger = { navController.navigate(Routes.PURCHASE_LEDGER) },
                onBack = { navController.popBackStack() },
                initialProductId = pid,
            )
        }

        composable(Routes.CATEGORIES) {
            CategoryScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }
        composable(Routes.UNITS) {
            UnitScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }

        composable(Routes.SALES_LEDGER) {
            SalesLedgerScreen(
                shopId = shopId,
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.saleDetail(id)) },
            )
        }
        composable(
            route = "${Routes.SALE_DETAIL}/{${Routes.ARG_SALE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_SALE_ID) { type = NavType.LongType }),
        ) { entry ->
            val saleId = entry.arguments?.getLong(Routes.ARG_SALE_ID) ?: return@composable
            SaleDetailScreen(saleId = saleId, onBack = { navController.popBackStack() })
        }

        composable(Routes.PURCHASE_LEDGER) {
            PurchaseLedgerScreen(
                shopId = shopId,
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.purchaseDetail(id)) },
            )
        }
        composable(
            route = "${Routes.PURCHASE_DETAIL}/{${Routes.ARG_PURCHASE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_PURCHASE_ID) { type = NavType.LongType }),
        ) { entry ->
            val purchaseId = entry.arguments?.getLong(Routes.ARG_PURCHASE_ID) ?: return@composable
            PurchaseDetailScreen(purchaseId = purchaseId, onBack = { navController.popBackStack() })
        }

        composable(Routes.EXPENSES) {
            ExpenseScreen(
                shopId = shopId,
                onBack = { navController.popBackStack() },
                onManageCategories = { navController.navigate(Routes.EXPENSE_CATEGORIES) },
            )
        }
        composable(Routes.EXPENSE_CATEGORIES) {
            ExpenseCategoryScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }

        composable(Routes.DUE_LEDGER) {
            DueLedgerScreen(
                shopId = shopId,
                onBack = { navController.popBackStack() },
                onOpenParty = { id -> navController.navigate(Routes.partyDetail(id)) },
            )
        }
        composable(
            route = "${Routes.PARTY_DETAIL}/{${Routes.ARG_PARTY_ID}}",
            arguments = listOf(navArgument(Routes.ARG_PARTY_ID) { type = NavType.LongType }),
        ) { entry ->
            val partyId = entry.arguments?.getLong(Routes.ARG_PARTY_ID) ?: return@composable
            PartyDetailScreen(shopId = shopId, partyId = partyId, onBack = { navController.popBackStack() })
        }

        composable(Routes.STOCK_REPORT) {
            StockReportScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }
        composable(Routes.BUSINESS_REPORT) {
            BusinessReportScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }

        composable(Routes.BACKUP) {
            BackupScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }

        composable(Routes.CASH_MANAGEMENT) {
            CashManagementScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }
        composable(Routes.DAILY_REPORT) {
            DailyReportScreen(shopId = shopId, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
