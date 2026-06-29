package com.minipos.feature.due

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.ConfirmDialog
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentDirection
import kotlin.math.abs

/** Per-party statement + running balance, record payment / add due (P8.3–P8.4). */
@Composable
fun PartyDetailScreen(
    shopId: Long,
    partyId: Long,
    onBack: () -> Unit,
) {
    val vm: PartyDetailViewModel = viewModel()
    LaunchedEffect(shopId, partyId) { vm.set(shopId, partyId) }

    val party by vm.party.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()

    var showPayment by remember { mutableStateOf(false) }
    var showDue by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val isCustomer = party?.type == PartyType.CUSTOMER

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = party?.name ?: "Party",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard {
                    val (label, color) = when {
                        net > 0 -> "You'll get ${Money.format(net)}" to IncomeGreen
                        net < 0 -> "You'll give ${Money.format(abs(net))}" to ExpenseRed
                        else -> "Settled" to TextMuted
                    }
                    Text(label, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
                    party?.let { p ->
                        Text(
                            p.type.name.lowercase().replaceFirstChar { it.uppercase() } +
                                (p.phone?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(text = "Record payment", onClick = { showPayment = true }, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "Add due", onClick = { showDue = true }, modifier = Modifier.weight(1f))
                }
            }

            SectionHeader("Statement", Modifier.padding(horizontal = 16.dp))
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries) { e ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(DateUtil.formatDateTime(e.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val positive = e.signedAmount >= 0
                                    Text(
                                        (if (positive) "+" else "−") + Money.format(abs(e.signedAmount)),
                                        color = if (positive) IncomeGreen else ExpenseRed,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val bal = e.runningBalance
                                    val balText = when {
                                        bal > 0 -> "Bal: get ${Money.format(bal)}"
                                        bal < 0 -> "Bal: give ${Money.format(abs(bal))}"
                                        else -> "Settled"
                                    }
                                    Text(balText, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPayment) {
        AmountChoiceDialog(
            title = "Record payment",
            choiceALabel = "Received (in)",
            choiceBLabel = "Given (out)",
            defaultA = net >= 0,
            onConfirm = { amount, isA ->
                vm.recordPayment(if (isA) PaymentDirection.RECEIVED else PaymentDirection.GIVEN, amount)
                showPayment = false
            },
            onDismiss = { showPayment = false },
        )
    }
    if (showDue) {
        AmountChoiceDialog(
            title = "Add due",
            choiceALabel = "They'll pay you",
            choiceBLabel = "You'll pay them",
            defaultA = isCustomer,
            onConfirm = { amount, isA ->
                vm.addDue(if (isA) DueDirection.RECEIVABLE else DueDirection.PAYABLE, amount)
                showDue = false
            },
            onDismiss = { showDue = false },
        )
    }
    if (showEdit) {
        party?.let { p ->
            PartyFormDialog(
                initial = p,
                defaultType = p.type,
                onSave = { name, phone, address, type ->
                    vm.updateParty(p.copy(name = name, phone = phone, address = address, type = type))
                    showEdit = false
                },
                onDelete = null,
                onDismiss = { showEdit = false },
            )
        }
    }
    if (showDelete) {
        party?.let { p ->
            ConfirmDialog(
                title = "Delete ${p.name}?",
                message = "This removes the party. Their dues and payments remain in totals but lose their name.",
                onConfirm = { vm.deleteParty(p); showDelete = false; onBack() },
                onDismiss = { showDelete = false },
            )
        }
    }
}

@Composable
private fun AmountChoiceDialog(
    title: String,
    choiceALabel: String,
    choiceBLabel: String,
    defaultA: Boolean,
    onConfirm: (amount: Long, isA: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var isA by remember { mutableStateOf(defaultA) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(isA, { isA = true }, label = { Text(choiceALabel) })
                    FilterChip(!isA, { isA = false }, label = { Text(choiceBLabel) })
                }
                AppTextField(
                    amount, { amount = it; error = null },
                    "Amount (৳)", keyboardType = KeyboardType.Number, errorText = error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paisa = Money.parseToPaisa(amount)
                if (paisa == null || paisa <= 0) {
                    error = "Enter a valid amount"
                    return@TextButton
                }
                onConfirm(paisa, isA)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
