package com.minipos.core.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.util.Money

/** Semantic colour for a money amount. */
enum class AmountType { INCOME, EXPENSE, NEUTRAL }

/**
 * The ONLY way money is shown in green/red (CONVENTIONS §9).
 * Pass paisa; colour is decided by [type], not by the sign of the value.
 */
@Composable
fun AmountText(
    paisa: Long,
    type: AmountType = AmountType.NEUTRAL,
    modifier: Modifier = Modifier,
    symbol: String = Money.DEFAULT_SYMBOL,
    style: TextStyle = LocalTextStyle.current,
) {
    val color = when (type) {
        AmountType.INCOME -> IncomeGreen
        AmountType.EXPENSE -> ExpenseRed
        AmountType.NEUTRAL -> OnSurface
    }
    Text(
        text = Money.format(paisa, symbol),
        color = color,
        fontWeight = FontWeight.SemiBold,
        style = style,
        modifier = modifier,
    )
}
