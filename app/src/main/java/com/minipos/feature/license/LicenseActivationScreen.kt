package com.minipos.feature.license

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.OnSurface
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.PrimaryButton
import kotlinx.coroutines.launch

/**
 * Activation gate shown until a valid license is entered. Fully offline: shows the Device ID
 * (copyable) the customer sends to the owner, takes a license key, and verifies it on-device.
 */
@Composable
fun LicenseActivationScreen(deviceId: String, reason: LockReason) {
    val vm: LicenseViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = BrandYellow, modifier = Modifier.size(88.dp))
            Text("MINI POS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Version $version", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(
                "Activate your license to start",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )

            lockMessage(reason)?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = ExpenseRed, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            DeviceIdCard(deviceId, snackbar, scope)
            Text(
                "Send this Device ID to the owner to receive your license key.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(20.dp))
            AppTextField(
                value = key,
                onValueChange = { key = it; error = null },
                label = "License key",
            )
            error?.let {
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(12.dp))
            if (busy) {
                CircularProgressIndicator()
            } else {
                PrimaryButton(
                    text = "Activate",
                    enabled = key.isNotBlank(),
                    onClick = {
                        busy = true
                        error = null
                        vm.activate(key) { result ->
                            busy = false
                            // On success the gate flips automatically (state becomes Active).
                            if (result is ActivationResult.Failure) error = result.message
                        }
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
            LicenseContactInfo()
        }
    }
}

/** A user-facing message for why the app is locked (null on a fresh, never-activated install). */
private fun lockMessage(reason: LockReason): String? = when (reason) {
    LockReason.NONE -> null
    LockReason.EXPIRED -> "Your license has expired. Enter a new license key to continue."
    LockReason.WRONG_DEVICE -> "This license belongs to another device. Enter a license issued for this Device ID."
    LockReason.INVALID -> "Your saved license is no longer valid. Please enter a valid key."
}

/** Device ID shown read-only with a one-tap copy button. Shared by activation + management. */
@Composable
fun DeviceIdCard(
    deviceId: String,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Your Device ID", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deviceId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                scope.launch { snackbar.showSnackbar("Device ID copied") }
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Device ID", tint = BrandYellow)
            }
        }
    }
}

/** "Need a license key? Contact the Software Owner …" — shown on activation + management. */
@Composable
fun LicenseContactInfo() {
    val uriHandler = LocalUriHandler.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Need a license key?", fontWeight = FontWeight.SemiBold)
        Text(
            "Contact the Software Owner — Jahirul Islam",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            "https://jahirulislam.info/",
            color = OnSurface,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { uriHandler.openUri("https://jahirulislam.info/") },
        )
    }
}
