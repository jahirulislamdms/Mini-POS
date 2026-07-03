package com.minipos.feature.sell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Product
import com.minipos.data.repo.SaleLineInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A product added to the sell cart. */
data class CartLine(val product: Product, val quantity: Int, val discount: Long) {
    val lineTotal: Long get() = (product.sellPrice * quantity - discount).coerceAtLeast(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SellViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val productRepo = ServiceLocator.productRepository
    private val partyRepo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val products: StateFlow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Party>> = shopIdState
        .filterNotNull()
        .flatMapLatest { partyRepo.observePartiesByType(it, PartyType.CUSTOMER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart: StateFlow<List<CartLine>> = _cart

    /** One-shot user messages (e.g. stock limit hit) for the screen to show as a snackbar. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    /** Max whole units sellable for a product (the cart uses integer quantities). */
    private fun maxSellable(product: Product): Int = product.stock.toInt().coerceAtLeast(0)

    fun addToCart(product: Product) {
        // Phase 6: never let the cart exceed available stock.
        val max = maxSellable(product)
        if (max <= 0) {
            _messages.tryEmit("${product.name} is out of stock")
            return
        }
        val existing = _cart.value.firstOrNull { it.product.id == product.id }
        if (existing == null) {
            _cart.value = _cart.value + CartLine(product, quantity = 1, discount = 0)
        } else if (existing.quantity >= max) {
            _messages.tryEmit("Only $max in stock")
        } else {
            _cart.value = _cart.value.map {
                if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
            }
        }
    }

    fun setQuantity(productId: Long, quantity: Int) {
        _cart.value = if (quantity <= 0) {
            _cart.value.filterNot { it.product.id == productId }
        } else {
            _cart.value.map {
                if (it.product.id == productId) {
                    // Phase 6: cap the requested quantity at the available stock.
                    val max = maxSellable(it.product)
                    val capped = quantity.coerceAtMost(max)
                    if (capped < quantity) _messages.tryEmit("Only $max in stock")
                    it.copy(quantity = capped)
                } else {
                    it
                }
            }
        }
    }

    fun setDiscount(productId: Long, discount: Long) {
        _cart.value = _cart.value.map {
            if (it.product.id == productId) it.copy(discount = discount.coerceAtLeast(0)) else it
        }
    }

    fun clearCart() { _cart.value = emptyList() }

    /** Add a product to the cart by id (used by the Product Details "Sell" quick action). */
    fun addProductById(productId: Long) = viewModelScope.launch {
        productRepo.getById(productId)?.let { addToCart(it) }
    }

    /** Scan-to-sell (Phase 28): find by barcode and add to the cart; [onResult] gets feedback. */
    fun addByBarcode(code: String, onResult: (String) -> Unit) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val product = productRepo.getByBarcode(shopId, code.trim())
        when {
            product == null -> onResult("No product found for this barcode")
            product.stock <= 0.0 -> onResult("${product.name} is out of stock")
            else -> {
                addToCart(product)
                onResult("Added: ${product.name}")
            }
        }
    }

    suspend fun createCustomer(name: String, phone: String?): Long {
        val shopId = shopIdState.value ?: return 0L
        return partyRepo.addParty(
            Party(shopId = shopId, name = name, phone = phone, type = PartyType.CUSTOMER, createdAt = 0),
        )
    }

    fun confirmCartSale(
        paymentType: PaymentType,
        partyId: Long?,
        paidAmount: Long,
        note: String?,
        onDone: (saleId: Long) -> Unit,
    ) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val lines = _cart.value.map {
            SaleLineInput(
                productId = it.product.id,
                name = it.product.name,
                unitPrice = it.product.sellPrice,
                quantity = it.quantity.toDouble(),
                discount = it.discount,
                lineTotal = it.lineTotal,
            )
        }
        if (lines.isEmpty()) return@launch
        val saleId = saleRepo.commitSale(shopId, lines, 0, paymentType, partyId, paidAmount, isQuickSale = false, note = note)
        _cart.value = emptyList()
        onDone(saleId)
    }

    fun confirmQuickSale(
        amount: Long,
        paymentType: PaymentType,
        partyId: Long?,
        paidAmount: Long,
        note: String?,
        onDone: (saleId: Long) -> Unit,
    ) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        if (amount <= 0) return@launch
        val line = SaleLineInput(
            productId = null,
            name = "Quick Sale",
            unitPrice = amount,
            quantity = 1.0,
            discount = 0,
            lineTotal = amount,
        )
        val saleId = saleRepo.commitSale(shopId, listOf(line), 0, paymentType, partyId, paidAmount, isQuickSale = true, note = note)
        onDone(saleId)
    }
}
