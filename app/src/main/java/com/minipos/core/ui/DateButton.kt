package com.minipos.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.minipos.core.util.DateUtil

/** Outlined button that opens a date picker; used for custom date ranges in ledgers/reports. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(
    label: String,
    millis: Long?,
    onPicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var show by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { show = true }, modifier = modifier) {
        Text(if (millis == null) label else "$label: ${DateUtil.formatDate(millis)}")
    }

    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onPicked)
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
