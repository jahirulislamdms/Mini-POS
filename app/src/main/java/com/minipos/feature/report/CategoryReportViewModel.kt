package com.minipos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class CategoryReportMode { DAY, MONTH, CUSTOM }

/** Per-product line of the Category Report. */
data class CategoryRow(
    val productId: Long,
    val name: String,
    val qtyPurchased: Double,
    val purchaseAmount: Long,
    val qtySold: Double,
    val salesAmount: Long,
    val profit: Long,
)

data class CategoryReportData(
    val rows: List<CategoryRow> = emptyList(),
    val totalQtyPurchased: Double = 0.0,
    val totalPurchase: Long = 0,
    val totalQtySold: Double = 0.0,
    val totalSales: Long = 0,
    val totalProfit: Long = 0,
    // Phase 23.1: current stock of the selection (same calc as the Products page: Σ stock, Σ stock×buyPrice).
    val stockQty: Double = 0.0,
    val stockValue: Long = 0,
)

private data class CatRangeKey(val shopId: Long, val start: Long, val end: Long)

/**
 * Category Report (Phase 23): purchase/sales quantity, amounts and profit per product for a
 * selected Category (+ Subcategory or All Subcategories), over Day / Month / Custom ranges.
 * Reuses the existing sales/purchase data; profit uses the app-wide convention
 * (sales amount − buyPrice × qty sold), matching the Business/Daily reports.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryReportViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val productRepo = ServiceLocator.productRepository
    private val categoryRepo = ServiceLocator.categoryRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(CategoryReportMode.DAY)
    private val dateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())
    private val categoryIdState = MutableStateFlow<Long?>(null)
    private val subCategoryIdState = MutableStateFlow<Long?>(null) // null = All Subcategories

    val mode: StateFlow<CategoryReportMode> = modeState
    val date: StateFlow<Long> = dateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState
    val categoryId: StateFlow<Long?> = categoryIdState
    val subCategoryId: StateFlow<Long?> = subCategoryIdState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: CategoryReportMode) { modeState.value = m }
    fun setDate(millis: Long) { dateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    fun setCategory(id: Long?) {
        categoryIdState.value = id
        subCategoryIdState.value = null // switching category resets to All Subcategories
    }

    fun setSubCategory(id: Long?) { subCategoryIdState.value = id }

    /** All categories of the shop (screen splits into top-level + subs of the selection). */
    val categories: StateFlow<List<Category>> = shopIdState
        .filterNotNull()
        .flatMapLatest { categoryRepo.observeAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun rangeFor(mode: CategoryReportMode, date: Long, start: Long, end: Long): Pair<Long, Long> =
        when (mode) {
            CategoryReportMode.DAY -> DateUtil.startOfDay(date) to DateUtil.endOfDay(date)
            CategoryReportMode.MONTH -> DateUtil.startOfMonth(date) to DateUtil.endOfMonth(date)
            CategoryReportMode.CUSTOM ->
                DateUtil.startOfDay(minOf(start, end)) to DateUtil.endOfDay(maxOf(start, end))
        }

    private val rangeKey = combine(
        shopIdState.filterNotNull(), modeState, dateState, rangeStartState, rangeEndState,
    ) { shop, m, d, s, e ->
        val (start, end) = rangeFor(m, d, s, e)
        CatRangeKey(shop, start, end)
    }

    private val selection = combine(categoryIdState, subCategoryIdState) { c, s -> c to s }

    val report: StateFlow<CategoryReportData> =
        combine(rangeKey, selection) { key, sel -> key to sel }
            .flatMapLatest { (key, sel) ->
                combine(
                    saleRepo.observeBetween(key.shopId, key.start, key.end),
                    purchaseRepo.observeBetween(key.shopId, key.start, key.end),
                    productRepo.observeByShop(key.shopId),
                ) { sales, purchases, products ->
                    val catId = sel.first ?: return@combine CategoryReportData()

                    // Products in the selected category (+ specific subcategory, if chosen).
                    val matching = products.filter { p ->
                        p.categoryId == catId && (sel.second == null || p.subCategoryId == sel.second)
                    }
                    if (matching.isEmpty()) return@combine CategoryReportData()

                    // Line items of the transactions inside the range, grouped per product.
                    val saleIds = sales.mapTo(HashSet()) { it.id }
                    val purchaseIds = purchases.mapTo(HashSet()) { it.id }
                    val soldByProduct = saleRepo.getItemsForShop(key.shopId)
                        .filter { it.saleId in saleIds && it.productId != null }
                        .groupBy { it.productId!! }
                    val boughtByProduct = purchaseRepo.getItemsForShop(key.shopId)
                        .filter { it.purchaseId in purchaseIds && it.productId != null }
                        .groupBy { it.productId!! }

                    val rows = matching.map { p ->
                        val sold = soldByProduct[p.id].orEmpty()
                        val bought = boughtByProduct[p.id].orEmpty()
                        val qtySold = sold.sumOf { it.quantity }
                        val salesAmount = sold.sumOf { it.lineTotal }
                        CategoryRow(
                            productId = p.id,
                            name = p.name,
                            qtyPurchased = bought.sumOf { it.quantity },
                            purchaseAmount = bought.sumOf { it.lineTotal },
                            qtySold = qtySold,
                            salesAmount = salesAmount,
                            // App-wide profit convention: sales − cost (buyPrice × qty sold).
                            profit = salesAmount - (p.buyPrice * qtySold).toLong(),
                        )
                    }
                    CategoryReportData(
                        rows = rows,
                        totalQtyPurchased = rows.sumOf { it.qtyPurchased },
                        totalPurchase = rows.sumOf { it.purchaseAmount },
                        totalQtySold = rows.sumOf { it.qtySold },
                        totalSales = rows.sumOf { it.salesAmount },
                        totalProfit = rows.sumOf { it.profit },
                        stockQty = matching.sumOf { it.stock },
                        stockValue = matching.sumOf { (it.stock * it.buyPrice).toLong() },
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryReportData())
}
