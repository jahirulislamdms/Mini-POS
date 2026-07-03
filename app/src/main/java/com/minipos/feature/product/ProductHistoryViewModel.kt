package com.minipos.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.Product
import com.minipos.data.repo.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** One movement, plus the stock balance right after it (computed from current stock). */
data class ProductHistoryRow(
    val id: Long,
    val type: MovementType,
    val change: Double,
    val balanceAfter: Double,
    val note: String?,
    val createdAt: Long,
)

/**
 * Read-only product movement history (Phase 13): purchases, sales and stock adjustments for one
 * product over the last 30 days, newest first. Reuses the existing `stock_movements` data and only
 * *filters* to the retention window — older rows are kept (so the Stock Report is unaffected).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductHistoryViewModel : ViewModel() {

    private val productRepo = ServiceLocator.productRepository

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

    private val movements = combine(
        shopIdState.filterNotNull(),
        productIdState.filterNotNull(),
    ) { shop, product -> shop to product }
        .flatMapLatest { (shop, product) ->
            productRepo.observeMovementsForProductSince(
                shop, product, System.currentTimeMillis() - RETENTION_MILLIS,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** History rows (newest first) with a running stock balance anchored on the current stock. */
    val rows: StateFlow<List<ProductHistoryRow>> = combine(product, movements) { p, list ->
        var running = p?.stock ?: 0.0
        list.map { m ->
            val after = running
            running -= m.change          // balance before this movement = balance after the older one
            ProductHistoryRow(m.id, m.type, m.change, after, m.note, m.createdAt)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        /** Keep the last 30 days of history (single source: [ProductRepository]). */
        const val RETENTION_MILLIS = ProductRepository.MOVEMENT_RETENTION_MILLIS
    }
}
