package com.minipos.feature.due

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.ui.AppTextField
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType

/** Create or edit a party (Customer/Supplier/Employee) — P8.1. */
@Composable
fun PartyFormDialog(
    initial: Party?,
    defaultType: PartyType,
    onSave: (name: String, phone: String?, address: String?, type: PartyType) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: defaultType) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New party" else "Edit party") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PartyType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                AppTextField(name, { name = it; nameError = null }, "Name", errorText = nameError)
                AppTextField(phone, { phone = it }, "Phone (optional)", keyboardType = KeyboardType.Phone)
                AppTextField(address, { address = it }, "Address (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = "Name is required"
                    return@TextButton
                }
                onSave(name.trim(), phone.trim().ifBlank { null }, address.trim().ifBlank { null }, type)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
