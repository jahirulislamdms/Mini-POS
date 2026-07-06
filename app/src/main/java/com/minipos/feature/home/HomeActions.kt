package com.minipos.feature.home

/** Navigation callbacks the Home dashboard needs (kept as one bundle to avoid a huge param list). */
data class HomeActions(
    val onOpenShops: () -> Unit,
    val onSell: () -> Unit,
    val onBuy: () -> Unit,
    val onOpenCashDrawer: () -> Unit,
    val onOpenSalesLedger: () -> Unit,
    val onOpenProducts: () -> Unit,
    val onOpenDailyReport: () -> Unit,
    val onOpenBuyReport: () -> Unit,
    val onOpenDueLedger: () -> Unit,
    val onOpenExpenses: () -> Unit,
)
