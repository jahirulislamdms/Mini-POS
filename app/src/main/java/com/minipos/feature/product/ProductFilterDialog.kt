package com.minipos.feature.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.TextMuted
import com.minipos.data.entity.Category

/**
 * Shared Category/Subcategory multi-select filter dialog (Phases 24/28.1) — used by the Products
 * page and the Barcode Printing page. Checkboxes apply immediately; filters stay active until
 * cleared or changed.
 */
@Composable
fun ProductFilterDialog(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    selectedSubCategoryIds: Set<Long>,
    onToggleCategory: (Long) -> Unit,
    onToggleSubCategory: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val topCategories = categories.filter { it.parentId == null }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter products") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (topCategories.isEmpty()) {
                    Text("No categories yet.", color = TextMuted)
                }
                topCategories.forEach { cat ->
                    FilterCheckRow(
                        label = cat.name,
                        checked = cat.id in selectedCategoryIds,
                        onToggle = { onToggleCategory(cat.id) },
                    )
                    categories.filter { it.parentId == cat.id }.forEach { sub ->
                        FilterCheckRow(
                            label = sub.name,
                            checked = sub.id in selectedSubCategoryIds,
                            onToggle = { onToggleSubCategory(sub.id) },
                            indent = 24.dp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onClear) { Text("Clear all") } },
    )
}

@Composable
private fun FilterCheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    indent: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
