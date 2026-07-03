package com.minipos.feature.expense

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AmountText
import com.minipos.core.ui.AmountType
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppDropdown
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.DateButton
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.StatCard
import com.minipos.core.util.DateFilter
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory

/** Expenses: list with date filter + total, add/edit via dialog (P7.2). */
@Composable
fun ExpenseScreen(
    shopId: Long,
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
) {
    val vm: ExpenseViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val rows by vm.rows.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val customStart by vm.customStart.collectAsStateWithLifecycle()
    val customEnd by vm.customEnd.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Expense?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Expenses",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Filled.Category, contentDescription = "Manage categories")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = ExpenseRed) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.minipos.core.ui.FilterChipsRow(selected = filter, onSelected = vm::setFilter)
                if (filter == DateFilter.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("From", customStart, vm::setCustomStart, Modifier.weight(1f))
                        DateButton("To", customEnd, vm::setCustomEnd, Modifier.weight(1f))
                    }
                }
                StatCard("Total expenses", total, ExpenseRed, Modifier.fillMaxWidth())
            }

            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(message = "No expenses in this period.", icon = Icons.Filled.Receipt)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rows, key = { it.expense.id }) { row ->
                        AppCard(modifier = Modifier.clickable { editing = row.expense }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        row.categoryName ?: "Uncategorized",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val sub = buildString {
                                        append(DateUtil.formatDate(row.expense.createdAt))
                                        if (!row.expense.note.isNullOrBlank()) append(" · ${row.expense.note}")
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                AmountText(row.expense.amount, AmountType.EXPENSE)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ExpenseDialog(
            initial = null,
            categories = categories,
            onSave = { amount, categoryId, note, date, onError ->
                vm.add(amount, categoryId, note, date) { ok, err ->
                    if (ok) showAdd = false else if (err != null) onError(err)
                }
            },
            onDelete = null,
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { expense ->
        ExpenseDialog(
            initial = expense,
            categories = categories,
            onSave = { amount, categoryId, note, date, _ ->
                vm.update(expense.copy(amount = amount, categoryId = categoryId, note = note, createdAt = date))
                editing = null
            },
            onDelete = { vm.delete(expense); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ExpenseDialog(
    initial: Expense?,
    categories: List<ExpenseCategory>,
    onSave: (amount: Long, categoryId: Long?, note: String?, date: Long, onError: (String) -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf(initial?.amount?.let { (it / 100).toString() } ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var date by remember { mutableStateOf(initial?.createdAt ?: System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add expense" else "Edit expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppTextField(
                    amount, { amount = it; amountError = null },
                    "Amount (৳)", keyboardType = KeyboardType.Number, errorText = amountError,
                )
                AppDropdown(
                    label = "Category",
                    options = categories,
                    selected = categories.firstOrNull { it.id == categoryId },
                    optionLabel = { it.name },
                    onSelected = { categoryId = it.id },
                    includeNone = true,
                    noneLabel = "No category",
                    onNoneSelected = { categoryId = null },
                )
                AppTextField(note, { note = it }, "Note (optional)")
                DateButton("Date", date, { date = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paisa = Money.parseToPaisa(amount)
                if (paisa == null || paisa <= 0) {
                    amountError = "Enter a valid amount"
                    return@TextButton
                }
                onSave(paisa, categoryId, note.trim().ifBlank { null }, date) { msg -> amountError = msg }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
