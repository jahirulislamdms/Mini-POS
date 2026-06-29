package com.minipos.feature.buy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Product
import com.minipos.data.repo.PurchaseLineInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A product added to the buy cart (priced at buy price; editable). */
data class BuyLine(val product: Product, val quantity: Int, val unitPrice: Long) {
    val lineTotal: Long get() = (unitPrice * quantity).coerceAtLeast(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class BuyViewModel : ViewModel() {

    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val productRepo = ServiceLocator.productRepository
    private val partyRepo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val products: StateFlow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suppliers: StateFlow<List<Party>> = shopIdState
        .filterNotNull()
        .flatMapLatest { partyRepo.observePartiesByType(it, PartyType.SUPPLIER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<List<BuyLine>>(emptyList())
    val cart: StateFlow<List<BuyLine>> = _cart

    fun addToCart(product: Product) {
        val existing = _cart.value.firstOrNull { it.product.id == product.id }
        _cart.value = if (existing == null) {
            _cart.value + BuyLine(product, quantity = 1, unitPrice = product.buyPrice)
        } else {
            _cart.value.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    fun setQuantity(productId: Long, quantity: Int) {
        _cart.value = if (quantity <= 0) {
            _cart.value.filterNot { it.product.id == productId }
        } else {
            _cart.value.map { if (it.product.id == productId) it.copy(quantity = quantity) else it }
        }
    }

    fun setUnitPrice(productId: Long, unitPrice: Long) {
        _cart.value = _cart.value.map {
            if (it.product.id == productId) it.copy(unitPrice = unitPrice.coerceAtLeast(0)) else it
        }
    }

    fun clearCart() { _cart.value = emptyList() }

    /** Add a product to the cart by id (used by the Product Details "Buy" quick action). */
    fun addProductById(productId: Long) = viewModelScope.launch {
        productRepo.getById(productId)?.let { addToCart(it) }
    }

    suspend fun createSupplier(name: String, phone: String?): Long {
        val shopId = shopIdState.value ?: return 0L
        return partyRepo.addParty(
            Party(shopId = shopId, name = name, phone = phone, type = PartyType.SUPPLIER, createdAt = 0),
        )
    }

    fun confirmPurchase(
        paymentType: PaymentType,
        partyId: Long?,
        paidAmount: Long,
        note: String?,
        onDone: () -> Unit,
    ) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val lines = _cart.value.map {
            PurchaseLineInput(
                productId = it.product.id,
                name = it.product.name,
                unitPrice = it.unitPrice,
                quantity = it.quantity.toDouble(),
                discount = 0,
                lineTotal = it.lineTotal,
            )
        }
        if (lines.isEmpty()) return@launch
        purchaseRepo.commitPurchase(shopId, lines, 0, paymentType, partyId, paidAmount, note)
        _cart.value = emptyList()
        onDone()
    }
}
