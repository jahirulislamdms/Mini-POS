package com.minipos.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.NameInputDialog
import com.minipos.core.ui.SectionHeader

/** Settings hub: shop, catalog, money, notifications, data, about (P12.1). */
@Composable
fun SettingsScreen(
    shopId: Long,
    onOpenShops: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenExpenseCategories: () -> Unit,
    onOpenDueLedger: () -> Unit,
    onOpenCashManagement: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val backupReminderEnabled by vm.backupReminderEnabled.collectAsStateWithLifecycle()
    val backupReminderHour by vm.backupReminderHour.collectAsStateWithLifecycle()
    val backupReminderMinute by vm.backupReminderMinute.collectAsStateWithLifecycle()

    var showLowStock by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Settings") },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader("Shop")
            NavRow(Icons.Filled.Storefront, "Manage shops", "Switch, add, edit or delete shops", onOpenShops)

            SectionHeader("Catalog")
            NavRow(Icons.Filled.Inventory2, "Products & inventory", "Add products, search, update stock", onOpenProducts)
            NavRow(Icons.Filled.Category, "Categories", "Custom categories & sub-categories", onOpenCategories)
            NavRow(Icons.Filled.Straighten, "Units", "Custom measurement units", onOpenUnits)

            SectionHeader("Money")
            NavRow(Icons.Filled.Payments, "Cash Management", "Add or withdraw cash (adjusts balance)", onOpenCashManagement)
            NavRow(Icons.Filled.Receipt, "Expenses", "Record & review expenses", onOpenExpenses)
            NavRow(Icons.Filled.Category, "Expense categories", "Custom expense categories", onOpenExpenseCategories)
            NavRow(Icons.Filled.People, "Due ledger (Baki)", "Parties, balances & payments", onOpenDueLedger)

            SectionHeader("Inventory alerts")
            NavRow(
                Icons.Filled.Warning,
                "Low-stock threshold",
                "Default: ${settings?.lowStockDefault?.let { fmt(it) } ?: "…"} units",
                onClick = { showLowStock = true },
            )

            SectionHeader("Notifications")
            SwitchRow(
                Icons.Filled.Warning,
                "Low-stock alerts",
                "Daily reminder when products run low",
                checked = settings?.lowStockNotify ?: true,
                onCheckedChange = vm::setLowStockNotify,
            )
            SwitchRow(
                Icons.Filled.Notifications,
                "Due reminders",
                "Daily reminder of money to collect",
                checked = settings?.dueNotify ?: true,
                onCheckedChange = vm::setDueNotify,
            )
            SwitchRow(
                Icons.Filled.Backup,
                "Backup reminder",
                "Daily reminder to back up your data",
                checked = backupReminderEnabled,
                onCheckedChange = vm::setBackupReminderEnabled,
            )
            NavRow(
                Icons.Filled.Schedule,
                "Backup reminder time",
                formatTime(backupReminderHour, backupReminderMinute),
                onClick = { showTimePicker = true },
            )

            SectionHeader("Data")
            NavRow(Icons.Filled.Backup, "Backup & restore", "Export this shop to a .zip, or restore one", onOpenBackup)

            SectionHeader("About")
            NavRow(Icons.Filled.Info, "About MINI POS", "Offline POS for small shops", onClick = { showAbout = true })
        }
    }

    if (showLowStock) {
        NameInputDialog(
            title = "Low-stock threshold",
            label = "Default units",
            initial = settings?.lowStockDefault?.let { fmt(it) } ?: "5",
            onConfirm = {
                it.toDoubleOrNull()?.let { v -> vm.setLowStockDefault(v) }
                showLowStock = false
            },
            onDismiss = { showLowStock = false },
        )
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = backupReminderHour,
            initialMinute = backupReminderMinute,
            onConfirm = { h, m -> vm.setBackupReminderTime(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(hour12, minute, amPm)
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Mini POS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("App Name: Mini POS")
                Text("Owner: Jahirul Islam")
                Row {
                    Text("Website: ")
                    Text(
                        "jahirulislam.info",
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri("https://jahirulislam.info/") },
                    )
                }
                Text(
                    "Version $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                Text(
                    "100% offline point-of-sale & bookkeeping for small shops.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = BrandYellow, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = BrandYellow, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
