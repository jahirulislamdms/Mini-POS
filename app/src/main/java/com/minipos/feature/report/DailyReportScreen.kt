package com.minipos.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money

/** Daily Transactions Report: sales + purchases for a single day or a custom range (≤ 1 month). */
@Composable
fun DailyReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: DailyReportViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val report by vm.report.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val singleDate by vm.singleDate.collectAsStateWithLifecycle()
    val rangeStart by vm.rangeStart.collectAsStateWithLifecycle()
    val rangeEnd by vm.rangeEnd.collectAsStateWithLifecycle()
    val clamped by vm.rangeClamped.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Daily Transactions", onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == DailyMode.SINGLE, { vm.setMode(DailyMode.SINGLE) }, label = { Text("Single day") })
                    FilterChip(mode == DailyMode.RANGE, { vm.setMode(DailyMode.RANGE) }, label = { Text("Date range") })
                }
            }
            item {
                if (mode == DailyMode.SINGLE) {
                    DateButton("Date", singleDate, vm::setSingleDate, Modifier.fillMaxWidth())
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                            DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                        }
                        if (clamped) {
                            Text(
                                "Range limited to 1 month from the start date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed,
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Total sales", report.totalSales, IncomeGreen, Modifier.weight(1f))
                    StatCard("Total purchases", report.totalPurchases, ExpenseRed, Modifier.weight(1f))
                }
            }
            item {
                StatCard(
                    if (report.profit >= 0) "Profit" else "Loss",
                    report.profit,
                    if (report.profit >= 0) IncomeGreen else ExpenseRed,
                    Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    "Sales (${report.sales.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (report.sales.isEmpty()) {
                item { Text("No sales in this period.", color = TextMuted) }
            } else {
                items(report.sales, key = { "s_${it.id}" }) { entry -> TxnCard(entry, isSale = true) }
            }

            item {
                Text(
                    "Purchases (${report.purchases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (report.purchases.isEmpty()) {
                item { Text("No purchases in this period.", color = TextMuted) }
            } else {
                items(report.purchases, key = { "p_${it.id}" }) { entry -> TxnCard(entry, isSale = false) }
            }
        }
    }
}

@Composable
private fun TxnCard(entry: TxnEntry, isSale: Boolean) {
    val color = if (isSale) IncomeGreen else ExpenseRed
    AppCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(DateUtil.formatDateTime(entry.createdAt), fontWeight = FontWeight.SemiBold)
                Text(entry.paymentLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(Money.format(entry.total), color = color, fontWeight = FontWeight.Bold)
        }
        if (entry.lines.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            entry.lines.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${line.quantity.qtyText()} × ${Money.format(line.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Text(Money.format(line.lineTotal), color = OnSurface)
                }
            }
        }
    }
}

private fun Double.qtyText(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
