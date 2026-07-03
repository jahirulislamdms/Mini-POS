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
import com.minipos.core.ui.SectionHeader
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import kotlin.math.abs

/** Stock Report: totals, per-product breakdown, movement history (P9.1). */
@Composable
fun StockReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: StockReportViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val totalUnits by vm.totalUnits.collectAsStateWithLifecycle()
    val totalValue by vm.totalValue.collectAsStateWithLifecycle()
    val lines by vm.lines.collectAsStateWithLifecycle()
    val movements by vm.movements.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Stock Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("stock-report.pdf", shopId) {
                        "Stock Report" to buildList {
                            add(PdfLine.KeyValue("Total units", totalUnits.asQty()))
                            add(PdfLine.KeyValue("Stock value", Money.format(totalValue)))
                            add(PdfLine.Header("Products"))
                            add(PdfLine.Cols(listOf("Product", "Qty", "Buy price", "Value"), bold = true))
                            lines.forEach {
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            it.product.name,
                                            it.product.stock.asQty(),
                                            Money.format(it.product.buyPrice),
                                            Money.format(it.value),
                                        ),
                                    ),
                                )
                            }
                            add(PdfLine.Header("Stock movements"))
                            add(PdfLine.Cols(listOf("Product", "Type", "Date", "Change"), bold = true))
                            movements.forEach { row ->
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            row.productName ?: "(deleted product)",
                                            row.movement.type.name.lowercase(),
                                            DateUtil.formatDateTime(row.movement.createdAt),
                                            row.movement.change.asQty(),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitsCard(totalUnits, Modifier.weight(1f))
                    StatCard("Stock value", totalValue, OnSurface, Modifier.weight(1f))
                }
            }

            item { SectionHeader("Products") }
            if (lines.isEmpty()) {
                item { Text("No products.", color = TextMuted) }
            } else {
                items(lines, key = { "prod_${it.product.id}" }) { line ->
                    AppCard {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(line.product.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Qty ${line.product.stock.asQty()} · buy ${Money.format(line.product.buyPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                            Text(Money.format(line.value), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { SectionHeader("Stock movements") }
            if (movements.isEmpty()) {
                item { Text("No movements yet.", color = TextMuted) }
            } else {
                items(movements, key = { "mv_${it.movement.id}" }) { row ->
                    AppCard {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(row.productName ?: "(deleted product)")
                                Text(
                                    "${row.movement.type.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                                        DateUtil.formatDateTime(row.movement.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                            val positive = row.movement.change >= 0
                            Text(
                                (if (positive) "+" else "−") + abs(row.movement.change).asQty(),
                                color = if (positive) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitsCard(units: Double, modifier: Modifier) {
    AppCard(modifier = modifier) {
        Text("Total units", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(units.asQty(), style = MaterialTheme.typography.titleMedium, color = OnSurface)
    }
}

internal fun Double.asQty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
