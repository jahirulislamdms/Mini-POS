package com.minipos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.PaymentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class SalesReportMode { DAY, MONTH, CUSTOM }

/** One invoice in the report: date/time, invoice no, customer, totals — plus every product sold (Phase 37). */
data class SaleReportEntry(
    val id: Long,
    val createdAt: Long,
    val invoiceNo: String,
    val customerName: String?,
    val total: Long,
    val paymentLabel: String,
    val paid: Long,
    val due: Long,
    val lines: List<TxnLine>,
)

/** All sales (Cash + Due) in a period, with period totals (profit reuses the ledger/report SQL). */
data class SalesReportData(
    val totalSales: Long = 0,
    val totalProfit: Long = 0,
    val sales: List<SaleReportEntry> = emptyList(),
)

private data class SalesRangeKey(val shopId: Long, val start: Long, val end: Long)

/**
 * Sales Report (Phase 35): reads existing sale data — same Day / Month / Custom structure as
 * [BuyReportViewModel]. Invoice numbers use the receipt format (INV-S000123); customer names
 * come from the sale's party when the sale was on due.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalesReportViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val partyRepo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(SalesReportMode.DAY)
    private val dateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())

    val mode: StateFlow<SalesReportMode> = modeState
    val date: StateFlow<Long> = dateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: SalesReportMode) { modeState.value = m }
    fun setDate(millis: Long) { dateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    private fun rangeFor(mode: SalesReportMode, date: Long, start: Long, end: Long): Pair<Long, Long> =
        when (mode) {
            SalesReportMode.DAY -> DateUtil.startOfDay(date) to DateUtil.endOfDay(date)
            SalesReportMode.MONTH -> DateUtil.startOfMonth(date) to DateUtil.endOfMonth(date)
            SalesReportMode.CUSTOM ->
                DateUtil.startOfDay(minOf(start, end)) to DateUtil.endOfDay(maxOf(start, end))
        }

    val report: StateFlow<SalesReportData> = combine(
        shopIdState.filterNotNull(),
        modeState,
        dateState,
        rangeStartState,
        rangeEndState,
    ) { shop, m, d, s, e ->
        val (start, end) = rangeFor(m, d, s, e)
        SalesRangeKey(shop, start, end)
    }.flatMapLatest { key ->
        combine(
            saleRepo.observeBetween(key.shopId, key.start, key.end),
            partyRepo.observeParties(key.shopId),
            // Phase 37: same profit query the Daily/Business reports use — no duplicate math.
            saleRepo.observeProfitBetween(key.shopId, key.start, key.end),
        ) { sales, parties, profit ->
            val partyNames = parties.associate { it.id to it.name }
            val items = saleRepo.getItemsForShop(key.shopId).groupBy { it.saleId }
            SalesReportData(
                totalSales = sales.sumOf { it.total },
                totalProfit = profit,
                sales = sales.map { sale ->
                    SaleReportEntry(
                        id = sale.id,
                        createdAt = sale.createdAt,
                        invoiceNo = "INV-S%06d".format(sale.id),
                        customerName = sale.partyId?.let { partyNames[it] },
                        total = sale.total,
                        paymentLabel = if (sale.paymentType == PaymentType.DUE) "Due" else "Cash",
                        paid = sale.paidAmount,
                        due = sale.dueAmount,
                        lines = items[sale.id].orEmpty().map {
                            TxnLine(it.name, it.quantity, it.unitPrice, it.lineTotal)
                        },
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SalesReportData())
}
