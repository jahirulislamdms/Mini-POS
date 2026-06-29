package com.minipos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.TextMuted
import com.minipos.core.util.Money

/**
 * Small dashboard tile: a muted label over a coloured money amount.
 * [color] tints the amount (green income / red expense / brand blue, per caller).
 */
@Composable
fun StatCard(
    label: String,
    amountPaisa: Long,
    color: Color,
    modifier: Modifier = Modifier,
    symbol: String = Money.DEFAULT_SYMBOL,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            Text(
                text = Money.format(amountPaisa, symbol),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
    }
}
