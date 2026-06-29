package com.minipos.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.StockMovement

/** Adjust stock with +/− steppers and log a StockMovement; shows movement history (P4.4). */
@Composable
fun UpdateStockScreen(
    shopId: Long,
    productId: Long,
    onBack: () -> Unit,
) {
    val vm: ProductViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val productFlow = remember(productId) { vm.observeProduct(productId) }
    val product by productFlow.collectAsStateWithLifecycle(initialValue = null)
    val movementsFlow = remember(shopId, productId) { vm.movementsFor(shopId, productId) }
    val movements by movementsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var amount by remember { mutableIntStateOf(1) }
    var isAdd by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Update Stock", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Text(
                    product?.name ?: "…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Current stock: ${product?.stock?.asQty() ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text("Add stock") })
                FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text("Remove stock") })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Quantity", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                QtyStepper(value = amount, onValueChange = { amount = it }, min = 1)
            }

            AppTextField(note, { note = it }, "Note (optional)")

            PrimaryButton(
                text = if (isAdd) "Add $amount to stock" else "Remove $amount from stock",
                enabled = product != null,
                onClick = {
                    val p = product ?: return@PrimaryButton
                    if (!isAdd && amount > p.stock) {
                        // Never let a removal drive stock negative.
                        error = "Cannot remove more than current stock (${p.stock.asQty()})."
                        return@PrimaryButton
                    }
                    val delta = if (isAdd) amount.toDouble() else -amount.toDouble()
                    vm.applyStockChange(p, delta, note.trim().ifBlank { null })
                    amount = 1
                    note = ""
                    error = null
                },
            )
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
            }

            SectionHeader("Movement history")
            if (movements.isEmpty()) {
                Text("No stock movements yet.", color = TextMuted)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(movements, key = { it.id }) { m -> MovementRow(m) }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: StockMovement) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.History, contentDescription = null, tint = TextMuted)
            Column(Modifier.weight(1f)) {
                Text(
                    movement.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(DateUtil.formatDateTime(movement.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                if (!movement.note.isNullOrBlank()) {
                    Text(movement.note, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            val positive = movement.change >= 0
            Text(
                text = (if (positive) "+" else "") + movement.change.asQty(),
                color = if (positive) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
