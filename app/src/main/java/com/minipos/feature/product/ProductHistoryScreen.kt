package com.minipos.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.minipos.core.ui.EmptyState
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.MovementType
import kotlin.math.abs

/** Read-only movement history for one product (Phase 13): Buy / Sell / Stock Adjustment, last 30 days, newest first. */
@Composable
fun ProductHistoryScreen(
    shopId: Long,
    productId: Long,
    onBack: () -> Unit,
) {
    val vm: ProductHistoryViewModel = viewModel()
    LaunchedEffect(shopId, productId) { vm.set(shopId, productId) }

    val product by vm.product.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Product History",
                onBack = onBack,
                actions = {
                    ReportPdfAction("product-history.pdf", shopId) {
                        "Product History — ${product?.name ?: ""}" to buildList {
                            add(PdfLine.Plain("Stock movements (last 30 days)"))
                            add(PdfLine.Cols(listOf("Type", "Date", "Change", "Balance"), bold = true))
                            rows.forEach { row ->
                                add(
                                    PdfLine.Cols(
                                        listOf(
                                            row.type.name.lowercase(),
                                            DateUtil.formatDateTime(row.createdAt),
                                            row.change.toString(),
                                            row.balanceAfter.toString(),
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
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No stock movements in the last 30 days.", icon = Icons.Filled.History)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        product?.name ?: "Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Stock movements (last 30 days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
            items(rows, key = { it.id }) { row -> HistoryCard(row) }
        }
    }
}

@Composable
private fun HistoryCard(row: ProductHistoryRow) {
    val positive = row.change >= 0
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(iconFor(row.type), contentDescription = null, tint = TextMuted)
            Column(Modifier.weight(1f)) {
                Text(labelFor(row.type), fontWeight = FontWeight.SemiBold)
                Text(
                    DateUtil.formatDateTime(row.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                if (!row.note.isNullOrBlank()) {
                    Text(row.note, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (positive) "+" else "−") + abs(row.change).asQty(),
                    color = if (positive) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Bal: ${row.balanceAfter.asQty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }
    }
}

private fun labelFor(type: MovementType): String = when (type) {
    MovementType.PURCHASE -> "Buy"
    MovementType.SALE -> "Sell"
    MovementType.ADJUSTMENT -> "Stock Adjustment"
    MovementType.INITIAL -> "Opening stock"
}

private fun iconFor(type: MovementType): ImageVector = when (type) {
    MovementType.PURCHASE -> Icons.Filled.ShoppingBag
    MovementType.SALE -> Icons.Filled.Storefront
    MovementType.ADJUSTMENT -> Icons.Filled.Tune
    MovementType.INITIAL -> Icons.Filled.Inventory2
}
