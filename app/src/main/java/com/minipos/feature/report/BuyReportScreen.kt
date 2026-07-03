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
import com.minipos.core.print.PdfLine
import com.minipos.core.print.ReportPdfAction
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money

/** Buy Report (Phase 19): all purchases (Cash + Due) by Day / Month / Custom range, with a total. */
@Composable
fun BuyReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: BuyReportViewModel = viewModel()
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
                title = "Buy Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("buy-report.pdf", shopId) {
                        "Buy Report" to buildList {
                            add(PdfLine.KeyValue("Total Buy Amount", Money.format(report.totalBuy)))
                            add(PdfLine.Header("Purchases (${report.purchases.size})"))
                            report.purchases.forEach { entry ->
                                add(
                                    PdfLine.Cols(
                                        listOf(DateUtil.formatDateTime(entry.createdAt), entry.paymentLabel, Money.format(entry.total)),
                                        bold = true,
                                    ),
                                )
                                entry.lines.forEach { line ->
                                    add(
                                        PdfLine.Cols(
                                            listOf(
                                                "  ${line.name}",
                                                "${line.quantity} x ${Money.format(line.unitPrice)}",
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
                    FilterChip(mode == BuyReportMode.DAY, { vm.setMode(BuyReportMode.DAY) }, label = { Text("Day") })
                    FilterChip(mode == BuyReportMode.MONTH, { vm.setMode(BuyReportMode.MONTH) }, label = { Text("Month") })
                    FilterChip(mode == BuyReportMode.CUSTOM, { vm.setMode(BuyReportMode.CUSTOM) }, label = { Text("Custom") })
                }
            }
            item {
                when (mode) {
                    BuyReportMode.DAY -> DateButton("Date", date, vm::setDate, Modifier.fillMaxWidth())
                    BuyReportMode.MONTH -> DateButton("Month", date, vm::setDate, Modifier.fillMaxWidth())
                    BuyReportMode.CUSTOM -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                        DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                    }
                }
            }

            item {
                StatCard("Total Buy Amount", report.totalBuy, ExpenseRed, Modifier.fillMaxWidth())
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
                items(report.purchases, key = { it.id }) { entry -> PurchaseCard(entry) }
            }
        }
    }
}

@Composable
private fun PurchaseCard(entry: TxnEntry) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(DateUtil.formatDateTime(entry.createdAt), fontWeight = FontWeight.SemiBold)
                Text(entry.paymentLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(Money.format(entry.total), color = ExpenseRed, fontWeight = FontWeight.Bold)
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
