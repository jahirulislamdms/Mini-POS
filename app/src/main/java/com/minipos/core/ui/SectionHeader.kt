package com.minipos.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.OnSurface

/** 16sp semibold section label (CONVENTIONS §6). */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = OnSurface,
        modifier = modifier.padding(vertical = 8.dp),
    )
}
