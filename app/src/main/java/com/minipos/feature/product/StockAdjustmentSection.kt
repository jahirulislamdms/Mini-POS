package com.minipos.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.SectionHeader

/**
 * Shared stock-adjustment UI (Phase 8) used by **both** the Edit Product screen and the Product
 * Details screen so the behaviour and business rules live in one place. It collects an add/remove
 * direction, a quantity, and an optional reason, enforces the no-negative-stock rule, then calls
 * [onApply] with the signed delta. The actual stock change (Room write + StockMovement) is the
 * caller's existing logic — this composable never duplicates it.
 *
 * Adjustments correct inventory only; they are never recorded as a sale.
 */
@Composable
fun StockAdjustmentSection(
    currentStock: Double,
    onApply: (delta: Double, note: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Phase 10: default quantity is 0 — the user must raise it to 1+ before adjusting.
    var amount by remember { mutableIntStateOf(0) }
    var isAdd by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Stock adjustment")
        Text(
            "Current stock: ${currentStock.asQty()}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(isAdd, { isAdd = true }, label = { Text("Add stock") })
            FilterChip(!isAdd, { isAdd = false }, label = { Text("Remove stock") })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quantity", modifier = Modifier.weight(1f))
            QtyStepper(value = amount, onValueChange = { amount = it }, min = 0)
        }
        AppTextField(note, { note = it }, "Reason (optional)")
        SecondaryButton(
            text = if (isAdd) "Add $amount to stock" else "Remove $amount from stock",
            onClick = {
                when {
                    // Phase 10: block the action at quantity 0 with a validation message.
                    amount <= 0 ->
                        error = "Enter a quantity of 1 or more."
                    !isAdd && amount > currentStock ->
                        // Never let an adjustment drive stock negative.
                        error = "Cannot remove more than current stock (${currentStock.asQty()})."
                    else -> {
                        val delta = if (isAdd) amount.toDouble() else -amount.toDouble()
                        onApply(delta, note.trim().ifBlank { null })
                        amount = 0
                        note = ""
                        error = null
                    }
                }
            },
        )
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
        }
        Text(
            "Stock adjustments correct inventory only — they are not recorded as a sale.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}
