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

enum class DailyMode { SINGLE, RANGE }

data class TxnLine(val name: String, val quantity: Double, val unitPrice: Long, val lineTotal: Long)

data class TxnEntry(
    val id: Long,
    val createdAt: Long,
    val total: Long,
    val paymentLabel: String,
    val lines: List<TxnLine>,
)

data class DailyReport(
    val totalSales: Long = 0,
    val totalPurchases: Long = 0,
    val profit: Long = 0,
    val sales: List<TxnEntry> = emptyList(),
    val purchases: List<TxnEntry> = emptyList(),
)

private data class RangeKey(val shopId: Long, val start: Long, val end: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReportViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val purchaseRepo = ServiceLocator.purchaseRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(DailyMode.SINGLE)
    private val singleDateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())

    val mode: StateFlow<DailyMode> = modeState
    val singleDate: StateFlow<Long> = singleDateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: DailyMode) { modeState.value = m }
    fun setSingleDate(millis: Long) { singleDateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    /** True when the chosen custom range exceeded one month and was clamped. */
    val rangeClamped: StateFlow<Boolean> =
        combine(modeState, rangeStartState, rangeEndState) { m, s, e ->
            m == DailyMode.RANGE &&
                (DateUtil.endOfDay(maxOf(s, e)) - DateUtil.startOfDay(minOf(s, e))) > MAX_RANGE_MILLIS
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private fun effectiveRange(mode: DailyMode, single: Long, start: Long, end: Long): Pair<Long, Long> =
        when (mode) {
            DailyMode.SINGLE -> DateUtil.startOfDay(single) to DateUtil.endOfDay(single)
            DailyMode.RANGE -> {
                val s = DateUtil.startOfDay(minOf(start, end))
                val rawEnd = DateUtil.endOfDay(maxOf(start, end))
                s to minOf(rawEnd, s + MAX_RANGE_MILLIS)
            }
        }

    val report: StateFlow<DailyReport> =
        combine(
            shopIdState.filterNotNull(),
            modeState,
            singleDateState,
            rangeStartState,
            rangeEndState,
        ) { shop, m, single, st, en ->
            val (start, end) = effectiveRange(m, single, st, en)
            RangeKey(shop, start, end)
        }.flatMapLatest { key ->
            combine(
                saleRepo.observeBetween(key.shopId, key.start, key.end),
                purchaseRepo.observeBetween(key.shopId, key.start, key.end),
                saleRepo.observeProfitBetween(key.shopId, key.start, key.end),
            ) { sales, purchases, profit ->
                val saleItems = saleRepo.getItemsForShop(key.shopId).groupBy { it.saleId }
                val purchaseItems = purchaseRepo.getItemsForShop(key.shopId).groupBy { it.purchaseId }
                DailyReport(
                    totalSales = sales.sumOf { it.total },
                    totalPurchases = purchases.sumOf { it.total },
                    profit = profit,
                    sales = sales.map { s ->
                        TxnEntry(
                            id = s.id,
                            createdAt = s.createdAt,
                            total = s.total,
                            paymentLabel = label(s.paymentType),
                            lines = saleItems[s.id].orEmpty().map {
                                TxnLine(it.name, it.quantity, it.unitPrice, it.lineTotal)
                            },
                        )
                    },
                    purchases = purchases.map { p ->
                        TxnEntry(
                            id = p.id,
                            createdAt = p.createdAt,
                            total = p.total,
                            paymentLabel = label(p.paymentType),
                            lines = purchaseItems[p.id].orEmpty().map {
                                TxnLine(it.name, it.quantity, it.unitPrice, it.lineTotal)
                            },
                        )
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyReport())

    private fun label(type: PaymentType): String = if (type == PaymentType.DUE) "Due" else "Cash"

    companion object {
        private const val MAX_RANGE_MILLIS = 31L * 24 * 60 * 60 * 1000 // ~1 month
    }
}
