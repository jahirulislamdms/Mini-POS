package com.minipos.feature.category

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
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
import com.minipos.data.entity.Category

/** Manage fully-custom categories and their sub-categories (P4.1). */
@Composable
fun CategoryScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: CategoryViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val groups by vm.groups.collectAsStateWithLifecycle()

    // Dialog state
    var showAddTop by remember { mutableStateOf(false) }
    var addSubFor by remember { mutableStateOf<Category?>(null) }
    var renaming by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Categories", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTop = true }, containerColor = BrandYellow) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        },
    ) { innerPadding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No categories yet. Tap + to create one.", icon = Icons.Filled.Category)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.parent.id }) { group ->
                    AppCard {
                        CategoryRow(
                            name = group.parent.name,
                            isParent = true,
                            onEdit = { renaming = group.parent },
                            onDelete = { deleting = group.parent },
                        )
                        group.children.forEach { child ->
                            CategoryRow(
                                name = child.name,
                                isParent = false,
                                onEdit = { renaming = child },
                                onDelete = { deleting = child },
                            )
                        }
                        TextButton(onClick = { addSubFor = group.parent }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(" Add sub-category")
                        }
                    }
                }
            }
        }
    }

    if (showAddTop) {
        NameInputDialog(
            title = "New category",
            label = "Category name",
            onConfirm = { vm.addCategory(it); showAddTop = false },
            onDismiss = { showAddTop = false },
        )
    }
    addSubFor?.let { parent ->
        NameInputDialog(
            title = "New sub-category of ${parent.name}",
            label = "Sub-category name",
            onConfirm = { vm.addSubCategory(parent.id, it); addSubFor = null },
            onDismiss = { addSubFor = null },
        )
    }
    renaming?.let { cat ->
        NameInputDialog(
            title = "Rename",
            label = "Name",
            initial = cat.name,
            onConfirm = { vm.rename(cat, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { cat ->
        ConfirmDialog(
            title = "Delete ${cat.name}?",
            message = if (cat.parentId == null) {
                "This category and its sub-categories will be removed. Products keep their data but lose this category."
            } else {
                "This sub-category will be removed."
            },
            onConfirm = { vm.delete(cat); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CategoryRow(
    name: String,
    isParent: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = if (isParent) 0.dp else 16.dp),
    ) {
        if (!isParent) {
            Icon(
                Icons.Filled.SubdirectoryArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isParent) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextMuted) }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted) }
    }
}
