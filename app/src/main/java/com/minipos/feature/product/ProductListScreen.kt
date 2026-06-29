package com.minipos.feature.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.OnYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.StatCard
import com.minipos.core.util.ImageStorage
import com.minipos.core.util.Money
import com.minipos.data.entity.Product

/** Product/Inventory list: search + category filter + empty state (P4.3). */
@Composable
fun ProductListScreen(
    shopId: Long,
    onAddProduct: () -> Unit,
    onProductDetail: (Long) -> Unit,
    onUpdateStock: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val vm: ProductViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val products by vm.products.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedCategory by vm.categoryFilter.collectAsStateWithLifecycle()
    val lowStockDefault by vm.lowStockDefault.collectAsStateWithLifecycle()
    val totalUnits by vm.totalUnits.collectAsStateWithLifecycle()
    val totalStockValue by vm.totalStockValue.collectAsStateWithLifecycle()

    val topCategories = categories.filter { it.parentId == null }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Products", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct, containerColor = BrandYellow) {
                Icon(Icons.Filled.Add, contentDescription = "Add product")
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Shop-wide inventory summary (Phase 4) — same totals as the Stock Report; live-updating.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard("Total units", totalUnits.asQty(), Modifier.weight(1f))
                StatCard("Stock value", totalStockValue, OnSurface, Modifier.weight(1f))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                label = { Text("Search products") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (topCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { vm.setCategoryFilter(null) },
                        label = { Text("All") },
                    )
                    topCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.id,
                            onClick = { vm.setCategoryFilter(cat.id) },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        message = if (query.isBlank() && selectedCategory == null) {
                            "No products yet. Tap + to add your first one."
                        } else {
                            "No products match your search."
                        },
                        icon = Icons.Filled.Inventory2,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            lowStockDefault = lowStockDefault,
                            onClick = { onProductDetail(product.id) },
                            onUpdateStock = { onUpdateStock(product.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    lowStockDefault: Double,
    onClick: () -> Unit,
    onUpdateStock: () -> Unit,
) {
    val context = LocalContext.current
    val isLow = product.lowStockAlertEnabled &&
        product.stock <= (product.lowStockThreshold ?: lowStockDefault)

    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val photo = product.photoPath
            if (photo != null) {
                AsyncImage(
                    model = ImageStorage.fileFor(context, photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Icon(
                    Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(52.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Sell: ${Money.format(product.sellPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Stock: ${product.stock.asQty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLow) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Update",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnYellow,
                    modifier = Modifier.clickable(onClick = onUpdateStock),
                )
            }
        }
    }
}

/** Non-money summary tile (count) — matches StatCard's muted-label-over-value design. */
@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    AppCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = OnSurface)
    }
}

internal fun Double.asQty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
