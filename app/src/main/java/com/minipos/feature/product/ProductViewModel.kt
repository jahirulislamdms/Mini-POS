package com.minipos.feature.product

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.ImageStorage
import com.minipos.core.util.SearchUtil
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

    // Phase 24: multi-select Category / Subcategory filters. Empty sets = no filter (all products).
    private val selectedCategoryIdsState = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedSubCategoryIdsState = MutableStateFlow<Set<Long>>(emptySet())

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setQuery(q: String) { queryState.value = q }

    fun toggleCategory(id: Long) {
        selectedCategoryIdsState.value = selectedCategoryIdsState.value.toggle(id)
    }

    fun toggleSubCategory(id: Long) {
        selectedSubCategoryIdsState.value = selectedSubCategoryIdsState.value.toggle(id)
    }

    fun clearFilters() {
        selectedCategoryIdsState.value = emptySet()
        selectedSubCategoryIdsState.value = emptySet()
    }

    private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

    val query: StateFlow<String> = queryState
    val selectedCategoryIds: StateFlow<Set<Long>> = selectedCategoryIdsState
    val selectedSubCategoryIds: StateFlow<Set<Long>> = selectedSubCategoryIdsState

    private val baseProducts: Flow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }

    /**
     * Products after the Category/Subcategory filters (Phase 24). Union semantics: a product
     * matches when its category is among the selected categories OR its subcategory is among
     * the selected subcategories. No selection = all products.
     */
    private val filteredProducts: Flow<List<Product>> = combine(
        baseProducts, selectedCategoryIdsState, selectedSubCategoryIdsState,
    ) { list, cats, subs ->
        ProductFilters.apply(list, cats, subs)
    }

    /** Filtered products after also applying the search query (smart search, Phase 22). */
    val products: StateFlow<List<Product>> =
        combine(filteredProducts, queryState) { list, q ->
            SearchUtil.filter(list, q) { listOf(it.name) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Inventory summary for the Products page header (Phase 4, updated by Phase 24): totals of
     * the **filtered** products — with no filters that is all products (same numbers as the
     * Stock Report). The search box does not affect the summary. Same calculation as the
     * Stock Report lines: Σ stock and Σ (stock × buyPrice).
     */
    val totalUnits: StateFlow<Double> = filteredProducts
        .map { list -> list.sumOf { it.stock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalStockValue: StateFlow<Long> = filteredProducts
        .map { list -> list.sumOf { (it.stock * it.buyPrice).toLong() } }
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

    /** The shop's default unit (Phase 31) — pre-selected on new products, changeable per product. */
    val defaultUnit: StateFlow<String> = shopIdState
        .filterNotNull()
        .flatMapLatest { shopRepo.observeSettings(it) }
        .map { it?.defaultUnit ?: "pcs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    suspend fun loadForEdit(productId: Long): Product? = productRepo.getById(productId)

    fun observeProduct(productId: Long): Flow<Product?> = productRepo.observeById(productId)

    fun movementsFor(shopId: Long, productId: Long): Flow<List<StockMovement>> =
        productRepo.observeMovementsForProduct(shopId, productId)

    /**
     * Create or update a product. Phase 28: a blank barcode is auto-generated; a duplicate
     * barcode (another product of the shop) blocks the save. Returns an error message or null.
     */
    suspend fun save(product: Product, photoUri: Uri?): String? {
        val barcode = productRepo.ensureBarcode(product.shopId, product.barcode)
        if (productRepo.isBarcodeTaken(product.shopId, barcode, excludeProductId = product.id)) {
            return "This barcode is already used by another product."
        }
        val context = getApplication<Application>()
        val withBarcode = product.copy(barcode = barcode)
        val withPhoto = if (photoUri != null) {
            withBarcode.copy(photoPath = ImageStorage.copyProductPhoto(context, product.shopId, photoUri))
        } else {
            withBarcode
        }
        if (withPhoto.id == 0L) productRepo.add(withPhoto) else productRepo.update(withPhoto)
        return null
    }

    /** Look a product up by its exact barcode (scanner flows, Phase 28). */
    fun findByBarcode(code: String, onResult: (Product?) -> Unit) = viewModelScope.launch {
        val shopId = shopIdState.value
        onResult(if (shopId == null) null else productRepo.getByBarcode(shopId, code.trim()))
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
