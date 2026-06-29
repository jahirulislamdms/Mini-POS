package com.minipos.core.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnYellow
import com.minipos.core.util.DateFilter

/** Day / Month / Year / All / Custom selector used across ledgers & reports (CONVENTIONS §7). */
@Composable
fun FilterChipsRow(
    selected: DateFilter,
    onSelected: (DateFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandYellow,
                    selectedLabelColor = OnYellow,
                ),
            )
        }
    }
}
