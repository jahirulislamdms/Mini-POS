package com.minipos.feature.cash

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.CashType

/** Manual cash adjustments (Cash In / Cash Out) — affects Current Balance only, never sales/purchases. */
@Composable
fun CashManagementScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: CashViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()

    var entryType by remember { mutableStateOf<CashType?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Cash Management", onBack = onBack) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Cash in", totals.cashIn, IncomeGreen, Modifier.weight(1f))
                    StatCard("Cash out", totals.cashOut, ExpenseRed, Modifier.weight(1f))
                    StatCard("Net", totals.net, OnSurface, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(text = "Add Cash", onClick = { entryType = CashType.CASH_IN }, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "Withdraw Cash", onClick = { entryType = CashType.CASH_OUT }, modifier = Modifier.weight(1f))
                }
                Text(
                    "These adjustments change the Current Balance only — they are not sales or purchases.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cash adjustments yet.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(transactions, key = { it.id }) { txn ->
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
                                IconButton(onClick = { vm.delete(txn) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    entryType?.let { type ->
        CashEntryDialog(
            type = type,
            onConfirm = { amount, note ->
                vm.add(amount, type, note)
                entryType = null
            },
            onDismiss = { entryType = null },
        )
    }
}

@Composable
private fun CashEntryDialog(
    type: CashType,
    onConfirm: (amount: Long, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val title = if (type == CashType.CASH_IN) "Add Cash" else "Withdraw Cash"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    amount, { amount = it; error = null },
                    "Amount (৳)", keyboardType = KeyboardType.Number, errorText = error,
                )
                AppTextField(note, { note = it }, "Note / reason (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paisa = Money.parseToPaisa(amount)
                if (paisa == null || paisa <= 0) {
                    error = "Enter a valid amount"
                    return@TextButton
                }
                onConfirm(paisa, note.trim().ifBlank { null })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
