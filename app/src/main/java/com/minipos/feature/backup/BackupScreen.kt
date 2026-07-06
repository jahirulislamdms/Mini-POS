package com.minipos.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SecondaryButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Per-shop backup & restore via the Storage Access Framework (P11) + automatic backups (Phase 32). */
@Composable
fun BackupScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: BackupViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val shop by vm.shop.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val auto by vm.auto.collectAsStateWithLifecycle()
    val autoBusy by vm.autoBusy.collectAsStateWithLifecycle()
    val autoMessage by vm.autoMessage.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) vm.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.import(uri) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.setAutoFolder(uri) }

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Backup & restore", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Text("Current shop", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(shop?.name ?: "…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Backup exports this shop (data, settings, logo & product photos) to a single .zip you choose. " +
                        "Restore loads a .zip into a brand-new shop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            PrimaryButton(
                text = "Export this shop",
                enabled = !busy && shop != null,
                onClick = { exportLauncher.launch(vm.suggestedFileName()) },
            )
            SecondaryButton(
                text = "Restore from backup",
                enabled = !busy,
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            )

            if (busy) {
                CircularProgressIndicator()
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            // --- Automatic backup (Phase 32) ---
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Automatic backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Backs up the current shop to a folder on a schedule — no manual steps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = auto.enabled,
                        onCheckedChange = { on ->
                            vm.setAutoEnabled(on)
                            if (on && auto.folderUri == null) folderLauncher.launch(null)
                        },
                    )
                }

                if (auto.enabled) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()

                    AutoSettingRow(
                        label = "Backup folder",
                        value = folderDisplayName(auto.folderUri) ?: "Not selected — tap to choose",
                        onClick = { if (!autoBusy) folderLauncher.launch(auto.folderUri?.let(Uri::parse)) },
                    )
                    // Phase 36: the picked folder is verified right away with a real backup.
                    if (autoBusy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.height(20.dp).width(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Creating a backup to verify the folder…",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                    autoMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    HorizontalDivider()
                    AutoSettingRow(
                        label = "Frequency",
                        value = frequencyLabel(auto.frequencyDays),
                        onClick = { showFrequencyDialog = true },
                    )
                    HorizontalDivider()
                    AutoSettingRow(
                        label = "Backup time",
                        value = formatTime(auto.hour, auto.minute),
                        onClick = { showTimeDialog = true },
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (auto.lastSuccessAt > 0) {
                            "Last automatic backup: ${formatDateTime(auto.lastSuccessAt)}"
                        } else {
                            "No automatic backup yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    Text(
                        "Keeps the latest 15 automatic backups — older ones are deleted automatically; " +
                            "manual backups are never touched. If a scheduled backup is missed (phone off), " +
                            "it runs the next time you open the app. While this is on, the daily backup " +
                            "reminder notification stays off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }

    if (showFrequencyDialog) {
        FrequencyDialog(
            current = auto.frequencyDays,
            onConfirm = { vm.setAutoFrequency(it); showFrequencyDialog = false },
            onDismiss = { showFrequencyDialog = false },
        )
    }
    if (showTimeDialog) {
        TimePickerDialog(
            initialHour = auto.hour,
            initialMinute = auto.minute,
            onConfirm = { h, m -> vm.setAutoTime(h, m); showTimeDialog = false },
            onDismiss = { showTimeDialog = false },
        )
    }
}

@Composable
private fun AutoSettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
private fun FrequencyDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(1, 2, 3, 7, 15, 30)
    var selected by remember { mutableStateOf(if (current in options) current else 1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup frequency") },
        text = {
            Column {
                options.forEach { days ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = days },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == days, onClick = { selected = days })
                        Text(frequencyLabel(days), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
        title = { Text("Backup time") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Human-readable name of a SAF tree Uri, e.g. "primary:MiniPOS Backups" → "MiniPOS Backups". */
private fun folderDisplayName(uriString: String?): String? {
    if (uriString == null) return null
    val last = Uri.parse(uriString).lastPathSegment ?: return uriString
    return last.substringAfterLast(':').ifBlank { last }
}

private fun frequencyLabel(days: Int): String =
    if (days == 1) "Every day" else "Every $days days"

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(hour12, minute, amPm)
}

private fun formatDateTime(millis: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
