package com.minipos.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** Standard outlined text field with optional inline error (CONVENTIONS §9 — forms validate). */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
