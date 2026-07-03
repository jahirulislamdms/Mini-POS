package com.minipos.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Category
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Read-only product detail: live product + categories (for resolving category/sub-category names). */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModel : ViewModel() {

    private val productRepo = ServiceLocator.productRepository
    private val categoryRepo = ServiceLocator.categoryRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val productIdState = MutableStateFlow<Long?>(null)

    fun set(shopId: Long, productId: Long) {
        shopIdState.value = shopId
        productIdState.value = productId
    }

    val product: StateFlow<Product?> = productIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<Category>> = shopIdState
        .filterNotNull()
        .flatMapLatest { categoryRepo.observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Manual stock adjustment (Phase 8) — same business logic as the Edit screen:
     * delegates to the shared [ProductRepository.adjustStock] (records a StockMovement, never a
     * sale). The [product] flow re-emits afterward, so Product Details updates the stock instantly.
     */
    fun applyStockChange(product: Product, delta: Double, note: String?) = viewModelScope.launch {
        productRepo.adjustStock(
            shopId = product.shopId,
            productId = product.id,
            delta = delta,
            type = MovementType.ADJUSTMENT,
            note = note,
        )
    }
}
