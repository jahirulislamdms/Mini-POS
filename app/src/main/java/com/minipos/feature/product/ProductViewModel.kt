package com.minipos.feature.product

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.ImageStorage
import com.minipos.data.entity.Category
import com.minipos.data.entity.MeasureUnit
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.Product
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModel(app: Application) : AndroidViewModel(app) {

    private val productRepo = ServiceLocator.productRepository
    private val categoryRepo = ServiceLocator.categoryRepository
    private val unitRepo = ServiceLocator.unitRepository
    private val shopRepo = ServiceLocator.shopRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val queryState = MutableStateFlow("")
    private val categoryFilterState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setQuery(q: String) { queryState.value = q }
    fun setCategoryFilter(categoryId: Long?) { categoryFilterState.value = categoryId }

    val query: StateFlow<String> = queryState
    val categoryFilter: StateFlow<Long?> = categoryFilterState

    private val baseProducts: Flow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }

    /** Products after applying the search query + category filter. */
    val products: StateFlow<List<Product>> =
        combine(baseProducts, queryState, categoryFilterState) { list, q, cat ->
            list.filter { p ->
                (q.isBlank() || p.name.contains(q, ignoreCase = true)) &&
                    (cat == null || p.categoryId == cat)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Shop-wide inventory summary for the Products page header (Future Updates Phase 4).
     * Unaffected by the search/category filter — mirrors the Stock Report's totals so the
     * numbers always match. Both update automatically as inventory changes.
     */
    val totalUnits: StateFlow<Double> = baseProducts
        .map { list -> list.sumOf { it.stock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalStockValue: StateFlow<Long> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeStockValue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val categories: StateFlow<List<Category>> = shopIdState
        .filterNotNull()
        .flatMapLatest { categoryRepo.observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val units: StateFlow<List<MeasureUnit>> = shopIdState
        .filterNotNull()
        .flatMapLatest { unitRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lowStockDefault: StateFlow<Double> = shopIdState
        .filterNotNull()
        .flatMapLatest { shopRepo.observeSettings(it) }
        .map { it?.lowStockDefault ?: 5.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5.0)

    suspend fun loadForEdit(productId: Long): Product? = productRepo.getById(productId)

    fun observeProduct(productId: Long): Flow<Product?> = productRepo.observeById(productId)

    fun movementsFor(shopId: Long, productId: Long): Flow<List<StockMovement>> =
        productRepo.observeMovementsForProduct(shopId, productId)

    /** Create or update a product, copying a newly picked photo into app storage first. */
    suspend fun save(product: Product, photoUri: Uri?) {
        val context = getApplication<Application>()
        val withPhoto = if (photoUri != null) {
            product.copy(photoPath = ImageStorage.copyProductPhoto(context, product.shopId, photoUri))
        } else {
            product
        }
        if (withPhoto.id == 0L) productRepo.add(withPhoto) else productRepo.update(withPhoto)
    }

    fun delete(product: Product) = viewModelScope.launch {
        productRepo.delete(product)
        product.photoPath?.let { ImageStorage.deleteFile(getApplication(), it) }
    }

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
