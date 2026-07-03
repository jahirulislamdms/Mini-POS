package com.minipos.feature.cashdrawer

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
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.repo.CashDrawerDay

/**
 * Cash Drawer (Phase 27): a business day's cash movement — Opening Cash (editable), Cash Sales,
 * Customer Due Collections, Cash In/Out, Expenses and the computed Closing Cash — plus history
 * (Month / Custom range). Independent from the Current Balance; buying never appears here.
 */
@Composable
fun CashDrawerScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: CashDrawerViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val mode by vm.mode.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val rangeStart by vm.rangeStart.collectAsStateWithLifecycle()
    val rangeEnd by vm.rangeEnd.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val dayTransactions by vm.dayTransactions.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Cash Drawer",
                onBack = onBack,
                actions = {
                    ReportPdfAction("cash-drawer.pdf", shopId) {
                        "Cash Drawer Report" to buildList {
                            if (mode == DrawerMode.DAY) {
                                day?.let { d ->
                                    add(PdfLine.Plain(DateUtil.formatDate(d.dayStart)))
                                    add(PdfLine.KeyValue("Opening Cash", Money.format(d.opening)))
                                    add(PdfLine.KeyValue("Cash Sales", Money.format(d.cashSales)))
                                    add(PdfLine.KeyValue("Customer Due Collections", Money.format(d.dueCollections)))
                                    add(PdfLine.KeyValue("Cash In", Money.format(d.cashIn)))
                                    add(PdfLine.KeyValue("Cash Out", Money.format(d.cashOut)))
                                    add(PdfLine.KeyValue("Expenses", Money.format(d.expenses)))
                                    add(PdfLine.Divider)
                                    add(PdfLine.KeyValue("Closing Cash", Money.format(d.closing)))
                                    add(PdfLine.Header("Transactions"))
                                    dayTransactions.forEach { row ->
                                        add(
                                            PdfLine.Cols(
                                                listOf(
                                                    "${row.txn.type}${row.txn.description?.let { " · $it" } ?: ""}",
                                                    DateUtil.formatDateTime(row.txn.createdAt),
                                                    Money.format(row.txn.amount),
                                                    "Bal: ${Money.format(row.balanceAfter)}",
                                                ),
                                            ),
                                        )
                                    }
                                }
                            } else {
                                add(PdfLine.Cols(listOf("Date", "Opening", "In", "Out", "Closing"), bold = true))
                                history.forEach { d ->
                                    add(
                                        PdfLine.Cols(
                                            listOf(
                                                DateUtil.formatDate(d.dayStart),
                                                Money.format(d.opening),
                                                Money.format(d.cashSales + d.dueCollections + d.cashIn),
                                                Money.format(d.cashOut + d.expenses),
                                                Money.format(d.closing),
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
                    FilterChip(mode == DrawerMode.DAY, { vm.setMode(DrawerMode.DAY) }, label = { Text("Day") })
                    FilterChip(mode == DrawerMode.MONTH, { vm.setMode(DrawerMode.MONTH) }, label = { Text("Month") })
                    FilterChip(mode == DrawerMode.CUSTOM, { vm.setMode(DrawerMode.CUSTOM) }, label = { Text("Custom") })
                }
            }
            item {
                when (mode) {
                    DrawerMode.DAY -> DateButton("Date", date, vm::setDate, Modifier.fillMaxWidth())
                    DrawerMode.MONTH -> DateButton("Month", date, vm::setDate, Modifier.fillMaxWidth())
                    DrawerMode.CUSTOM -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                        DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                    }
                }
            }

            if (mode == DrawerMode.DAY) {
                day?.let { d ->
                    item {
                        // Opening Cash is not editable — always carried forward automatically.
                        DrawerCard(
                            day = d,
                            title = DateUtil.formatDate(d.dayStart),
                            openingAuto = true,
                        )
                    }
                    item { SectionHeader("Transactions") }
                    if (dayTransactions.isEmpty()) {
                        item { Text("No cash transactions this day.", color = TextMuted) }
                    } else {
                        items(dayTransactions, key = { it.txn.key }) { row -> TxnCard(row) }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    item { Text("No cash activity in this period.", color = TextMuted) }
                } else {
                    items(history, key = { it.dayStart }) { d ->
                        DrawerCard(day = d, title = DateUtil.formatDate(d.dayStart))
                    }
                }
            }
        }
    }

}

/** One day's drawer breakdown (Opening Cash is read-only — always carried forward). */
@Composable
private fun DrawerCard(
    day: CashDrawerDay,
    title: String,
    openingAuto: Boolean = false,
) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Opening Cash", color = TextMuted, modifier = Modifier.weight(1f))
            Text(Money.format(day.opening), color = OnSurface, fontWeight = FontWeight.SemiBold)
        }
        if (openingAuto) {
            Text(
                "Carried forward from the previous day's closing.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        DrawerLine("Cash Sales", day.cashSales, IncomeGreen)
        DrawerLine("Customer Due Collections", day.dueCollections, IncomeGreen)
        DrawerLine("Cash In", day.cashIn, IncomeGreen)
        DrawerLine("Cash Out", day.cashOut, ExpenseRed)
        DrawerLine("Expenses", day.expenses, ExpenseRed)

        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Closing Cash", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                Money.format(day.closing),
                color = if (day.closing >= 0) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DrawerLine(label: String, amount: Long, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(Money.format(amount), color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** One cash transaction of the day: type, description, time, signed amount + running balance. */
@Composable
private fun TxnCard(row: DrawerTxnRow) {
    val positive = row.txn.amount >= 0
    AppCard {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(row.txn.type, fontWeight = FontWeight.SemiBold)
                if (!row.txn.description.isNullOrBlank()) {
                    Text(row.txn.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Text(
                    DateUtil.formatDateTime(row.txn.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    (if (positive) "+" else "−") + Money.format(kotlin.math.abs(row.txn.amount)),
                    color = if (positive) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Bal: ${Money.format(row.balanceAfter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }
    }
}

