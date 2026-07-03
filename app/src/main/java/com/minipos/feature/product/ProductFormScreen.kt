package com.minipos.feature.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.ExpenseRed
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppDropdown
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.ConfirmDialog
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.ImageStorage
import com.minipos.feature.barcode.BarcodeScannerDialog
import com.minipos.core.util.Money
import com.minipos.data.entity.Product
import kotlinx.coroutines.launch

/** Add or edit a product: all fields + toggles + photo (P4.2). Stock is managed separately. */
@Composable
fun ProductFormScreen(
    shopId: Long,
    editingId: Long?,
    onClose: () -> Unit,
    onDeleted: () -> Unit = onClose,
) {
    val vm: ProductViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(shopId) { vm.setShop(shopId) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val defaultUnit by vm.defaultUnit.collectAsStateWithLifecycle()

    var loaded by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var openingStock by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var subCategoryId by remember { mutableStateOf<Long?>(null) }
    var unitName by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var existingPhoto by remember { mutableStateOf<String?>(null) }

    var lowStockAlert by remember { mutableStateOf(true) }
    var lowStockThreshold by remember { mutableStateOf("") }
    var vatEnabled by remember { mutableStateOf(false) }
    var vatPercent by remember { mutableStateOf("") }
    var warrantyEnabled by remember { mutableStateOf(false) }
    var warrantyText by remember { mutableStateOf("") }
    var wholesaleEnabled by remember { mutableStateOf(false) }
    var wholesalePrice by remember { mutableStateOf("") }
    var discountEnabled by remember { mutableStateOf(false) }
    var discountPercent by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(editingId != null) }
    var saving by remember { mutableStateOf(false) }

    // Barcode (Phase 28): blank = auto-generate; scanning fills the field.
    var barcode by remember { mutableStateOf("") }
    var barcodeError by remember { mutableStateOf<String?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Delete (edit mode only) — allowed only when stock is exactly 0.
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingId) {
        if (editingId != null) {
            vm.loadForEdit(editingId)?.let { p ->
                loaded = p
                name = p.name
                sellPrice = p.sellPrice.toTakaInput()
                buyPrice = p.buyPrice.toTakaInput()
                categoryId = p.categoryId
                subCategoryId = p.subCategoryId
                unitName = p.unit
                existingPhoto = p.photoPath
                barcode = p.barcode.orEmpty()
                lowStockAlert = p.lowStockAlertEnabled
                lowStockThreshold = p.lowStockThreshold?.asInput() ?: ""
                vatEnabled = p.vatEnabled
                vatPercent = p.vatPercent.asInput()
                warrantyEnabled = p.warrantyEnabled
                warrantyText = p.warrantyText.orEmpty()
                wholesaleEnabled = p.wholesaleEnabled
                wholesalePrice = p.wholesalePrice?.toTakaInput() ?: ""
                discountEnabled = p.discountEnabled
                discountPercent = p.discountPercent.asInput()
            }
            loading = false
        }
    }

    // Phase 31: pre-select the shop's default unit on NEW products (only while nothing is chosen
    // and the unit actually exists); the user can still pick any other unit or none.
    LaunchedEffect(defaultUnit, units) {
        if (editingId == null && unitName == null && units.any { it.name == defaultUnit }) {
            unitName = defaultUnit
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }

    val topCategories = categories.filter { it.parentId == null }
    val subCategories = categories.filter { it.parentId == categoryId }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(title = if (editingId == null) "Add Product" else "Edit Product", onBack = onClose)
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
            // Photo
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val model: Any? = photoUri ?: existingPhoto?.let { ImageStorage.fileFor(context, it) }
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Product photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { picker.launch(imageRequest()) },
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = "Add photo",
                            tint = BrandYellow,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { picker.launch(imageRequest()) }.padding(20.dp),
                        )
                        Text("Add photo (optional)", color = TextMuted)
                    }
                }
            }

            AppTextField(name, { name = it; nameError = null }, "Product name", errorText = nameError)
            AppTextField(sellPrice, { sellPrice = it }, "Sell price (৳)", keyboardType = KeyboardType.Number)
            AppTextField(buyPrice, { buyPrice = it }, "Buy price (৳)", keyboardType = KeyboardType.Number)
            if (editingId == null) {
                AppTextField(openingStock, { openingStock = it }, "Opening stock", keyboardType = KeyboardType.Number)
            }

            // Barcode with camera-scan fill (Phase 28).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AppTextField(
                        barcode, { barcode = it; barcodeError = null },
                        "Barcode (leave empty to auto-generate)",
                        errorText = barcodeError,
                    )
                }
                IconButton(onClick = { showBarcodeScanner = true }) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode", tint = BrandYellow)
                }
            }

            AppDropdown(
                label = "Category",
                options = topCategories,
                selected = topCategories.firstOrNull { it.id == categoryId },
                optionLabel = { it.name },
                onSelected = { categoryId = it.id; subCategoryId = null },
                includeNone = true,
                noneLabel = "No category",
                onNoneSelected = { categoryId = null; subCategoryId = null },
            )
            AppDropdown(
                label = "Sub-category",
                options = subCategories,
                selected = subCategories.firstOrNull { it.id == subCategoryId },
                optionLabel = { it.name },
                onSelected = { subCategoryId = it.id },
                includeNone = true,
                noneLabel = "None",
                onNoneSelected = { subCategoryId = null },
                enabled = categoryId != null,
            )
            AppDropdown(
                label = "Unit",
                options = units,
                selected = units.firstOrNull { it.name == unitName },
                optionLabel = { it.name },
                onSelected = { unitName = it.name },
                includeNone = true,
                onNoneSelected = { unitName = null },
            )

            SectionHeader("Options")
            ToggleField("Low-stock alert", lowStockAlert, { lowStockAlert = it }) {
                AppTextField(
                    lowStockThreshold, { lowStockThreshold = it },
                    "Low-stock threshold (blank = shop default)", keyboardType = KeyboardType.Number,
                )
            }
            ToggleField("VAT", vatEnabled, { vatEnabled = it }) {
                AppTextField(vatPercent, { vatPercent = it }, "VAT %", keyboardType = KeyboardType.Number)
            }
            ToggleField("Warranty", warrantyEnabled, { warrantyEnabled = it }) {
                AppTextField(warrantyText, { warrantyText = it }, "Warranty details")
            }
            ToggleField("Wholesale price", wholesaleEnabled, { wholesaleEnabled = it }) {
                AppTextField(wholesalePrice, { wholesalePrice = it }, "Wholesale price (৳)", keyboardType = KeyboardType.Number)
            }
            ToggleField("Discount", discountEnabled, { discountEnabled = it }) {
                AppTextField(discountPercent, { discountPercent = it }, "Discount %", keyboardType = KeyboardType.Number)
            }

            loaded?.let { current ->
                if (editingId != null) {
                    // Shared with Product Details (Phase 8) — same UI, same rules, same logic.
                    StockAdjustmentSection(
                        currentStock = current.stock,
                        onApply = { delta, adjNote ->
                            vm.applyStockChange(current, delta, adjNote)
                            loaded = current.copy(stock = current.stock + delta)
                        },
                    )
                }
            }

            PrimaryButton(
                text = if (saving) "Saving…" else "Save product",
                enabled = !saving,
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Product name is required"
                        return@PrimaryButton
                    }
                    saving = true
                    scope.launch {
                        val base = loaded ?: Product(shopId = shopId, name = "", createdAt = 0, updatedAt = 0)
                        val product = base.copy(
                            shopId = shopId,
                            name = name.trim(),
                            sellPrice = Money.parseToPaisa(sellPrice) ?: 0L,
                            buyPrice = Money.parseToPaisa(buyPrice) ?: 0L,
                            stock = if (editingId == null) (openingStock.toDoubleOrNull() ?: 0.0) else base.stock,
                            categoryId = categoryId,
                            subCategoryId = subCategoryId,
                            unit = unitName,
                            photoPath = existingPhoto,
                            lowStockThreshold = if (lowStockAlert) lowStockThreshold.toDoubleOrNull() else null,
                            lowStockAlertEnabled = lowStockAlert,
                            vatEnabled = vatEnabled,
                            vatPercent = vatPercent.toDoubleOrNull() ?: 0.0,
                            warrantyEnabled = warrantyEnabled,
                            warrantyText = warrantyText.trim().ifBlank { null },
                            wholesaleEnabled = wholesaleEnabled,
                            wholesalePrice = if (wholesaleEnabled) Money.parseToPaisa(wholesalePrice) else null,
                            discountEnabled = discountEnabled,
                            discountPercent = discountPercent.toDoubleOrNull() ?: 0.0,
                            barcode = barcode.trim().ifBlank { null },
                        )
                        val error = vm.save(product, photoUri)
                        if (error != null) {
                            barcodeError = error
                            saving = false
                        } else {
                            onClose()
                        }
                    }
                },
            )

            if (editingId != null) {
                OutlinedButton(
                    onClick = {
                        val current = loaded
                        if (current != null && current.stock != 0.0) {
                            deleteError = "This product still has stock. Remove all stock before deleting."
                        } else {
                            deleteError = null
                            showDeleteConfirm = true
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete product")
                }
                deleteError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan barcode",
            feedback = null,
            onScanned = { code ->
                barcode = code
                barcodeError = null
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false },
        )
    }

    if (showDeleteConfirm) {
        val current = loaded
        ConfirmDialog(
            title = "Delete product?",
            message = "\"${current?.name.orEmpty()}\" will be permanently deleted. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                if (current != null) {
                    vm.delete(current)
                    onDeleted()
                }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

/** Toggle header + revealed content when checked. */
@Composable
private fun ToggleField(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChecked)
        }
        if (checked) content()
    }
}

private fun imageRequest() =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

private fun Long.toTakaInput(): String {
    val whole = this / 100
    val frac = (this % 100).toInt()
    return if (frac == 0) whole.toString() else "$whole.${frac.toString().padStart(2, '0')}"
}

private fun Double.asInput(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
