package com.minipos.feature.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar

/** Reports hub linking to every report + the Due Ledger (P9; Due Ledger moved here in Phase 34). */
@Composable
fun ReportScreen(
    onOpenStockReport: () -> Unit,
    onOpenBusinessReport: () -> Unit,
    onOpenDailyReport: () -> Unit,
    onOpenCashReport: () -> Unit,
    onOpenSalesReport: () -> Unit,
    onOpenBuyReport: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenCategoryReport: () -> Unit,
    onOpenDueLedger: () -> Unit,
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Reports") },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportEntry(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "Daily Transactions Report",
                subtitle = "Sales & purchases by day or range (up to 1 month)",
                onClick = onOpenDailyReport,
            )
            ReportEntry(
                icon = Icons.Filled.Inventory2,
                title = "Stock Report",
                subtitle = "Units, stock value, per-product & movements",
                onClick = onOpenStockReport,
            )
            ReportEntry(
                icon = Icons.Filled.Assessment,
                title = "Business Report",
                subtitle = "Money in/out, net balance & profit",
                onClick = onOpenBusinessReport,
            )
            ReportEntry(
                icon = Icons.Filled.Payments,
                title = "Cash Management Report",
                subtitle = "Cash In / Out by day, month or custom range",
                onClick = onOpenCashReport,
            )
            // Phase 35: dedicated Sales Report (same structure as the Buy Report).
            ReportEntry(
                icon = Icons.Filled.Sell,
                title = "Sales Report",
                subtitle = "All sales (Cash & Due) by day, month or custom range",
                onClick = onOpenSalesReport,
            )
            ReportEntry(
                icon = Icons.Filled.ShoppingBag,
                title = "Buy Report",
                subtitle = "All purchases (Cash & Due) by day, month or custom range",
                onClick = onOpenBuyReport,
            )
            // Phase 35: moved here from Settings → Money — same screen, same behavior.
            ReportEntry(
                icon = Icons.Filled.Receipt,
                title = "Expense Report",
                subtitle = "Record & review expenses",
                onClick = onOpenExpenses,
            )
            ReportEntry(
                icon = Icons.Filled.Category,
                title = "Category Report",
                subtitle = "Buy/sell quantity, amounts & profit by category",
                onClick = onOpenCategoryReport,
            )
            // Phase 34: moved here from Settings — same screen, same behavior.
            ReportEntry(
                icon = Icons.Filled.People,
                title = "Due Ledger (Baki)",
                subtitle = "Parties, balances & payments",
                onClick = onOpenDueLedger,
            )
        }
    }
}

@Composable
private fun ReportEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = BrandYellow, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
        }
    }
}
