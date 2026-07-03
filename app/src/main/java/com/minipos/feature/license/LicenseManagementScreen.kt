package com.minipos.feature.license

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.SectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings → License Management: status, Device ID, expiry, remaining days, Renew & Replace. */
@Composable
fun LicenseManagementScreen(onBack: () -> Unit) {
    val vm: LicenseViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var dialogTitle by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "License", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Status")
            StatusCard(state)

            val deviceId = when (val s = state) {
                is LicenseState.Active -> s.deviceId
                is LicenseState.Locked -> s.deviceId
                LicenseState.Loading -> null
            }
            if (deviceId != null) {
                DeviceIdCard(deviceId, snackbar, scope)
            }

            SectionHeader("Manage")
            Text(
                "Renew or replace your license key. This never affects your shop or saved data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            PrimaryButton(text = "Renew license", onClick = { dialogTitle = "Renew license" })
            SecondaryButton(text = "Replace license", onClick = { dialogTitle = "Replace license" })

            SectionHeader("Support")
            LicenseContactInfo()
        }
    }

    dialogTitle?.let { title ->
        UpdateLicenseDialog(
            title = title,
            onDismiss = { dialogTitle = null },
            onSubmit = { key, setError ->
                vm.activate(key) { result ->
                    when (result) {
                        is ActivationResult.Success -> {
                            dialogTitle = null
                            scope.launch { snackbar.showSnackbar("License updated") }
                        }
                        is ActivationResult.Failure -> setError(result.message)
                    }
                }
            },
        )
    }
}

@Composable
private fun StatusCard(state: LicenseState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        when (state) {
            LicenseState.Loading -> Text("Checking license…", color = TextMuted)
            is LicenseState.Active -> {
                val now = System.currentTimeMillis()
                val days = ((state.expiryMillis - now) / 86_400_000L).coerceAtLeast(0)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Status", color = TextMuted, modifier = Modifier.weight(1f))
                    Text("Active", color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                }
                LabelValue("Expires", formatDate(state.expiryMillis))
                LabelValue("Days remaining", days.toString())
            }
            is LicenseState.Locked -> {
                val label = when (state.reason) {
                    LockReason.EXPIRED -> "Expired"
                    LockReason.WRONG_DEVICE -> "Wrong device"
                    LockReason.INVALID -> "Invalid"
                    LockReason.NONE -> "Not activated"
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Status", color = TextMuted, modifier = Modifier.weight(1f))
                    Text(label, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(modifier = Modifier.padding(top = 6.dp)) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, color = OnSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UpdateLicenseDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (key: String, setError: (String) -> Unit) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paste the new license key for this device.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                AppTextField(value = key, onValueChange = { key = it; error = null }, label = "License key")
                error?.let { Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = key.isNotBlank(),
                onClick = { onSubmit(key) { msg -> error = msg } },
            ) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
