package com.minipos.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.ConfirmDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.util.DateUtil

/** Settings → Activities (Phase 15): chronological log of the last 30 days, each undoable once. */
@Composable
fun ActivityScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: ActivityViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val activities by vm.activities.collectAsStateWithLifecycle()

    var pendingUndo by remember { mutableStateOf<ActivityItem?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Activities", onBack = onBack) },
    ) { innerPadding ->
        if (activities.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No activities in the last 30 days.", icon = Icons.AutoMirrored.Filled.Undo)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(activities, key = { it.key }) { item ->
                ActivityRowCard(item, onUndo = { pendingUndo = item })
            }
        }
    }

    pendingUndo?.let { item ->
        ConfirmDialog(
            title = "Undo this ${item.title.lowercase()}?",
            message = "This permanently reverses the transaction and restores stock/balances. " +
                "It will be recorded in your activity history.",
            confirmText = "Undo",
            onConfirm = {
                vm.undo(item)
                pendingUndo = null
            },
            onDismiss = { pendingUndo = null },
        )
    }
}

/**
 * Shared activity row (Phase 15/18). Reused by the Activities screen (with Undo) and the Home
 * dashboard's Recent Activity (view-only: pass [onUndo] = null so no Undo button shows).
 */
@Composable
fun ActivityRowCard(item: ActivityItem, onUndo: (() -> Unit)? = null) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(iconFor(item.type), contentDescription = null, tint = tintFor(item.type))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                if (!item.subtitle.isNullOrBlank()) {
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Text(DateUtil.formatDateTime(item.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.amountText != null) {
                    Text(item.amountText, color = amountColor(item.type), fontWeight = FontWeight.SemiBold)
                }
                if (item.undoable && onUndo != null) {
                    TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Undo")
                    }
                }
            }
        }
    }
}

private fun iconFor(type: ActivityType): ImageVector = when (type) {
    ActivityType.SELL -> Icons.Filled.Storefront
    ActivityType.BUY -> Icons.Filled.ShoppingBag
    ActivityType.EXPENSE -> Icons.Filled.Receipt
    ActivityType.CASH_IN, ActivityType.CASH_OUT -> Icons.Filled.Payments
    ActivityType.STOCK_ADJUSTMENT -> Icons.Filled.Tune
    ActivityType.UNDO -> Icons.AutoMirrored.Filled.Undo
}

private fun tintFor(type: ActivityType) = when (type) {
    ActivityType.SELL, ActivityType.CASH_IN -> IncomeGreen
    ActivityType.BUY, ActivityType.EXPENSE, ActivityType.CASH_OUT -> ExpenseRed
    else -> TextMuted
}

private fun amountColor(type: ActivityType) = when (type) {
    ActivityType.SELL, ActivityType.CASH_IN -> IncomeGreen
    ActivityType.BUY, ActivityType.EXPENSE, ActivityType.CASH_OUT -> ExpenseRed
    else -> OnSurface
}
