package com.minipos.feature.cashdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.data.repo.CashDrawerDay
import com.minipos.data.repo.DrawerTxn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class DrawerMode { DAY, MONTH, CUSTOM }

/** A day transaction plus the drawer balance right after it (Phase 27.1). */
data class DrawerTxnRow(val txn: DrawerTxn, val balanceAfter: Long)

/** Cash Drawer (Phase 27): today's (or any day's) cash movement + history, on live data. */
@OptIn(ExperimentalCoroutinesApi::class)
class CashDrawerViewModel : ViewModel() {

    private val repo = ServiceLocator.cashDrawerRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val modeState = MutableStateFlow(DrawerMode.DAY)
    private val dateState = MutableStateFlow(System.currentTimeMillis())
    private val rangeStartState = MutableStateFlow(System.currentTimeMillis())
    private val rangeEndState = MutableStateFlow(System.currentTimeMillis())

    val mode: StateFlow<DrawerMode> = modeState
    val date: StateFlow<Long> = dateState
    val rangeStart: StateFlow<Long> = rangeStartState
    val rangeEnd: StateFlow<Long> = rangeEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setMode(m: DrawerMode) { modeState.value = m }
    fun setDate(millis: Long) { dateState.value = millis }
    fun setRangeStart(millis: Long) { rangeStartState.value = millis }
    fun setRangeEnd(millis: Long) { rangeEndState.value = millis }

    /** The selected day's live drawer (Day mode). */
    val day: StateFlow<CashDrawerDay?> =
        combine(shopIdState.filterNotNull(), dateState) { shop, date -> shop to date }
            .flatMapLatest { (shop, date) ->
                repo.observeDay(shop, DateUtil.startOfDay(date), DateUtil.endOfDay(date))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Per-day history for Month / Custom modes (newest first). */
    val history: StateFlow<List<CashDrawerDay>> = combine(
        shopIdState.filterNotNull(), modeState, dateState, rangeStartState, rangeEndState,
    ) { shop, m, d, s, e ->
        val (start, end) = when (m) {
            DrawerMode.DAY -> DateUtil.startOfDay(d) to DateUtil.endOfDay(d)
            DrawerMode.MONTH -> DateUtil.startOfMonth(d) to DateUtil.endOfMonth(d)
            DrawerMode.CUSTOM ->
                DateUtil.startOfDay(minOf(s, e)) to DateUtil.endOfDay(maxOf(s, e))
        }
        Triple(shop, start, end)
    }.flatMapLatest { (shop, start, end) ->
        repo.observeHistory(shop, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The selected day's cash transactions with a running drawer balance (oldest first). */
    val dayTransactions: StateFlow<List<DrawerTxnRow>> = combine(
        combine(shopIdState.filterNotNull(), dateState) { shop, date -> shop to date }
            .flatMapLatest { (shop, date) ->
                repo.observeDayTransactions(shop, DateUtil.startOfDay(date), DateUtil.endOfDay(date))
            },
        day,
    ) { txns, d ->
        var running = d?.opening ?: 0L
        txns.map { t ->
            running += t.amount
            DrawerTxnRow(t, running)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
