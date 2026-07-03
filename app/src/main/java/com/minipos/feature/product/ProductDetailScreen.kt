package com.minipos.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.OnYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.util.ImageStorage
import com.minipos.core.util.Money

/** Read-only product details with Sell / Buy / Edit quick actions (edit protection). */
@Composable
fun ProductDetailScreen(
    shopId: Long,
    productId: Long,
    onBack: () -> Unit,
    onSell: () -> Unit,
    onBuy: () -> Unit,
    onEdit: () -> Unit,
) {
    val vm: ProductDetailViewModel = viewModel()
    LaunchedEffect(shopId, productId) { vm.set(shopId, productId) }

    val product by vm.product.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Product details", onBack = onBack) },
    ) { innerPadding ->
        val p = product
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val categoryName = p.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name }
        val subCategoryName = p.subCategoryId?.let { id -> categories.firstOrNull { it.id == id }?.name }

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: photo + name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val photo = p.photoPath
                if (photo != null) {
                    AsyncImage(
                        model = ImageStorage.fileFor(context, photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(72.dp))
                }
                Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("Sell", Icons.Filled.Storefront, Modifier.weight(1f), onSell)
                ActionButton("Buy", Icons.Filled.ShoppingBag, Modifier.weight(1f), onBuy)
                ActionButton("Edit", Icons.Filled.Edit, Modifier.weight(1f), onEdit)
            }

            // Details (read-only)
            AppCard {
                InfoRow("Category", categoryName ?: "—")
                InfoRow("Sub-category", subCategoryName ?: "—")
                InfoRow("Unit", p.unit ?: "—")
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                InfoRow("Selling price", Money.format(p.sellPrice))
                InfoRow("Buy price", Money.format(p.buyPrice))
                StockRow(p.stock.asQty(), p.isLowStock())
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                InfoRow("Low-stock alert", if (p.lowStockAlertEnabled) "On" else "Off")
                if (p.lowStockThreshold != null) {
                    InfoRow("Low-stock threshold", p.lowStockThreshold.asQty())
                }
                if (p.vatEnabled) InfoRow("VAT", "${p.vatPercent.cleanNumber()}%")
                if (p.warrantyEnabled) InfoRow("Warranty", p.warrantyText?.ifBlank { "Yes" } ?: "Yes")
                if (p.wholesaleEnabled && p.wholesalePrice != null) {
                    InfoRow("Wholesale price", Money.format(p.wholesalePrice))
                }
                if (p.discountEnabled) InfoRow("Discount", "${p.discountPercent.cleanNumber()}%")
            }

            Text(
                "Product details are read-only. Tap Edit to change them.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // Stock adjustment (Phase 8) — same shared section/logic as the Edit screen.
            StockAdjustmentSection(
                currentStock = p.stock,
                onApply = { delta, note -> vm.applyStockChange(p, delta, note) },
            )
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandYellow, contentColor = OnYellow),
        modifier = modifier.height(48.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  $label")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StockRow(stock: String, isLow: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("Current stock", modifier = Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(
            stock + if (isLow) "  (low)" else "",
            fontWeight = FontWeight.SemiBold,
            color = if (isLow) ExpenseRed else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun com.minipos.data.entity.Product.isLowStock(): Boolean =
    lowStockAlertEnabled && stock <= (lowStockThreshold ?: Double.NEGATIVE_INFINITY)

private fun Double.cleanNumber(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
