package com.minipos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Product
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A product's stock line for the report (value = stock × buyPrice, paisa). */
data class StockLine(val product: Product, val value: Long)

/** A stock movement plus the product name (resolved). */
data class MovementRow(val movement: StockMovement, val productName: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class StockReportViewModel : ViewModel() {

    private val productRepo = ServiceLocator.productRepository
    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    private val products: StateFlow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalUnits: StateFlow<Double> = products
        .map { list -> list.sumOf { it.stock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalValue: StateFlow<Long> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeStockValue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val lines: StateFlow<List<StockLine>> = products
        .map { list -> list.map { StockLine(it, (it.stock * it.buyPrice).toLong()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movements: StateFlow<List<MovementRow>> =
        combine(
            shopIdState.filterNotNull().flatMapLatest { productRepo.observeMovements(it) },
            products,
        ) { movements, products ->
            val names = products.associateBy({ it.id }, { it.name })
            movements.map { MovementRow(it, names[it.productId]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
