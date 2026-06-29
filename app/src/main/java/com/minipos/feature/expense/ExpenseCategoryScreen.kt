package com.minipos.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.ConfirmDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.NameInputDialog
import com.minipos.data.entity.ExpenseCategory

/** Manage custom expense categories (P7.1; defaults seeded per shop, all editable). */
@Composable
fun ExpenseCategoryScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: ExpenseCategoryViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val categories by vm.categories.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ExpenseCategory?>(null) }
    var deleting by remember { mutableStateOf<ExpenseCategory?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Expense categories", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = BrandYellow) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        },
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No expense categories yet. Tap + to add one.", icon = Icons.Filled.Receipt)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { renaming = category }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextMuted)
                            }
                            IconButton(onClick = { deleting = category }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameInputDialog(
            title = "New expense category",
            label = "Category name",
            onConfirm = { vm.add(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    renaming?.let { cat ->
        NameInputDialog(
            title = "Rename category",
            label = "Category name",
            initial = cat.name,
            onConfirm = { vm.rename(cat, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { cat ->
        ConfirmDialog(
            title = "Delete ${cat.name}?",
            message = "This category will be removed. Existing expenses keep their data but lose this category.",
            onConfirm = { vm.delete(cat); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
