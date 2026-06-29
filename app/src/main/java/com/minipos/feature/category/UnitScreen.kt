package com.minipos.feature.category

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
import androidx.compose.material.icons.filled.Straighten
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
import com.minipos.data.entity.MeasureUnit

/** Manage custom measurement units (P4.1). */
@Composable
fun UnitScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: UnitViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val units by vm.units.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<MeasureUnit?>(null) }
    var deleting by remember { mutableStateOf<MeasureUnit?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Units", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = BrandYellow) {
                Icon(Icons.Filled.Add, contentDescription = "Add unit")
            }
        },
    ) { innerPadding ->
        if (units.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(message = "No units yet. Tap + to add one (e.g. pcs, kg).", icon = Icons.Filled.Straighten)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(units, key = { it.id }) { unit ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(unit.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { renaming = unit }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextMuted)
                            }
                            IconButton(onClick = { deleting = unit }) {
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
            title = "New unit",
            label = "Unit name",
            onConfirm = { vm.add(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    renaming?.let { unit ->
        NameInputDialog(
            title = "Rename unit",
            label = "Unit name",
            initial = unit.name,
            onConfirm = { vm.rename(unit, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { unit ->
        ConfirmDialog(
            title = "Delete ${unit.name}?",
            message = "This unit will be removed. Existing products keep their saved unit text.",
            onConfirm = { vm.delete(unit); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
