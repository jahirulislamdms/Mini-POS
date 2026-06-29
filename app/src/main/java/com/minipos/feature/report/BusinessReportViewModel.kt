package com.minipos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateFilter
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.PaymentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Business report figures for a period (all paisa). */
data class BusinessReport(
    val cashSale: Long = 0,
    val dueCollected: Long = 0,
    val otherIncome: Long = 0,
    val cashPurchase: Long = 0,
    val duePaid: Long = 0,
    val otherExpense: Long = 0,
    val netBalance: Long = 0,
    val profitTotal: Long = 0,
    val profitCash: Long = 0,
    val profitDue: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessReportViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val expenseRepo = ServiceLocator.expenseRepository
    private val partyRepo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val filterState = MutableStateFlow(DateFilter.MONTH)
    private val customStartState = MutableStateFlow<Long?>(null)
    private val customEndState = MutableStateFlow<Long?>(null)

    val filter: StateFlow<DateFilter> = filterState
    val customStart: StateFlow<Long?> = customStartState
    val customEnd: StateFlow<Long?> = customEndState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setFilter(f: DateFilter) { filterState.value = f }
    fun setCustomStart(millis: Long) { customStartState.value = millis }
    fun setCustomEnd(millis: Long) { customEndState.value = millis }

    val report: StateFlow<BusinessReport> =
        combine(shopIdState.filterNotNull(), filterState, customStartState, customEndState) { shop, f, cs, ce ->
            val (start, end) = DateUtil.rangeFor(f, customStart = cs, customEnd = ce)
            Triple(shop, start, end)
        }.flatMapLatest { (shop, start, end) ->
            combine(
                saleRepo.observeCashInBetween(shop, start, end),
                partyRepo.observePaymentTotalBetween(shop, PaymentDirection.RECEIVED, start, end),
                purchaseRepo.observeCashOutBetween(shop, start, end),
                partyRepo.observePaymentTotalBetween(shop, PaymentDirection.GIVEN, start, end),
                expenseRepo.observeTotalBetween(shop, start, end),
                saleRepo.observeProfitBetween(shop, start, end),
                saleRepo.observeProfitByPaymentBetween(shop, PaymentType.CASH, start, end),
            ) { v ->
                val cashSale = v[0]
                val dueCollected = v[1]
                val cashPurchase = v[2]
                val duePaid = v[3]
                val otherExpense = v[4]
                val profitTotal = v[5]
                val profitCash = v[6]
                BusinessReport(
                    cashSale = cashSale,
                    dueCollected = dueCollected,
                    otherIncome = 0,
                    cashPurchase = cashPurchase,
                    duePaid = duePaid,
                    otherExpense = otherExpense,
                    netBalance = (cashSale + dueCollected) - (cashPurchase + duePaid + otherExpense),
                    profitTotal = profitTotal,
                    profitCash = profitCash,
                    profitDue = profitTotal - profitCash,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessReport())
}
