package com.minipos.feature.purchaseledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.PaymentType

/** Read-only detail of a single purchase (P6.3). */
@Composable
fun PurchaseDetailScreen(
    purchaseId: Long,
    onBack: () -> Unit,
) {
    val vm: PurchaseDetailViewModel = viewModel()
    var data by remember { mutableStateOf<PurchaseDetailData?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(purchaseId) {
        data = vm.load(purchaseId)
        loading = false
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Purchase detail", onBack = onBack) },
    ) { innerPadding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            data == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Purchase not found.", color = TextMuted)
            }
            else -> {
                val d = data ?: return@Scaffold
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppCard {
                        Text(DateUtil.formatDateTime(d.purchase.createdAt), fontWeight = FontWeight.SemiBold)
                        val payment = if (d.purchase.paymentType == PaymentType.DUE) "Due" else "Cash"
                        Text("Payment: $payment", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        if (d.party != null) {
                            Text("Supplier: ${d.party.name}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!d.purchase.note.isNullOrBlank()) {
                            Text("Note: ${d.purchase.note}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    SectionHeader("Items")
                    AppCard {
                        d.items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name)
                                    Text(
                                        "${item.quantity.qty()} × ${Money.format(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                                Text(Money.format(item.lineTotal), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (d.items.isEmpty()) Text("No items.", color = TextMuted)
                    }

                    AppCard {
                        AmountRow("Subtotal", d.purchase.subtotal)
                        if (d.purchase.discount > 0) AmountRow("Discount", -d.purchase.discount)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        AmountRow("Total", d.purchase.total, bold = true)
                        AmountRow("Paid", d.purchase.paidAmount)
                        if (d.purchase.dueAmount > 0) AmountRow("Due", d.purchase.dueAmount)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, paisa: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(Money.format(paisa), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun Double.qty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
