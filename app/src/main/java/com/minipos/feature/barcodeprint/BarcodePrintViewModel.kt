package com.minipos.feature.barcodeprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.SearchUtil
import com.minipos.data.entity.Category
import com.minipos.data.entity.Product
import com.minipos.feature.product.ProductFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Barcode label printing (Phase 28): pick products + label counts, choose the label fields and
 * sheet geometry, then export a print-ready PDF. Uses the products' existing barcodes only —
 * nothing is generated or duplicated here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BarcodePrintViewModel : ViewModel() {

    private val productRepo = ServiceLocator.productRepository
    private val categoryRepo = ServiceLocator.categoryRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    /** All products of the shop (unfiltered — used to resolve selections when generating). */
    val allProducts: StateFlow<List<Product>> = shopIdState
        .filterNotNull()
        .flatMapLatest { productRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = shopIdState
        .filterNotNull()
        .flatMapLatest { categoryRepo.observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Phase 28.1: shared smart search + Category/Subcategory multi-filter (same as Products page).
    private val queryState = MutableStateFlow("")
    private val selectedCategoryIdsState = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedSubCategoryIdsState = MutableStateFlow<Set<Long>>(emptySet())

    val query: StateFlow<String> = queryState
    val selectedCategoryIds: StateFlow<Set<Long>> = selectedCategoryIdsState
    val selectedSubCategoryIds: StateFlow<Set<Long>> = selectedSubCategoryIdsState

    fun setQuery(q: String) { queryState.value = q }

    fun toggleCategory(id: Long) {
        selectedCategoryIdsState.value =
            if (id in selectedCategoryIdsState.value) selectedCategoryIdsState.value - id
            else selectedCategoryIdsState.value + id
    }

    fun toggleSubCategory(id: Long) {
        selectedSubCategoryIdsState.value =
            if (id in selectedSubCategoryIdsState.value) selectedSubCategoryIdsState.value - id
            else selectedSubCategoryIdsState.value + id
    }

    fun clearFilters() {
        selectedCategoryIdsState.value = emptySet()
        selectedSubCategoryIdsState.value = emptySet()
    }

    /** The visible product list: filters then search (name or barcode), instantly updating. */
    val products: StateFlow<List<Product>> = kotlinx.coroutines.flow.combine(
        allProducts, queryState, selectedCategoryIdsState, selectedSubCategoryIdsState,
    ) { list, q, cats, subs ->
        SearchUtil.filter(ProductFilters.apply(list, cats, subs), q) { listOf(it.name, it.barcode) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** productId → number of labels to print (present = selected). */
    private val selectionState = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val selection: StateFlow<Map<Long, Int>> = selectionState

    fun toggleProduct(id: Long) {
        selectionState.value = selectionState.value.toMutableMap().apply {
            if (containsKey(id)) remove(id) else put(id, 1)
        }
    }

    fun setLabelCount(id: Long, count: Int) {
        if (count <= 0) {
            selectionState.value = selectionState.value - id
        } else {
            selectionState.value = selectionState.value + (id to count)
        }
    }

    /** Select every currently *visible* (searched/filtered) product, keeping prior selections. */
    fun selectAll() {
        val current = selectionState.value.toMutableMap()
        products.value.forEach { p -> if (!current.containsKey(p.id)) current[p.id] = 1 }
        selectionState.value = current
    }

    fun clearSelection() { selectionState.value = emptyMap() }

    /** Label field toggles. */
    val options = MutableStateFlow(LabelOptions())
    fun setOptions(o: LabelOptions) { options.value = o }

    /** Scan-to-select (Phase 28.1): look a product up by its exact barcode. */
    fun findByBarcode(code: String, onResult: (Product?) -> Unit) = viewModelScope.launch {
        val shopId = shopIdState.value
        onResult(if (shopId == null) null else productRepo.getByBarcode(shopId, code.trim()))
    }

    /**
     * Build the PDF bytes off the main thread. [onError]/[onReady] are invoked on the caller's
     * (main) dispatcher.
     */
    fun generatePdf(
        layout: LabelLayout,
        onReady: (ByteArray) -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        val selected = selectionState.value
        if (selected.isEmpty()) {
            onError("Select at least one product.")
            return@launch
        }
        val productById = allProducts.value.associateBy { it.id }
        val categoryNames = categories.value.associateBy({ it.id }, { it.name })
        val labels = selected.entries.flatMap { (id, count) ->
            val p = productById[id] ?: return@flatMap emptyList()
            val barcode = p.barcode ?: return@flatMap emptyList()
            List(count) {
                LabelData(
                    barcode = barcode,
                    name = p.name,
                    category = p.categoryId?.let(categoryNames::get),
                    subcategory = p.subCategoryId?.let(categoryNames::get),
                    sellPrice = p.sellPrice,
                    buyPrice = p.buyPrice,
                )
            }
        }
        if (labels.isEmpty()) {
            onError("The selected products have no barcodes yet.")
            return@launch
        }
        val bytes = withContext(Dispatchers.Default) {
            val doc = LabelPdf.generate(labels, options.value, layout)
            ByteArrayOutputStream().use { out ->
                doc.writeTo(out)
                doc.close()
                out.toByteArray()
            }
        }
        onReady(bytes)
    }
}
