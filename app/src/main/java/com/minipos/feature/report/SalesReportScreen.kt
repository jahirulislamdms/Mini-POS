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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.print.PdfLine
import com.minipos.core.print.ReportPdfAction
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

/** Sales Report (Phase 35): all sales (Cash + Due) by Day / Month / Custom range, with a total. */
@Composable
fun SalesReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: SalesReportViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val mode by vm.mode.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val rangeStart by vm.rangeStart.collectAsStateWithLifecycle()
    val rangeEnd by vm.rangeEnd.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Sales Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("sales-report.pdf", shopId) {
                        "Sales Report" to buildList {
                            add(PdfLine.KeyValue("Total Sales", Money.format(report.totalSales)))
                            add(PdfLine.KeyValue("Total Profit", Money.format(report.totalProfit)))
                            add(PdfLine.Header("Sales (${report.sales.size})"))
                            report.sales.forEach { entry ->
                                add(
                                    PdfLine.Cols(
                                        listOf(DateUtil.formatDateTime(entry.createdAt), entry.invoiceNo, Money.format(entry.total)),
                                        bold = true,
                                    ),
                                )
                                entry.customerName?.let {
                                    add(PdfLine.Cols(listOf("  Customer: $it", "", "")))
                                }
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            "  ${entry.paymentLabel}",
                                            "Paid ${Money.format(entry.paid)}",
                                            if (entry.due > 0) "Due ${Money.format(entry.due)}" else "",
                                        ),
                                    ),
                                )
                                // Phase 37: every product sold in the invoice.
                                entry.lines.forEach { line ->
                                    add(
                                        PdfLine.Cols(
                                            listOf(
                                                "  ${line.name}",
                                                "${line.quantity.qtyText()} x ${Money.format(line.unitPrice)}",
                                                Money.format(line.lineTotal),
                                            ),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == SalesReportMode.DAY, { vm.setMode(SalesReportMode.DAY) }, label = { Text("Day") })
                    FilterChip(mode == SalesReportMode.MONTH, { vm.setMode(SalesReportMode.MONTH) }, label = { Text("Month") })
                    FilterChip(mode == SalesReportMode.CUSTOM, { vm.setMode(SalesReportMode.CUSTOM) }, label = { Text("Custom") })
                }
            }
            item {
                when (mode) {
                    SalesReportMode.DAY -> DateButton("Date", date, vm::setDate, Modifier.fillMaxWidth())
                    SalesReportMode.MONTH -> DateButton("Month", date, vm::setDate, Modifier.fillMaxWidth())
                    SalesReportMode.CUSTOM -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                        DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                    }
                }
            }

            item {
                // Phase 37: same period totals logic as the ledger/reports (profit via existing SQL).
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Total Sales", report.totalSales, IncomeGreen, Modifier.weight(1f))
                    StatCard("Total Profit", report.totalProfit, OnSurface, Modifier.weight(1f))
                }
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
                items(report.sales, key = { it.id }) { entry -> SaleCard(entry) }
            }
        }
    }
}

@Composable
private fun SaleCard(entry: SaleReportEntry) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(DateUtil.formatDateTime(entry.createdAt), fontWeight = FontWeight.SemiBold)
                Text(entry.invoiceNo, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                entry.customerName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(entry.total), color = IncomeGreen, fontWeight = FontWeight.Bold)
                Text(entry.paymentLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        // Phase 37: every product sold in this invoice, ledger-style (name, qty × price, total).
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
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Paid ${Money.format(entry.paid)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.weight(1f),
            )
            if (entry.due > 0) {
                Text(
                    "Due ${Money.format(entry.due)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseRed,
                )
            }
        }
    }
}

private fun Double.qtyText(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
