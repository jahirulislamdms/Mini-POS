package com.minipos.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.minipos.core.theme.ExpenseRed

/** Confirmation for destructive actions (CONVENTIONS §9 — delete needs a confirm dialog). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = ExpenseRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
