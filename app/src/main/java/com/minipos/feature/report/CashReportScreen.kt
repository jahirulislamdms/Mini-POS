package com.minipos.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.CashType

/** Cash Management Report (Phase 17): Cash In / Out with day / month / custom filters and totals. */
@Composable
fun CashReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: CashReportViewModel = viewModel()
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
                title = "Cash Management Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("cash-report.pdf", shopId) {
                        "Cash Management Report" to buildList {
                            add(PdfLine.KeyValue("Total Cash In", Money.format(report.cashIn)))
                            add(PdfLine.KeyValue("Total Cash Out", Money.format(report.cashOut)))
                            add(PdfLine.Header("Transactions"))
                            add(PdfLine.Cols(listOf("Date", "Type", "Note", "Amount"), bold = true))
                            report.items.forEach { txn ->
                                val isIn = txn.type == CashType.CASH_IN
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            DateUtil.formatDateTime(txn.createdAt),
                                            if (isIn) "Cash In" else "Cash Out",
                                            txn.note ?: "",
                                            (if (isIn) "+" else "-") + Money.format(txn.amount),
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == CashReportMode.DAY, { vm.setMode(CashReportMode.DAY) }, label = { Text("Day") })
                    FilterChip(mode == CashReportMode.MONTH, { vm.setMode(CashReportMode.MONTH) }, label = { Text("Month") })
                    FilterChip(mode == CashReportMode.CUSTOM, { vm.setMode(CashReportMode.CUSTOM) }, label = { Text("Custom") })
                }

                when (mode) {
                    CashReportMode.DAY ->
                        DateButton("Date", date, vm::setDate, Modifier.fillMaxWidth())
                    CashReportMode.MONTH ->
                        DateButton("Month", date, vm::setDate, Modifier.fillMaxWidth())
                    CashReportMode.CUSTOM ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                            DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                        }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Total Cash In", report.cashIn, IncomeGreen, Modifier.weight(1f))
                    StatCard("Total Cash Out", report.cashOut, ExpenseRed, Modifier.weight(1f))
                }
            }

            if (report.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cash transactions in this period.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(report.items, key = { it.id }) { txn ->
                        val isIn = txn.type == CashType.CASH_IN
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = if (isIn) IncomeGreen else ExpenseRed)
                                Column(Modifier.weight(1f)) {
                                    Text(if (isIn) "Cash In" else "Cash Out", fontWeight = FontWeight.SemiBold)
                                    val sub = buildString {
                                        append(DateUtil.formatDateTime(txn.createdAt))
                                        if (!txn.note.isNullOrBlank()) append(" · ${txn.note}")
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Text(
                                    (if (isIn) "+" else "−") + Money.format(txn.amount),
                                    color = if (isIn) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
