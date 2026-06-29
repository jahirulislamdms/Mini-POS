package com.minipos.feature.sell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnYellow
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppDropdown
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.CheckoutDialog
import com.minipos.core.ui.EmptyState
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.QtyStepper
import com.minipos.core.ui.SectionHeader
import com.minipos.core.util.Money
import com.minipos.data.entity.Party
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
    var showCheckout by remember { mutableStateOf(false) }   // Quick Sell → payment
    var showCartSummary by remember { mutableStateOf(false) } // Products → Review sale popup

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Stock-limit (and similar) messages from the cart logic.
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val cartTotal = cart.sumOf { it.lineTotal }
    val quickTotal = Money.parseToPaisa(quickAmount) ?: 0L

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
            // Compact "Continue" bar — keeps the product list as the primary focus.
            when (mode) {
                SellMode.QUICK -> if (quickTotal > 0) {
                    ContinueBar(itemCount = null, total = quickTotal, onContinue = { showCheckout = true })
                }
                SellMode.PRODUCTS -> if (cart.isNotEmpty()) {
                    ContinueBar(itemCount = cart.size, total = cartTotal, onContinue = { showCartSummary = true })
                }
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
                    // Out-of-stock products are hidden from the Sell list — you can't sell what isn't in stock.
                    val filtered = products.filter {
                        (search.isBlank() || it.name.contains(search, ignoreCase = true)) && it.stock > 0.0
                    }
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
                            items(filtered, key = { "prod_${it.id}" }) { product ->
                                val inCartQty = cart.firstOrNull { it.product.id == product.id }?.quantity ?: 0
                                ProductPickRow(
                                    product = product,
                                    inCartQty = inCartQty,
                                    onClick = { vm.addToCart(product) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Sell → existing shared Cash/Due checkout.
    if (showCheckout && quickTotal > 0) {
        CheckoutDialog(
            total = quickTotal,
            partyNoun = "Customer",
            partyType = PartyType.CUSTOMER,
            parties = customers,
            confirmText = "Confirm sale",
            onCreateParty = { name, phone -> vm.createCustomer(name, phone) },
            onConfirm = { paymentType, partyId, paid, note ->
                vm.confirmQuickSale(quickTotal, paymentType, partyId, paid, note) {
                    showCheckout = false
                    quickAmount = ""
                    scope.launch { snackbar.showSnackbar("Sale recorded") }
                }
            },
            onDismiss = { showCheckout = false },
        )
    }

    // Products → Review sale popup: scrollable cart summary + payment, sticky Confirm Sell.
    if (showCartSummary && cart.isNotEmpty()) {
        CartSummaryDialog(
            cart = cart,
            customers = customers,
            onQty = { id, qty -> vm.setQuantity(id, qty) },
            onDiscount = { id, disc -> vm.setDiscount(id, disc) },
            onCreateCustomer = { name, phone -> vm.createCustomer(name, phone) },
            onConfirm = { paymentType, partyId, paid, note ->
                vm.confirmCartSale(paymentType, partyId, paid, note) {
                    showCartSummary = false
                    scope.launch { snackbar.showSnackbar("Sale recorded") }
                }
            },
            onDismiss = { showCartSummary = false },
        )
    }
}

/** Compact bottom bar: item count + total and a single Continue action. */
@Composable
private fun ContinueBar(itemCount: Int?, total: Long, onContinue: () -> Unit) {
    Surface(color = AppBackground, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val label = if (itemCount != null) "$itemCount item${if (itemCount == 1) "" else "s"}" else "Total"
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            PrimaryButton(text = "Continue", onClick = onContinue)
        }
    }
}

@Composable
private fun ProductPickRow(product: Product, inCartQty: Int, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(Money.format(product.sellPrice), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Stock: ${product.stock.asQty()}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                if (inCartQty > 0) {
                    Surface(color = BrandYellow, shape = RoundedCornerShape(50)) {
                        Text(
                            "In cart: $inCartQty",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnYellow,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
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
    maxQty: Int,
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
            QtyStepper(value = quantity, onValueChange = onQty, min = 0, max = maxQty)
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

/**
 * Review-sale popup (Future Updates Phase 5): a scrollable cart summary (per-line qty/discount,
 * line totals, grand total) plus Cash/Due payment, with a **sticky Confirm Sell** button at the
 * bottom. The sale is committed only when Confirm Sell is pressed.
 */
@Composable
private fun CartSummaryDialog(
    cart: List<CartLine>,
    customers: List<Party>,
    onQty: (Long, Int) -> Unit,
    onDiscount: (Long, Long) -> Unit,
    onCreateCustomer: suspend (String, String?) -> Long,
    onConfirm: (PaymentType, Long?, Long, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var paymentType by remember { mutableStateOf(PaymentType.CASH) }
    var selectedParty by remember { mutableStateOf<Party?>(null) }
    var showNewParty by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var paidNow by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val total = cart.sumOf { it.lineTotal }
    val isDue = paymentType == PaymentType.DUE
    val confirmEnabled = cart.isNotEmpty() && (!isDue || selectedParty != null)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 600.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Review sale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()

                // Scrollable middle — cart items + payment options.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader("Items")
                    cart.forEach { line ->
                        CartLineRow(
                            name = line.product.name,
                            unitPrice = line.product.sellPrice,
                            quantity = line.quantity,
                            discount = line.discount,
                            lineTotal = line.lineTotal,
                            maxQty = line.product.stock.toInt().coerceAtLeast(0),
                            onQty = { onQty(line.product.id, it) },
                            onDiscount = { onDiscount(line.product.id, it) },
                        )
                    }

                    SectionHeader("Payment")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!isDue, { paymentType = PaymentType.CASH }, label = { Text("Cash") })
                        FilterChip(isDue, { paymentType = PaymentType.DUE }, label = { Text("Due") })
                    }
                    if (isDue) {
                        AppDropdown(
                            label = "Customer",
                            options = customers,
                            selected = selectedParty,
                            optionLabel = { it.name },
                            onSelected = { selectedParty = it },
                            includeNone = true,
                            noneLabel = "Select customer",
                            onNoneSelected = { selectedParty = null },
                        )
                        TextButton(onClick = { showNewParty = !showNewParty }) {
                            Text(if (showNewParty) "Cancel" else "+ New customer")
                        }
                        if (showNewParty) {
                            AppTextField(newName, { newName = it }, "Customer name")
                            AppTextField(newPhone, { newPhone = it }, "Phone (optional)", keyboardType = KeyboardType.Phone)
                            TextButton(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        val nm = newName.trim()
                                        val ph = newPhone.trim().ifBlank { null }
                                        scope.launch {
                                            val id = onCreateCustomer(nm, ph)
                                            selectedParty = Party(
                                                id = id, shopId = 0, name = nm, phone = ph,
                                                type = PartyType.CUSTOMER, createdAt = 0,
                                            )
                                            showNewParty = false
                                            newName = ""
                                            newPhone = ""
                                        }
                                    }
                                },
                            ) { Text("Add customer") }
                        }
                        AppTextField(paidNow, { paidNow = it }, "Paid now (optional)", keyboardType = KeyboardType.Number)
                    }
                    AppTextField(note, { note = it }, "Note (optional)")
                }

                HorizontalDivider()

                // Sticky bottom — grand total + Confirm Sell (the only action that commits the sale).
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Grand total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(Money.format(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    PrimaryButton(
                        text = "Confirm Sell ${Money.format(total)}",
                        enabled = confirmEnabled,
                        onClick = {
                            val paid = if (isDue) (Money.parseToPaisa(paidNow) ?: 0L) else total
                            onConfirm(
                                paymentType,
                                if (isDue) selectedParty?.id else null,
                                paid,
                                note.trim().ifBlank { null },
                            )
                        },
                    )
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

/** Reuse the quantity formatter from the product feature. */
private fun Double.asQty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
