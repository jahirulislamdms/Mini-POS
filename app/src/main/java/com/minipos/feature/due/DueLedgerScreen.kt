package com.minipos.feature.due

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.StatCard
import com.minipos.core.util.Money
import com.minipos.data.entity.PartyType
import kotlin.math.abs

/** Due ledger: receive/give headline, party tabs, party list with net balances (P8.1–P8.2). */
@Composable
fun DueLedgerScreen(
    shopId: Long,
    onBack: () -> Unit,
    onOpenParty: (Long) -> Unit,
) {
    val vm: DueLedgerViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val headline by vm.headline.collectAsStateWithLifecycle()
    val selectedType by vm.selectedType.collectAsStateWithLifecycle()
    val rows by vm.partyRows.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Due Ledger (Baki)", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Add party")
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("You'll receive", headline.youllReceive, IncomeGreen, Modifier.weight(1f))
                StatCard("You'll give", headline.youllGive, ExpenseRed, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PartyType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { vm.setType(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() } + "s") },
                    )
                }
            }

            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(message = "No parties here yet. Tap + to add one.", icon = Icons.Filled.People)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rows, key = { it.party.id }) { row ->
                        AppCard(modifier = Modifier.clickable { onOpenParty(row.party.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.party.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (!row.party.phone.isNullOrBlank()) {
                                        Text(row.party.phone, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                }
                                BalanceLabel(row.net)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        PartyFormDialog(
            initial = null,
            defaultType = selectedType,
            onSave = { name, phone, address, type ->
                vm.addParty(name, phone, address, type)
                showAdd = false
            },
            onDelete = null,
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun BalanceLabel(net: Long) {
    val (text, color) = when {
        net > 0 -> "You'll get ${Money.format(net)}" to IncomeGreen
        net < 0 -> "You'll give ${Money.format(abs(net))}" to ExpenseRed
        else -> "Settled" to TextMuted
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold)
    }
}
