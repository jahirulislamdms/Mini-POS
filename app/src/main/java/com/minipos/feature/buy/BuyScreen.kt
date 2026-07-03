package com.minipos.feature.buy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.minipos.core.print.ReceiptPrinter
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.CheckoutDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.Money
import com.minipos.core.util.SearchUtil
import com.minipos.feature.barcode.BarcodeScannerDialog
import com.minipos.data.entity.PartyType
import kotlinx.coroutines.launch

/** Buy: build a supplier purchase cart, then Cash/Due checkout. Increments stock (P6.1–P6.2). */
@Composable
fun BuyScreen(
    shopId: Long,
    onOpenPurchaseLedger: () -> Unit,
    onBack: (() -> Unit)? = null,
    initialProductId: Long? = null,
) {
    val vm: BuyViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    LaunchedEffect(initialProductId) { if (initialProductId != null) vm.addProductById(initialProductId) }

    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val suppliers by vm.suppliers.collectAsStateWithLifecycle()

    var search by remember { mutableStateOf("") }
    var showCheckout by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }     // Phase 28: scan-to-buy
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var askPrintPurchaseId by remember { mutableStateOf<Long?>(null) } // Phase 29: print receipt?
    val context = LocalContext.current

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val total = cart.sumOf { it.lineTotal }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Buy",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenPurchaseLedger) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Purchase ledger")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (total > 0) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(Money.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    PrimaryButton(text = "Pay ${Money.format(total)}", onClick = { showCheckout = true })
                }
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search products") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { scanFeedback = null; showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            if (products.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(message = "No products yet. Add products first to record a purchase.")
                }
            } else {
                // Smart search (Phase 22): case/space-insensitive partial matching, ranked.
                val filtered = SearchUtil.filter(products, search) { listOf(it.name) }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (cart.isNotEmpty()) {
                        item { SectionHeader("Cart") }
                        items(cart, key = { "cart_${it.product.id}" }) { line ->
                            BuyLineRow(
                                name = line.product.name,
                                quantity = line.quantity,
                                unitPrice = line.unitPrice,
                                lineTotal = line.lineTotal,
                                onQty = { vm.setQuantity(line.product.id, it) },
                                onUnitPrice = { vm.setUnitPrice(line.product.id, it) },
                            )
                        }
                        item { HorizontalDivider() }
                    }
                    item { SectionHeader("Tap to add") }
                    items(filtered, key = { "prod_${it.id}" }) { product ->
                        AppCard(modifier = Modifier.clickable { vm.addToCart(product) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Buy: ${Money.format(product.buyPrice)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Text("Stock: ${product.stock.asQty()}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Phase 28: continuous scan-to-buy — each scanned product is added to the cart.
    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scan to buy",
            feedback = scanFeedback,
            onScanned = { code -> vm.addByBarcode(code) { msg -> scanFeedback = msg } },
            onDismiss = { showScanner = false },
        )
    }

    if (showCheckout && total > 0) {
        CheckoutDialog(
            total = total,
            partyNoun = "Supplier",
            partyType = PartyType.SUPPLIER,
            parties = suppliers,
            confirmText = "Confirm purchase",
            onCreateParty = { name, phone -> vm.createSupplier(name, phone) },
            onConfirm = { paymentType, partyId, paid, note ->
                vm.confirmPurchase(paymentType, partyId, paid, note) { purchaseId ->
                    showCheckout = false
                    askPrintPurchaseId = purchaseId
                    scope.launch { snackbar.showSnackbar("Purchase recorded") }
                }
            },
            onDismiss = { showCheckout = false },
        )
    }

    // Phase 29: offer to print the receipt after every completed purchase.
    askPrintPurchaseId?.let { purchaseId ->
        AlertDialog(
            onDismissRequest = { askPrintPurchaseId = null },
            title = { Text("Print receipt?") },
            text = { Text("Do you want to print the receipt for this purchase?") },
            confirmButton = {
                TextButton(onClick = {
                    askPrintPurchaseId = null
                    scope.launch { ReceiptPrinter.printPurchase(context, purchaseId) }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { askPrintPurchaseId = null }) { Text("No") }
            },
        )
    }
}

@Composable
private fun BuyLineRow(
    name: String,
    quantity: Int,
    unitPrice: Long,
    lineTotal: Long,
    onQty: (Int) -> Unit,
    onUnitPrice: (Long) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(Money.format(lineTotal), fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QtyStepper(value = quantity, onValueChange = onQty, min = 0)
            OutlinedTextField(
                value = if (unitPrice == 0L) "" else (unitPrice / 100).toString(),
                onValueChange = { onUnitPrice(Money.parseToPaisa(it) ?: 0L) },
                label = { Text("Buy ৳") },
                singleLine = true,
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

private fun Double.asQty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
