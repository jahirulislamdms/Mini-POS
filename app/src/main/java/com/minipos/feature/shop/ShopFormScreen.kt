package com.minipos.feature.shop

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.util.ImageStorage
import kotlinx.coroutines.launch

/**
 * Create or edit a shop (BUILD_PLAN §6.11, P3.2): name, logo, address, phone, currency label,
 * default low-stock threshold. [firstRun] hides the back arrow for the onboarding case.
 */
@Composable
fun ShopFormScreen(
    editingId: Long?,
    firstRun: Boolean,
    onClose: () -> Unit,
) {
    val vm: ShopViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("৳") }
    var lowStock by remember { mutableStateOf("5") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var existingLogoPath by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(editingId != null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(editingId) {
        if (editingId != null) {
            vm.loadForEdit(editingId)?.let { data ->
                name = data.shop.name
                address = data.shop.address.orEmpty()
                phone = data.shop.phone.orEmpty()
                existingLogoPath = data.shop.logoPath
                currency = data.settings?.currencyLabel ?: "৳"
                lowStock = (data.settings?.lowStockDefault ?: 5.0).asCleanString()
            }
            loading = false
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) logoUri = uri }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = if (editingId == null) "Add Shop" else "Edit Shop",
                onBack = if (firstRun) null else onClose,
            )
        },
    ) { innerPadding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Logo picker
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val model: Any? = logoUri ?: existingLogoPath?.let { ImageStorage.fileFor(context, it) }
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Shop logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable { picker.launch(imageRequest()) },
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = "Add logo",
                            tint = BrandYellow,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { picker.launch(imageRequest()) }
                                .padding(24.dp),
                        )
                        Text("Add logo (optional)", color = TextMuted)
                    }
                }
            }

            AppTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Shop name",
                errorText = nameError,
            )
            AppTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address (optional)",
                singleLine = false,
            )
            AppTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone (optional)",
                keyboardType = KeyboardType.Phone,
            )
            AppTextField(
                value = currency,
                onValueChange = { currency = it },
                label = "Currency label",
                placeholder = "৳",
            )
            AppTextField(
                value = lowStock,
                onValueChange = { lowStock = it },
                label = "Default low-stock threshold",
                keyboardType = KeyboardType.Number,
            )

            PrimaryButton(
                text = if (saving) "Saving…" else "Save",
                enabled = !saving,
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Shop name is required"
                        return@PrimaryButton
                    }
                    saving = true
                    scope.launch {
                        val result = vm.save(
                            editingId = editingId,
                            name = name.trim(),
                            address = address.trim().ifBlank { null },
                            phone = phone.trim().ifBlank { null },
                            currencyLabel = currency.trim().ifBlank { "৳" },
                            lowStockDefault = lowStock.toDoubleOrNull() ?: 5.0,
                            logoUri = logoUri,
                        )
                        saving = false
                        // Created flips the current-shop gate (root recomposes); only Updated needs to close.
                        if (result is SaveResult.Updated || result is SaveResult.NotFound) onClose()
                    }
                },
            )
        }
    }
}

private fun imageRequest() =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

private fun Double.asCleanString(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
