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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class BuyReportMode { DAY, MONTH, CUSTOM }

/** All purchases (Cash + Due) in a period, with a period total. Reuses [TxnEntry] / [TxnLine]. */
data class BuyReportData(
    val totalBuy: Long = 0,
    val purchases: List<TxnEntry> = emptyList(),
)

private data class BuyRangeKey(val shopId: Long, val start: Long, val end: Long)

/** Buy Report (Phase 19): reads existing purchase data; Day / Month / Custom filters + total. */
@OptIn(ExperimentalCoroutinesApi::class)
class BuyReportViewModel : ViewModel() {

    private val purchaseRepo = ServiceLocator.purchaseRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(BuyReportMode.DAY)
    private val dateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())

    val mode: StateFlow<BuyReportMode> = modeState
    val date: StateFlow<Long> = dateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: BuyReportMode) { modeState.value = m }
    fun setDate(millis: Long) { dateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    private fun rangeFor(mode: BuyReportMode, date: Long, start: Long, end: Long): Pair<Long, Long> =
        when (mode) {
            BuyReportMode.DAY -> DateUtil.startOfDay(date) to DateUtil.endOfDay(date)
            BuyReportMode.MONTH -> DateUtil.startOfMonth(date) to DateUtil.endOfMonth(date)
            BuyReportMode.CUSTOM ->
                DateUtil.startOfDay(minOf(start, end)) to DateUtil.endOfDay(maxOf(start, end))
        }

    val report: StateFlow<BuyReportData> = combine(
        shopIdState.filterNotNull(),
        modeState,
        dateState,
        rangeStartState,
        rangeEndState,
    ) { shop, m, d, s, e ->
        val (start, end) = rangeFor(m, d, s, e)
        BuyRangeKey(shop, start, end)
    }.flatMapLatest { key ->
        purchaseRepo.observeBetween(key.shopId, key.start, key.end).map { purchases ->
            val items = purchaseRepo.getItemsForShop(key.shopId).groupBy { it.purchaseId }
            BuyReportData(
                totalBuy = purchases.sumOf { it.total },
                purchases = purchases.map { p ->
                    TxnEntry(
                        id = p.id,
                        createdAt = p.createdAt,
                        total = p.total,
                        paymentLabel = if (p.paymentType == PaymentType.DUE) "Due" else "Cash",
                        lines = items[p.id].orEmpty().map {
                            TxnLine(it.name, it.quantity, it.unitPrice, it.lineTotal)
                        },
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuyReportData())
}
