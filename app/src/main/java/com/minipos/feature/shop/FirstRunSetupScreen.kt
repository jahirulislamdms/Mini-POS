package com.minipos.feature.shop

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minipos.ServiceLocator
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.data.backup.ImportResult
import kotlinx.coroutines.launch

private enum class SetupMode { CHOOSER, CREATE }

/**
 * First-run setup (Future Updates Phase 1): on first launch (no shops yet) let the user either
 * **Create a New Shop** or **Restore from Backup** — fully offline. A successful restore activates
 * the restored shop and the app opens automatically (the current-shop gate in AppRoot flips).
 */
@Composable
fun FirstRunSetupScreen() {
    var mode by remember { mutableStateOf(SetupMode.CHOOSER) }

    when (mode) {
        SetupMode.CHOOSER -> SetupChooser(onCreateShop = { mode = SetupMode.CREATE })
        // Reuse the existing create flow; back returns to the chooser. On create it sets the current
        // shop, so the AppRoot gate flips and this whole screen is replaced by the main shell.
        SetupMode.CREATE -> ShopFormScreen(
            editingId = null,
            firstRun = false,
            onClose = { mode = SetupMode.CHOOSER },
        )
    }
}

@Composable
private fun SetupChooser(onCreateShop: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                message = null
                val result = runCatching { ServiceLocator.backupManager.import(uri) }
                    .getOrElse { ImportResult.Failure(it.message ?: "Restore failed") }
                // On success the current-shop gate flips and this screen is replaced — nothing more to do.
                if (result is ImportResult.Failure) {
                    message = result.message
                    busy = false
                }
            }
        }
    }

    Scaffold(containerColor = AppBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Icon(
                Icons.Filled.Storefront,
                contentDescription = null,
                tint = BrandYellow,
                modifier = Modifier.size(96.dp),
            )
            Text("MINI POS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Welcome! How would you like to get started?",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            OptionCard(
                icon = Icons.Filled.AddBusiness,
                title = "Create a New Shop",
                subtitle = "Set up a fresh shop and start selling.",
                enabled = !busy,
                onClick = onCreateShop,
            )
            Spacer(Modifier.height(12.dp))
            OptionCard(
                icon = Icons.Filled.SettingsBackupRestore,
                title = "Restore from Backup",
                subtitle = "Import a .zip backup of an existing shop. Works offline.",
                enabled = !busy,
                onClick = {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                },
            )

            if (busy) {
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
                Text("Restoring backup…", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
            }
            message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = ExpenseRed, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppCard(modifier = Modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = BrandYellow,
                modifier = Modifier.size(48.dp),
            )
            Column(Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
