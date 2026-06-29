package com.minipos.feature.sell

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.CheckoutDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.Money
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Product
import kotlinx.coroutines.launch

private enum class SellMode { QUICK, PRODUCTS }

/** Sell: Quick Sell (amount) or a product cart, then Cash/Due checkout (P5.1–P5.4). */
@Composable
fun SellScreen(
    shopId: Long,
    onOpenSalesLedger: () -> Unit,
    onBack: (() -> Unit)? = null,
    initialProductId: Long? = null,
) {
    val vm: SellViewModel = viewModel()
    LaunchedEffect(shopId) { vm.setShop(shopId) }
    LaunchedEffect(initialProductId) { if (initialProductId != null) vm.addProductById(initialProductId) }

    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(SellMode.PRODUCTS) }
    var quickAmount by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var showCheckout by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cartTotal = cart.sumOf { it.lineTotal }
    val quickTotal = Money.parseToPaisa(quickAmount) ?: 0L
    val total = if (mode == SellMode.QUICK) quickTotal else cartTotal

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = "Sell",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenSalesLedger) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Sales ledger")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (total > 0) {
                ChargeBar(total = total, onCharge = { showCheckout = true })
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(mode == SellMode.QUICK, { mode = SellMode.QUICK }, label = { Text("Quick Sell") })
                FilterChip(mode == SellMode.PRODUCTS, { mode = SellMode.PRODUCTS }, label = { Text("Products") })
            }

            when (mode) {
                SellMode.QUICK -> {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        AppTextField(
                            quickAmount, { quickAmount = it },
                            "Amount (৳)", keyboardType = KeyboardType.Number,
                        )
                    }
                }

                SellMode.PRODUCTS -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Search products") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    val filtered = products.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
                    if (products.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            EmptyState(message = "No products yet. Add products first, or use Quick Sell.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (cart.isNotEmpty()) {
                                item { SectionHeader("Cart") }
                                items(cart, key = { "cart_${it.product.id}" }) { line ->
                                    CartLineRow(
                                        name = line.product.name,
                                        unitPrice = line.product.sellPrice,
                                        quantity = line.quantity,
                                        discount = line.discount,
                                        lineTotal = line.lineTotal,
                                        onQty = { vm.setQuantity(line.product.id, it) },
                                        onDiscount = { vm.setDiscount(line.product.id, it) },
                                    )
                                }
                                item { HorizontalDivider() }
                            }
                            item { SectionHeader("Tap to add") }
                            items(filtered, key = { "prod_${it.id}" }) { product ->
                                ProductPickRow(product = product, onClick = { vm.addToCart(product) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckout && total > 0) {
        CheckoutDialog(
            total = total,
            partyNoun = "Customer",
            partyType = PartyType.CUSTOMER,
            parties = customers,
            confirmText = "Confirm sale",
            onCreateParty = { name, phone -> vm.createCustomer(name, phone) },
            onConfirm = { paymentType, partyId, paid, note ->
                val onDone = {
                    showCheckout = false
                    quickAmount = ""
                    scope.launch { snackbar.showSnackbar("Sale recorded") }
                    Unit
                }
                if (mode == SellMode.QUICK) {
                    vm.confirmQuickSale(total, paymentType, partyId, paid, note, onDone)
                } else {
                    vm.confirmCartSale(paymentType, partyId, paid, note, onDone)
                }
            },
            onDismiss = { showCheckout = false },
        )
    }
}

@Composable
private fun ChargeBar(total: Long, onCharge: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(Money.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        PrimaryButton(text = "Charge ${Money.format(total)}", onClick = onCharge)
    }
}

@Composable
private fun ProductPickRow(product: Product, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(Money.format(product.sellPrice), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text("Stock: ${product.stock.asQty()}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun CartLineRow(
    name: String,
    unitPrice: Long,
    quantity: Int,
    discount: Long,
    lineTotal: Long,
    onQty: (Int) -> Unit,
    onDiscount: (Long) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(Money.format(unitPrice), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(Money.format(lineTotal), fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QtyStepper(value = quantity, onValueChange = onQty, min = 0)
            OutlinedTextField(
                value = if (discount == 0L) "" else (discount / 100).toString(),
                onValueChange = { onDiscount((Money.parseToPaisa(it) ?: 0L)) },
                label = { Text("Disc ৳") },
                singleLine = true,
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

/** Reuse the quantity formatter from the product feature. */
private fun Double.asQty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
