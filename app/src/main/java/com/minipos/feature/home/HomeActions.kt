package com.minipos.feature.home

/** Navigation callbacks the Home dashboard needs (kept as one bundle to avoid a huge param list). */
data class HomeActions(
    val onOpenShops: () -> Unit,
    val onSell: () -> Unit,
    val onBuy: () -> Unit,
    val onOpenCashDrawer: () -> Unit,
    val onOpenSalesLedger: () -> Unit,
    val onOpenPurchaseLedger: () -> Unit,
    val onOpenExpenses: () -> Unit,
    val onOpenDueLedger: () -> Unit,
    val onOpenStockReport: () -> Unit,
    val onOpenBusinessReport: () -> Unit,
    val onOpenSaleDetail: (Long) -> Unit,
    val onOpenPurchaseDetail: (Long) -> Unit,
)
