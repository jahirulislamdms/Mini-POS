package com.minipos.core.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import com.minipos.core.ui.BottomTab

/** Navigation route constants (CONVENTIONS §2 — routes as constants). */
object Routes {
    // Root-level destinations (outside the tab shell)
    const val MAIN = "main"
    const val SHOPS = "shops"
    const val SHOP_FORM = "shop_form"
    const val ARG_SHOP_ID = "shopId"

    const val PRODUCTS = "products"
    const val PRODUCT_FORM = "product_form"
    const val PRODUCT_DETAIL = "product_detail"
    const val UPDATE_STOCK = "update_stock"
    const val CATEGORIES = "categories"
    const val UNITS = "units"
    const val ARG_PRODUCT_ID = "productId"

    const val SALES_LEDGER = "sales_ledger"
    const val SALE_DETAIL = "sale_detail"
    const val ARG_SALE_ID = "saleId"

    const val PURCHASE_LEDGER = "purchase_ledger"
    const val PURCHASE_DETAIL = "purchase_detail"
    const val ARG_PURCHASE_ID = "purchaseId"

    const val EXPENSES = "expenses"
    const val EXPENSE_CATEGORIES = "expense_categories"

    const val DUE_LEDGER = "due_ledger"
    const val PARTY_DETAIL = "party_detail"
    const val ARG_PARTY_ID = "partyId"

    const val STOCK_REPORT = "stock_report"
    const val BUSINESS_REPORT = "business_report"
    const val DAILY_REPORT = "daily_report"

    const val CASH_MANAGEMENT = "cash_management"

    const val BACKUP = "backup"

    // Bottom-tab destinations (inside the shell)
    const val HOME = "home"
    const val SELL = "sell"
    const val BUY = "buy"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    /** Route to the shop form; null id = create, non-null = edit. */
    fun shopForm(shopId: Long?): String =
        if (shopId == null) SHOP_FORM else "$SHOP_FORM?$ARG_SHOP_ID=$shopId"

    /** Route to the product form; null id = create, non-null = edit. */
    fun productForm(productId: Long?): String =
        if (productId == null) PRODUCT_FORM else "$PRODUCT_FORM?$ARG_PRODUCT_ID=$productId"

    /** Route to the read-only product details. */
    fun productDetail(productId: Long): String = "$PRODUCT_DETAIL/$productId"

    /** Route to Sell/Buy pre-loaded with a product (Product Details quick actions). */
    fun sellForProduct(productId: Long): String = "$SELL?$ARG_PRODUCT_ID=$productId"
    fun buyForProduct(productId: Long): String = "$BUY?$ARG_PRODUCT_ID=$productId"

    /** Route to the update-stock screen for a product. */
    fun updateStock(productId: Long): String = "$UPDATE_STOCK/$productId"

    /** Route to a sale's detail. */
    fun saleDetail(saleId: Long): String = "$SALE_DETAIL/$saleId"

    /** Route to a purchase's detail. */
    fun purchaseDetail(purchaseId: Long): String = "$PURCHASE_DETAIL/$purchaseId"

    /** Route to a party's statement. */
    fun partyDetail(partyId: Long): String = "$PARTY_DETAIL/$partyId"
}

/** The five main-shell bottom-bar destinations. */
val bottomTabs: List<BottomTab> = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home),
    BottomTab(Routes.SELL, "Sell", Icons.Filled.Storefront),
    BottomTab(Routes.PRODUCTS, "Products", Icons.Filled.Inventory2),
    BottomTab(Routes.REPORTS, "Reports", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)
