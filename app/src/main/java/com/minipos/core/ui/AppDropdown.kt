package com.minipos.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Read-only exposed dropdown. Pass [includeNone] for optional fields (category, unit, …).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    includeNone: Boolean = false,
    noneLabel: String = "None",
    onNoneSelected: () -> Unit = {},
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: if (includeNone) noneLabel else "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (includeNone) {
                DropdownMenuItem(
                    text = { Text(noneLabel) },
                    onClick = { onNoneSelected(); expanded = false },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}
