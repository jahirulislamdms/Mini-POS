package com.minipos.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money

/** Home dashboard wired to real data (P10): tiles, Day/Month toggle, Buy/Sell, shortcuts, recent activity. */
@Composable
fun HomeScreen(
    shopId: Long,
    actions: HomeActions,
) {
    val vm: HomeViewModel = viewModel()
    androidx.compose.runtime.LaunchedEffect(shopId) { vm.setShop(shopId) }

    val shopFlow = remember(shopId) { ServiceLocator.shopRepository.observeShop(shopId) }
    val shop by shopFlow.collectAsStateWithLifecycle(initialValue = null)
    val stats by vm.stats.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = shop?.name ?: "MINI POS",
                actions = {
                    IconButton(onClick = actions.onOpenShops) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch shop")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(period == HomePeriod.DAY, { vm.setPeriod(HomePeriod.DAY) }, label = { Text("Day") })
                    FilterChip(period == HomePeriod.MONTH, { vm.setPeriod(HomePeriod.MONTH) }, label = { Text("Month") })
                }
            }

            item { StatCard("Current balance", stats.balance, OnSurface, Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(if (period == HomePeriod.DAY) "Today's sale" else "This month's sale", stats.periodSale, IncomeGreen, Modifier.weight(1f))
                    StatCard(if (period == HomePeriod.DAY) "Today's expense" else "This month's expense", stats.periodExpense, ExpenseRed, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("You'll receive", stats.duesReceive, IncomeGreen, Modifier.weight(1f))
                    StatCard("You'll give", stats.duesGive, ExpenseRed, Modifier.weight(1f))
                }
            }
            item { CountTile("Products in stock", stats.productCount) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigActionButton("Sell", BrandYellow, Modifier.weight(1f), actions.onSell)
                    BigActionButton("Buy", BrandYellow, Modifier.weight(1f), actions.onBuy)
                }
            }

            item {
                ShortcutGrid(actions)
            }

            item {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (recent.isEmpty()) {
                item { Text("No sales or purchases yet.", color = TextMuted) }
            } else {
                items(recent, key = { (if (it.isSale) "s" else "p") + it.id }) { item ->
                    AppCard(
                        modifier = Modifier.clickable {
                            if (item.isSale) actions.onOpenSaleDetail(item.id) else actions.onOpenPurchaseDetail(item.id)
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (item.isSale) Icons.Filled.Storefront else Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = if (item.isSale) IncomeGreen else ExpenseRed,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(if (item.isSale) "Sale" else "Purchase", fontWeight = FontWeight.SemiBold)
                                Text(DateUtil.formatDateTime(item.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Text(
                                Money.format(item.amount),
                                color = if (item.isSale) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item { DashboardCredit() }
        }
    }
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
private fun CountTile(label: String, count: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text("$count", style = MaterialTheme.typography.titleMedium, color = OnSurface)
    }
}

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
private fun ShortcutGrid(actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Shortcut("Products", Icons.Filled.Inventory2, Modifier.weight(1f), actions.onOpenProducts)
            Shortcut("Sales", Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f), actions.onOpenSalesLedger)
            Shortcut("Purchases", Icons.Filled.ShoppingBag, Modifier.weight(1f), actions.onOpenPurchaseLedger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Shortcut("Expenses", Icons.Filled.Receipt, Modifier.weight(1f), actions.onOpenExpenses)
            Shortcut("Due (Baki)", Icons.Filled.People, Modifier.weight(1f), actions.onOpenDueLedger)
            Shortcut("Reports", Icons.Filled.Assessment, Modifier.weight(1f), actions.onOpenBusinessReport)
        }
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
