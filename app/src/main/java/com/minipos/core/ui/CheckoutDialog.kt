package com.minipos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minipos.core.util.Money
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentType
import kotlinx.coroutines.launch

/**
 * Shared Cash/Due checkout used by Sell and Buy. The caller decides the party noun
 * ("Customer"/"Supplier"), its [partyType], and the confirm label.
 */
@Composable
fun CheckoutDialog(
    total: Long,
    partyNoun: String,
    partyType: PartyType,
    parties: List<Party>,
    confirmText: String,
    onCreateParty: suspend (String, String?) -> Long,
    onConfirm: (paymentType: PaymentType, partyId: Long?, paidAmount: Long, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var paymentType by remember { mutableStateOf(PaymentType.CASH) }
    var selectedParty by remember { mutableStateOf<Party?>(null) }
    var showNewParty by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var paidNow by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val isDue = paymentType == PaymentType.DUE
    val confirmEnabled = !isDue || selectedParty != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkout — ${Money.format(total)}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!isDue, { paymentType = PaymentType.CASH }, label = { Text("Cash") })
                    FilterChip(isDue, { paymentType = PaymentType.DUE }, label = { Text("Due") })
                }

                if (isDue) {
                    AppDropdown(
                        label = partyNoun,
                        options = parties,
                        selected = selectedParty,
                        optionLabel = { it.name },
                        onSelected = { selectedParty = it },
                        includeNone = true,
                        noneLabel = "Select ${partyNoun.lowercase()}",
                        onNoneSelected = { selectedParty = null },
                    )
                    TextButton(onClick = { showNewParty = !showNewParty }) {
                        Text(if (showNewParty) "Cancel" else "+ New ${partyNoun.lowercase()}")
                    }
                    if (showNewParty) {
                        AppTextField(newName, { newName = it }, "$partyNoun name")
                        AppTextField(newPhone, { newPhone = it }, "Phone (optional)", keyboardType = KeyboardType.Phone)
                        TextButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val nm = newName.trim()
                                    val ph = newPhone.trim().ifBlank { null }
                                    scope.launch {
                                        val id = onCreateParty(nm, ph)
                                        selectedParty = Party(
                                            id = id,
                                            shopId = 0,
                                            name = nm,
                                            phone = ph,
                                            type = partyType,
                                            createdAt = 0,
                                        )
                                        showNewParty = false
                                        newName = ""
                                        newPhone = ""
                                    }
                                }
                            },
                        ) { Text("Add $partyNoun") }
                    }
                    AppTextField(paidNow, { paidNow = it }, "Paid now (optional)", keyboardType = KeyboardType.Number)
                }

                AppTextField(note, { note = it }, "Note (optional)")
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = {
                    val paid = if (isDue) (Money.parseToPaisa(paidNow) ?: 0L) else total
                    onConfirm(paymentType, if (isDue) selectedParty?.id else null, paid, note.trim().ifBlank { null })
                },
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
