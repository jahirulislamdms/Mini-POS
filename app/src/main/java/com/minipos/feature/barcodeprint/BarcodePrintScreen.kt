package com.minipos.feature.barcodeprint

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.SectionHeader
import com.minipos.feature.barcode.BarcodeScannerDialog
import com.minipos.feature.product.ProductFilterDialog
import kotlinx.coroutines.launch
import java.io.File

/**
 * Settings → Barcode Printing (Phase 28): select products + label counts, choose label fields
 * and sheet size, then save a print-ready PDF or print via Android's print framework.
 */
@Composable
fun BarcodePrintScreen(
    shopId: Long,
    onBack: () -> Unit,
) {
    val vm: BarcodePrintViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val products by vm.products.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val options by vm.options.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selectedCategoryIds by vm.selectedCategoryIds.collectAsStateWithLifecycle()
    val selectedSubCategoryIds by vm.selectedSubCategoryIds.collectAsStateWithLifecycle()
    val filtersActive = selectedCategoryIds.isNotEmpty() || selectedSubCategoryIds.isNotEmpty()

    var showFilter by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }

    // Sheet geometry (mm / counts) — free-form fields with sensible defaults.
    var widthMm by remember { mutableStateOf("50") }
    var heightMm by remember { mutableStateOf("30") }
    var marginMm by remember { mutableStateOf("3") }
    var perRow by remember { mutableStateOf("3") }
    var perPage by remember { mutableStateOf("24") }

    var pendingPdf by remember { mutableStateOf<ByteArray?>(null) }

    fun currentLayout() = LabelLayout(
        widthMm = widthMm.toFloatOrNull() ?: 50f,
        heightMm = heightMm.toFloatOrNull() ?: 30f,
        marginMm = marginMm.toFloatOrNull() ?: 3f,
        perRow = perRow.toIntOrNull() ?: 3,
        perPage = perPage.toIntOrNull() ?: 24,
    )

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val bytes = pendingPdf
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }.onSuccess {
                scope.launch { snackbar.showSnackbar("PDF saved") }
            }.onFailure {
                scope.launch { snackbar.showSnackbar("Could not save the PDF") }
            }
        }
        pendingPdf = null
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Barcode Printing", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionHeader("Products (${selection.size} selected)") }
            item {
                // Phase 28.1: shared smart search + Category/Subcategory filter + scan-to-select.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { vm.setQuery(it) },
                        label = { Text("Search products") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { scanFeedback = null; showScanner = true }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showFilter = true }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filter",
                            tint = if (filtersActive) BrandYellow else TextMuted,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = vm::selectAll) { Text("Select all") }
                    TextButton(onClick = vm::clearSelection) { Text("Clear") }
                }
            }
            if (products.isEmpty()) {
                item {
                    Text(
                        if (query.isBlank() && !filtersActive) "No products yet."
                        else "No products match your search.",
                        color = TextMuted,
                    )
                }
            } else {
                items(products, key = { it.id }) { product ->
                    val count = selection[product.id]
                    AppCard(modifier = Modifier.clickable { vm.toggleProduct(product.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = count != null,
                                onCheckedChange = { vm.toggleProduct(product.id) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    product.barcode ?: "No barcode yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                            if (count != null) {
                                QtyStepper(
                                    value = count,
                                    onValueChange = { vm.setLabelCount(product.id, it) },
                                    min = 0,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Label content") }
            item {
                AppCard {
                    OptionSwitch("Barcode number", options.showBarcodeText) {
                        vm.setOptions(options.copy(showBarcodeText = it))
                    }
                    OptionSwitch("Product name", options.showName) {
                        vm.setOptions(options.copy(showName = it))
                    }
                    OptionSwitch("Category", options.showCategory) {
                        vm.setOptions(options.copy(showCategory = it))
                    }
                    OptionSwitch("Subcategory", options.showSubcategory) {
                        vm.setOptions(options.copy(showSubcategory = it))
                    }
                    OptionSwitch("Selling price", options.showSellPrice) {
                        vm.setOptions(options.copy(showSellPrice = it))
                    }
                    OptionSwitch("Buying price", options.showBuyPrice) {
                        vm.setOptions(options.copy(showBuyPrice = it))
                    }
                }
            }

            item { SectionHeader("Label size (A4 sheet)") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        AppTextField(widthMm, { widthMm = it }, "Width (mm)", keyboardType = KeyboardType.Number)
                    }
                    Column(Modifier.weight(1f)) {
                        AppTextField(heightMm, { heightMm = it }, "Height (mm)", keyboardType = KeyboardType.Number)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        AppTextField(marginMm, { marginMm = it }, "Margin (mm)", keyboardType = KeyboardType.Number)
                    }
                    Column(Modifier.weight(1f)) {
                        AppTextField(perRow, { perRow = it }, "Labels per row", keyboardType = KeyboardType.Number)
                    }
                    Column(Modifier.weight(1f)) {
                        AppTextField(perPage, { perPage = it }, "Labels per page", keyboardType = KeyboardType.Number)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    PrimaryButton(
                        text = "Save as PDF",
                        onClick = {
                            vm.generatePdf(
                                layout = currentLayout(),
                                onReady = { bytes ->
                                    pendingPdf = bytes
                                    savePdfLauncher.launch("barcodes.pdf")
                                },
                                onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                            )
                        },
                    )
                    SecondaryButton(
                        text = "Print",
                        onClick = {
                            vm.generatePdf(
                                layout = currentLayout(),
                                onReady = { bytes ->
                                    runCatching {
                                        val file = File(context.cacheDir, "barcode_labels.pdf")
                                        file.writeBytes(bytes)
                                        val printManager =
                                            context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                        printManager.print(
                                            "MINI POS barcode labels",
                                            PdfPrintAdapter(file, "MINI POS barcode labels"),
                                            PrintAttributes.Builder().build(),
                                        )
                                    }.onFailure {
                                        scope.launch { snackbar.showSnackbar("Could not start printing") }
                                    }
                                },
                                onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                            )
                        },
                    )
                }
            }
        }
    }

    // Shared Category/Subcategory filter (same dialog as the Products page).
    if (showFilter) {
        ProductFilterDialog(
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            selectedSubCategoryIds = selectedSubCategoryIds,
            onToggleCategory = vm::toggleCategory,
            onToggleSubCategory = vm::toggleSubCategory,
            onClear = vm::clearFilters,
            onDismiss = { showFilter = false },
        )
    }

    // Scan-to-select: each scan selects the product / adds one more label for it.
    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scan to select",
            feedback = scanFeedback,
            onScanned = { code ->
                vm.findByBarcode(code) { product ->
                    if (product == null) {
                        scanFeedback = "No product found for this barcode"
                    } else {
                        val count = (selection[product.id] ?: 0) + 1
                        vm.setLabelCount(product.id, count)
                        scanFeedback = "${product.name} — $count label(s)"
                    }
                }
            },
            onDismiss = { showScanner = false },
        )
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
