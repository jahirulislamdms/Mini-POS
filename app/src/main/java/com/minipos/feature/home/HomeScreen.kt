package com.minipos.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.ServiceLocator
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.OnYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.SectionHeader
import com.minipos.core.ui.StatCard
import com.minipos.core.util.Money
import com.minipos.feature.activity.ActivityRowCard
import com.minipos.feature.activity.ActivityViewModel

/** Home dashboard wired to real data (P10): tiles, Day/Month toggle, Buy/Sell, shortcuts, recent activity. */
@Composable
fun HomeScreen(
    shopId: Long,
    actions: HomeActions,
) {
    val vm: HomeViewModel = viewModel()
    // Phase 18: Recent Activity reuses the exact same source/logic as Settings → Activities.
    val activityVm: ActivityViewModel = viewModel()
    androidx.compose.runtime.LaunchedEffect(shopId) {
        vm.setShop(shopId)
        activityVm.setShop(shopId)
    }

    val shopFlow = remember(shopId) { ServiceLocator.shopRepository.observeShop(shopId) }
    val shop by shopFlow.collectAsStateWithLifecycle(initialValue = null)
    val stats by vm.stats.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val activities by activityVm.activities.collectAsStateWithLifecycle()
    val recentActivities = activities.take(10)

    // Phase 25 redesign: yellow hero header (blends into the status bar) + grouped content below.
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandYellow)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MINI POS | ${shop?.name ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnYellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = actions.onOpenShops) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch shop", tint = OnYellow)
                }
            }
            Text(
                "Current Balance",
                style = MaterialTheme.typography.labelLarge,
                color = OnYellow.copy(alpha = 0.75f),
            )
            Text(
                Money.format(stats.balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnYellow,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                PeriodChip("Day", period == HomePeriod.DAY) { vm.setPeriod(HomePeriod.DAY) }
                PeriodChip("Month", period == HomePeriod.MONTH) { vm.setPeriod(HomePeriod.MONTH) }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(if (period == HomePeriod.DAY) "Today's Sales" else "This Month's Sales", stats.periodSale, IncomeGreen, Modifier.weight(1f))
                    StatCard(if (period == HomePeriod.DAY) "Today's Expenses" else "This Month's Expenses", stats.periodExpense, ExpenseRed, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("You'll Receive", stats.duesReceive, IncomeGreen, Modifier.weight(1f))
                    StatCard("You'll Give", stats.duesGive, ExpenseRed, Modifier.weight(1f))
                }
            }

            item { SectionHeader("Inventory") }
            item {
                // Phase 34: all three inventory boxes share the exact same size.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(IntrinsicSize.Min),
                ) {
                    CountTile("Types of Products", stats.productCount.toString(), Modifier.weight(1f).fillMaxHeight())
                    CountTile("Total Units", stats.totalUnits.asUnits(), Modifier.weight(1f).fillMaxHeight())
                    CountTile("Stock Value", Money.format(stats.stockValue), Modifier.weight(1f).fillMaxHeight())
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    BigActionButton("Sell", BrandYellow, Modifier.weight(1f), actions.onSell)
                    BigActionButton("Buy", BrandYellow, Modifier.weight(1f), actions.onBuy)
                }
            }

            // Phase 34: the four ledger books in one balanced 2×2 grid of equal tiles.
            item { SectionHeader("Ledger Books") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(IntrinsicSize.Min),
                    ) {
                        Shortcut("Sales Ledger", Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f).fillMaxHeight(), actions.onOpenDailyReport)
                        Shortcut("Buy Ledger", Icons.Filled.ShoppingBag, Modifier.weight(1f).fillMaxHeight(), actions.onOpenBuyReport)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(IntrinsicSize.Min),
                    ) {
                        Shortcut("Due Ledger", Icons.Filled.People, Modifier.weight(1f).fillMaxHeight(), actions.onOpenDueLedger)
                        Shortcut("Expense Ledger", Icons.Filled.Receipt, Modifier.weight(1f).fillMaxHeight(), actions.onOpenExpenses)
                    }
                }
            }

            // Phase 34: Quick Access trimmed to the three everyday actions.
            item { SectionHeader("Quick Access") }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(IntrinsicSize.Min),
                ) {
                    Shortcut("Cash Drawer", Icons.Filled.PointOfSale, Modifier.weight(1f).fillMaxHeight(), actions.onOpenCashDrawer)
                    // Completed sales & receipt reprint (Sales Ledger) — kept per the owner.
                    Shortcut("Sales", Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f).fillMaxHeight(), actions.onOpenSalesLedger)
                    Shortcut("Products", Icons.Filled.Inventory2, Modifier.weight(1f).fillMaxHeight(), actions.onOpenProducts)
                }
            }

            item { SectionHeader("Recent Activity") }
            if (recentActivities.isEmpty()) {
                item { Text("No recent activity yet.", color = TextMuted) }
            } else {
                // View-only (no Undo here — use Settings → Activities to undo).
                items(recentActivities, key = { it.key }) { item ->
                    ActivityRowCard(item)
                }
            }

            item { DashboardCredit() }
        }
    }
}

/** Day/Month toggle styled for the yellow header (dark chip when selected). */
@Composable
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = OnYellow,
            selectedContainerColor = OnYellow,
            selectedLabelColor = BrandYellow,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = OnYellow.copy(alpha = 0.4f),
            selectedBorderColor = OnYellow,
        ),
    )
}

@Composable
private fun DashboardCredit() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("Mini POS by ", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(
            "Jahirul Islam",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri("https://jahirulislam.info/") },
        )
    }
}

@Composable
private fun CountTile(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = OnSurface)
    }
}

/** Format a stock count like the Products page (no trailing .0 for whole numbers). */
private fun Double.asUnits(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

@Composable
private fun BigActionButton(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = OnYellow),
        modifier = modifier.height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Shortcut(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = BrandYellow, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
