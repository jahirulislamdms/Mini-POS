package com.minipos.feature.salesledger

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AmountText
import com.minipos.core.ui.AmountType
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.FilterChipsRow
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateFilter
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.PaymentType

/** Sales history with date filters, search, totals and tap-to-detail (P5.5). */
@Composable
fun SalesLedgerScreen(
    shopId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val vm: SalesLedgerViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val rows by vm.rows.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val customStart by vm.customStart.collectAsStateWithLifecycle()
    val customEnd by vm.customEnd.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Sales", onBack = onBack) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChipsRow(selected = filter, onSelected = vm::setFilter)

                if (filter == DateFilter.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", customStart, vm::setCustomStart, Modifier.weight(1f))
                        DateButton("To", customEnd, vm::setCustomEnd, Modifier.weight(1f))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Total", totals.total, OnSurface, Modifier.weight(1f))
                    StatCard("Cash", totals.cashIn, IncomeGreen, Modifier.weight(1f))
                    StatCard("Due", totals.due, ExpenseRed, Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(message = "No sales in this period.", icon = Icons.AutoMirrored.Filled.ReceiptLong)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rows, key = { it.sale.id }) { row ->
                        AppCard(modifier = Modifier.clickable { onOpenDetail(row.sale.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        DateUtil.formatDateTime(row.sale.createdAt),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val sub = buildString {
                                        append(if (row.sale.paymentType == PaymentType.DUE) "Due" else "Cash")
                                        if (row.partyName != null) append(" · ${row.partyName}")
                                        if (row.sale.isQuickSale) append(" · Quick")
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                AmountText(row.sale.total, AmountType.INCOME)
                            }
                        }
                    }
                }
            }
        }
    }
}
