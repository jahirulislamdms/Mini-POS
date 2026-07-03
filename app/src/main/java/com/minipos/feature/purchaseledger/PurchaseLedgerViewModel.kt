package com.minipos.feature.purchaseledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateFilter
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.core.util.SearchUtil
import com.minipos.data.entity.Purchase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PurchaseRow(val purchase: Purchase, val partyName: String?)

data class PurchaseTotals(val total: Long, val cashOut: Long, val due: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseLedgerViewModel : ViewModel() {

    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val partyRepo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val filterState = MutableStateFlow(DateFilter.DAY)
    private val customStartState = MutableStateFlow<Long?>(null)
    private val customEndState = MutableStateFlow<Long?>(null)
    private val queryState = MutableStateFlow("")

    val filter: StateFlow<DateFilter> = filterState
    val query: StateFlow<String> = queryState
    val customStart: StateFlow<Long?> = customStartState
    val customEnd: StateFlow<Long?> = customEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setFilter(f: DateFilter) { filterState.value = f }
    fun setCustomStart(millis: Long) { customStartState.value = millis }
    fun setCustomEnd(millis: Long) { customEndState.value = millis }
    fun setQuery(q: String) { queryState.value = q }

    private val purchasesInRange: StateFlow<List<Purchase>> =
        combine(shopIdState.filterNotNull(), filterState, customStartState, customEndState) { shop, f, cs, ce ->
            Triple(shop, f, cs to ce)
        }.flatMapLatest { (shop, f, custom) ->
            val (start, end) = DateUtil.rangeFor(f, customStart = custom.first, customEnd = custom.second)
            purchaseRepo.observeBetween(shop, start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val parties = shopIdState.filterNotNull()
        .flatMapLatest { partyRepo.observeParties(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rows: StateFlow<List<PurchaseRow>> =
        combine(purchasesInRange, parties, queryState) { purchases, parties, q ->
            val names = parties.associateBy({ it.id }, { it.name })
            // Smart search (Phase 22): case/space-insensitive partial match on note/party/amount.
            SearchUtil.filter(purchases.map { PurchaseRow(it, it.partyId?.let(names::get)) }, q) { row ->
                listOf(row.purchase.note, row.partyName, Money.format(row.purchase.total))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<PurchaseTotals> = purchasesInRange
        .map { list ->
            PurchaseTotals(
                total = list.sumOf { it.total },
                cashOut = list.sumOf { it.paidAmount },
                due = list.sumOf { it.dueAmount },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseTotals(0, 0, 0))
}
