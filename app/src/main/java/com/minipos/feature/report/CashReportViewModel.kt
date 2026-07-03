package com.minipos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.CashType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class CashReportMode { DAY, MONTH, CUSTOM }

data class CashReportData(
    val cashIn: Long = 0,
    val cashOut: Long = 0,
    val items: List<CashTransaction> = emptyList(),
)

private data class CashRangeKey(val shopId: Long, val start: Long, val end: Long)

/** Cash Management Report (Phase 17): Cash In/Out by single day, month, or custom range. */
@OptIn(ExperimentalCoroutinesApi::class)
class CashReportViewModel : ViewModel() {

    private val repo = ServiceLocator.cashRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(CashReportMode.DAY)
    private val dateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())

    val mode: StateFlow<CashReportMode> = modeState
    val date: StateFlow<Long> = dateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: CashReportMode) { modeState.value = m }
    fun setDate(millis: Long) { dateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    private fun rangeFor(mode: CashReportMode, date: Long, start: Long, end: Long): Pair<Long, Long> =
        when (mode) {
            CashReportMode.DAY -> DateUtil.startOfDay(date) to DateUtil.endOfDay(date)
            CashReportMode.MONTH -> DateUtil.startOfMonth(date) to DateUtil.endOfMonth(date)
            CashReportMode.CUSTOM ->
                DateUtil.startOfDay(minOf(start, end)) to DateUtil.endOfDay(maxOf(start, end))
        }

    val report: StateFlow<CashReportData> = combine(
        shopIdState.filterNotNull(),
        modeState,
        dateState,
        rangeStartState,
        rangeEndState,
    ) { shop, m, d, s, e ->
        val (start, end) = rangeFor(m, d, s, e)
        CashRangeKey(shop, start, end)
    }.flatMapLatest { key ->
        repo.observeBetween(key.shopId, key.start, key.end).map { list ->
            CashReportData(
                cashIn = list.filter { it.type == CashType.CASH_IN }.sumOf { it.amount },
                cashOut = list.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount },
                items = list,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashReportData())
}
