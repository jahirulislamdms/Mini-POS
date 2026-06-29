package com.minipos.feature.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.ConfirmDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.util.ImageStorage
import com.minipos.data.entity.Shop

/** Shop list + switcher (BUILD_PLAN §6.11). Tap a shop to switch; add/edit/delete from here. */
@Composable
fun ShopSwitcherScreen(
    onBack: () -> Unit,
    onAddShop: () -> Unit,
    onEditShop: (Long) -> Unit,
) {
    val vm: ShopViewModel = viewModel()
    val shops by vm.shops.collectAsStateWithLifecycle()
    val currentId by vm.currentShopId.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Shop?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Shops",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onAddShop) {
                        Icon(Icons.Filled.Add, contentDescription = "Add shop")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (shops.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No shops yet. Tap + to add one.", icon = Icons.Filled.Storefront)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shops, key = { it.id }) { shop ->
                    ShopRow(
                        shop = shop,
                        isCurrent = shop.id == currentId,
                        onClick = { vm.switchTo(shop.id) },
                        onEdit = { onEditShop(shop.id) },
                        onDelete = { pendingDelete = shop },
                    )
                }
            }
        }
    }

    pendingDelete?.let { shop ->
        ConfirmDialog(
            title = "Delete ${shop.name}?",
            message = "This permanently removes this shop and ALL of its data (products, sales, " +
                "purchases, expenses, parties and dues). This cannot be undone.",
            onConfirm = {
                vm.deleteShop(shop)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ShopRow(
    shop: Shop,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    androidx.compose.material3.Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val logoPath = shop.logoPath
            if (logoPath != null) {
                AsyncImage(
                    model = ImageStorage.fileFor(context, logoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = BrandYellow,
                    modifier = Modifier.size(44.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!shop.address.isNullOrBlank()) {
                    Text(
                        text = shop.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            if (isCurrent) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Current shop", tint = IncomeGreen)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}
