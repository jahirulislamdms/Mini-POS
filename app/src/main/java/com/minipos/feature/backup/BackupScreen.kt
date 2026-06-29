package com.minipos.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/** Per-shop backup & restore via the Storage Access Framework (P11). */
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) vm.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.import(uri) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Backup & restore", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
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
        }
    }
}
