package com.minipos.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.minipos.core.ui.FilterChipsRow
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.DateFilter
import com.minipos.core.util.Money

/** Business Report: money in/out, net balance and profit, date-filtered (P9.2). */
@Composable
fun BusinessReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: BusinessReportViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val report by vm.report.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val customStart by vm.customStart.collectAsStateWithLifecycle()
    val customEnd by vm.customEnd.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Business Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("business-report.pdf", shopId) {
                        val moneyIn = report.cashSale + report.dueCollected + report.otherIncome
                        val moneyOut = report.cashPurchase + report.duePaid + report.otherExpense
                        "Business Report" to buildList {
                            add(PdfLine.Plain("Period: ${filter.label}"))
                            add(PdfLine.Header("Money in"))
                            add(PdfLine.KeyValue("Cash sale", Money.format(report.cashSale)))
                            add(PdfLine.KeyValue("Due collected", Money.format(report.dueCollected)))
                            add(PdfLine.KeyValue("Other income", Money.format(report.otherIncome)))
                            add(PdfLine.KeyValue("Total in", Money.format(moneyIn)))
                            add(PdfLine.Header("Money out"))
                            add(PdfLine.KeyValue("Cash purchase", Money.format(report.cashPurchase)))
                            add(PdfLine.KeyValue("Due paid", Money.format(report.duePaid)))
                            add(PdfLine.KeyValue("Other expense", Money.format(report.otherExpense)))
                            add(PdfLine.KeyValue("Total out", Money.format(moneyOut)))
                            add(PdfLine.Divider)
                            add(PdfLine.KeyValue("Net balance", Money.format(report.netBalance)))
                            add(PdfLine.Header("Profit"))
                            add(PdfLine.KeyValue("Cash profit", Money.format(report.profitCash)))
                            add(PdfLine.KeyValue("Due profit", Money.format(report.profitDue)))
                            add(PdfLine.KeyValue("Total profit", Money.format(report.profitTotal)))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChipsRow(selected = filter, onSelected = vm::setFilter)
            if (filter == DateFilter.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton("From", customStart, vm::setCustomStart, Modifier.weight(1f))
                    DateButton("To", customEnd, vm::setCustomEnd, Modifier.weight(1f))
                }
            }

            val moneyIn = report.cashSale + report.dueCollected + report.otherIncome
            AppCard {
                SectionHeader("Money in")
                LineRow("Cash sale", report.cashSale, IncomeGreen)
                LineRow("Due collected", report.dueCollected, IncomeGreen)
                LineRow("Other income", report.otherIncome, IncomeGreen)
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                LineRow("Total in", moneyIn, IncomeGreen, bold = true)
            }

            val moneyOut = report.cashPurchase + report.duePaid + report.otherExpense
            AppCard {
                SectionHeader("Money out")
                LineRow("Cash purchase", report.cashPurchase, ExpenseRed)
                LineRow("Due paid", report.duePaid, ExpenseRed)
                LineRow("Other expense", report.otherExpense, ExpenseRed)
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                LineRow("Total out", moneyOut, ExpenseRed, bold = true)
            }

            AppCard {
                LineRow(
                    "Net balance",
                    report.netBalance,
                    if (report.netBalance >= 0) IncomeGreen else ExpenseRed,
                    bold = true,
                )
            }

            AppCard {
                SectionHeader("Profit")
                LineRow("Cash profit", report.profitCash, OnSurface)
                LineRow("Due profit", report.profitDue, OnSurface)
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                LineRow(
                    "Total profit",
                    report.profitTotal,
                    if (report.profitTotal >= 0) IncomeGreen else ExpenseRed,
                    bold = true,
                )
            }
        }
    }
}

@Composable
private fun LineRow(label: String, paisa: Long, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            Money.format(paisa),
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}
