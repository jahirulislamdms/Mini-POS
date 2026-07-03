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
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppDropdown
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.SectionHeader
import com.minipos.core.ui.StatCard
import com.minipos.core.util.Money

/** Category Report (Phase 23): buy/sell qty, amounts & profit per product by Category/Subcategory. */
@Composable
fun CategoryReportScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: CategoryReportViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val mode by vm.mode.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val rangeStart by vm.rangeStart.collectAsStateWithLifecycle()
    val rangeEnd by vm.rangeEnd.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val categoryId by vm.categoryId.collectAsStateWithLifecycle()
    val subCategoryId by vm.subCategoryId.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    val topCategories = categories.filter { it.parentId == null }
    val subCategories = categories.filter { it.parentId == categoryId }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Category Report",
                onBack = onBack,
                actions = {
                    ReportPdfAction("category-report.pdf", shopId) {
                        val catName = topCategories.firstOrNull { it.id == categoryId }?.name ?: "—"
                        val subName = subCategories.firstOrNull { it.id == subCategoryId }?.name ?: "All Subcategories"
                        "Category Report" to buildList {
                            add(PdfLine.Plain("Category: $catName · Subcategory: $subName"))
                            add(PdfLine.Header("Products"))
                            add(PdfLine.Cols(listOf("Product", "Bought", "Buy amt", "Sold", "Sales amt", "Profit"), bold = true))
                            report.rows.forEach { row ->
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            row.name,
                                            row.qtyPurchased.toString(),
                                            Money.format(row.purchaseAmount),
                                            row.qtySold.toString(),
                                            Money.format(row.salesAmount),
                                            Money.format(row.profit),
                                        ),
                                    ),
                                )
                            }
                            add(PdfLine.Header("Summary"))
                            add(PdfLine.KeyValue("Total Purchase Quantity", report.totalQtyPurchased.toString()))
                            add(PdfLine.KeyValue("Total Purchase Amount", Money.format(report.totalPurchase)))
                            add(PdfLine.KeyValue("Total Sales Quantity", report.totalQtySold.toString()))
                            add(PdfLine.KeyValue("Total Sales Amount", Money.format(report.totalSales)))
                            add(PdfLine.KeyValue("Total Profit", Money.format(report.totalProfit)))
                            add(PdfLine.Header("Current Stock Summary"))
                            add(PdfLine.KeyValue("Current Stock Quantity", report.stockQty.toString()))
                            add(PdfLine.KeyValue("Current Stock Value", Money.format(report.stockValue)))
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
                    FilterChip(mode == CategoryReportMode.DAY, { vm.setMode(CategoryReportMode.DAY) }, label = { Text("Day") })
                    FilterChip(mode == CategoryReportMode.MONTH, { vm.setMode(CategoryReportMode.MONTH) }, label = { Text("Month") })
                    FilterChip(mode == CategoryReportMode.CUSTOM, { vm.setMode(CategoryReportMode.CUSTOM) }, label = { Text("Custom") })
                }
            }
            item {
                when (mode) {
                    CategoryReportMode.DAY -> DateButton("Date", date, vm::setDate, Modifier.fillMaxWidth())
                    CategoryReportMode.MONTH -> DateButton("Month", date, vm::setDate, Modifier.fillMaxWidth())
                    CategoryReportMode.CUSTOM -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", rangeStart, vm::setRangeStart, Modifier.weight(1f))
                        DateButton("To", rangeEnd, vm::setRangeEnd, Modifier.weight(1f))
                    }
                }
            }

            item {
                AppDropdown(
                    label = "Category",
                    options = topCategories,
                    selected = topCategories.firstOrNull { it.id == categoryId },
                    optionLabel = { it.name },
                    onSelected = { vm.setCategory(it.id) },
                    includeNone = true,
                    noneLabel = "Select category",
                    onNoneSelected = { vm.setCategory(null) },
                )
            }
            item {
                AppDropdown(
                    label = "Subcategory",
                    options = subCategories,
                    selected = subCategories.firstOrNull { it.id == subCategoryId },
                    optionLabel = { it.name },
                    onSelected = { vm.setSubCategory(it.id) },
                    includeNone = true,
                    noneLabel = "All Subcategories",
                    onNoneSelected = { vm.setSubCategory(null) },
                    enabled = categoryId != null,
                )
            }

            if (categoryId == null) {
                item { Text("Select a category to see its report.", color = TextMuted) }
            } else {
                item {
                    Text(
                        "Products (${report.rows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (report.rows.isEmpty()) {
                    item { Text("No products in this category.", color = TextMuted) }
                } else {
                    items(report.rows, key = { it.productId }) { row -> CategoryRowCard(row) }

                    item { SectionHeader("Summary") }
                    item {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            SummaryLine("Total Purchase Quantity", report.totalQtyPurchased.qty())
                            SummaryLine("Total Purchase Amount", Money.format(report.totalPurchase))
                            SummaryLine("Total Sales Quantity", report.totalQtySold.qty())
                            SummaryLine("Total Sales Amount", Money.format(report.totalSales))
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    if (report.totalProfit >= 0) "Total Profit" else "Total Loss",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    Money.format(report.totalProfit),
                                    color = if (report.totalProfit >= 0) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Phase 23.1: current inventory of the selection (range-independent).
                item { SectionHeader("Current Stock Summary") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppCard(modifier = Modifier.weight(1f)) {
                            Text("Current Stock Quantity", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                            Text(report.stockQty.qty(), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        }
                        StatCard("Current Stock Value", report.stockValue, OnSurface, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRowCard(row: CategoryRow) {
    AppCard {
        Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        MetricLine("Purchased", "${row.qtyPurchased.qty()} pcs", Money.format(row.purchaseAmount), ExpenseRed)
        MetricLine("Sold", "${row.qtySold.qty()} pcs", Money.format(row.salesAmount), IncomeGreen)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                if (row.profit >= 0) "Profit" else "Loss",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                Money.format(row.profit),
                color = if (row.profit >= 0) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MetricLine(label: String, qty: String, amount: String, amountColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(qty, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
        Text(amount, color = amountColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, color = OnSurface, fontWeight = FontWeight.SemiBold)
    }
}

private fun Double.qty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
