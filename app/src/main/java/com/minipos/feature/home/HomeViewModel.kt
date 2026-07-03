package com.minipos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.CashType
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.Expense
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.Sale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class HomePeriod { DAY, MONTH }

/** Headline numbers for the Home dashboard (money in paisa). */
data class HomeStats(
    val balance: Long = 0,
    val periodSale: Long = 0,
    val periodExpense: Long = 0,
    val duesReceive: Long = 0,
    val duesGive: Long = 0,
    val productCount: Int = 0,
    val totalUnits: Double = 0.0,
    val stockValue: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val expenseRepo = ServiceLocator.expenseRepository
    private val partyRepo = ServiceLocator.partyRepository
    private val productRepo = ServiceLocator.productRepository
    private val cashRepo = ServiceLocator.cashRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val periodState = MutableStateFlow(HomePeriod.DAY)

    val period: StateFlow<HomePeriod> = periodState
    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setPeriod(p: HomePeriod) { periodState.value = p }

    private val sales = shopIdState.filterNotNull().flatMapLatest { saleRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val purchases = shopIdState.filterNotNull().flatMapLatest { purchaseRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val expenses = shopIdState.filterNotNull().flatMapLatest { expenseRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val dues = shopIdState.filterNotNull().flatMapLatest { partyRepo.observeDues(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val payments = shopIdState.filterNotNull().flatMapLatest { partyRepo.observePayments(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val productCount = shopIdState.filterNotNull().flatMapLatest { productRepo.observeCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    // Phase 11: total units in stock — same Σ-stock calculation as the Products page header.
    private val totalUnits = shopIdState.filterNotNull().flatMapLatest { productRepo.observeByShop(it) }
        .map { list -> list.sumOf { it.stock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    // Phase 16: total stock value — same calculation as the Products page (Σ stock × buyPrice).
    private val stockValue = shopIdState.filterNotNull().flatMapLatest { productRepo.observeStockValue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    private val cashTransactions = shopIdState.filterNotNull().flatMapLatest { cashRepo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<HomeStats> = combine(
        combine(sales, purchases, expenses) { s, p, e -> Triple(s, p, e) },
        combine(dues, payments) { d, pay -> d to pay },
        combine(productCount, totalUnits, stockValue) { count, units, value -> Triple(count, units, value) },
        cashTransactions,
        periodState,
    ) { a, duesPay, product, cash, per ->
        computeStats(
            sales = a.first, purchases = a.second, expenses = a.third,
            dues = duesPay.first, payments = duesPay.second,
            productCount = product.first, totalUnits = product.second, stockValue = product.third,
            cashTransactions = cash, period = per,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())

    private fun computeStats(
        sales: List<Sale>,
        purchases: List<Purchase>,
        expenses: List<Expense>,
        dues: List<com.minipos.data.entity.Due>,
        payments: List<com.minipos.data.entity.DuePayment>,
        productCount: Int,
        totalUnits: Double,
        stockValue: Long,
        cashTransactions: List<CashTransaction>,
        period: HomePeriod,
    ): HomeStats {
        val now = System.currentTimeMillis()
        val (start, end) = if (period == HomePeriod.DAY) {
            DateUtil.startOfDay(now) to DateUtil.endOfDay(now)
        } else {
            DateUtil.startOfMonth(now) to DateUtil.endOfMonth(now)
        }

        val received = payments.filter { it.direction == PaymentDirection.RECEIVED }.sumOf { it.amount }
        val given = payments.filter { it.direction == PaymentDirection.GIVEN }.sumOf { it.amount }
        val cashIn = cashTransactions.filter { it.type == CashType.CASH_IN }.sumOf { it.amount }
        val cashOut = cashTransactions.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount }
        // Phase 17: Current Balance = petty cash. Buying (cash buys + supplier due payments) no
        // longer reduces it. Must stay in sync with BalanceRepository.observeBalance.
        val balance = (sales.sumOf { it.paidAmount } + received + cashIn) -
            (expenses.sumOf { it.amount } + cashOut)

        return HomeStats(
            balance = balance,
            periodSale = sales.filter { it.createdAt in start..end }.sumOf { it.total },
            periodExpense = expenses.filter { it.createdAt in start..end }.sumOf { it.amount },
            duesReceive = dues.filter { it.direction == DueDirection.RECEIVABLE }.sumOf { it.amount } - received,
            duesGive = dues.filter { it.direction == DueDirection.PAYABLE }.sumOf { it.amount } - given,
            productCount = productCount,
            totalUnits = totalUnits,
            stockValue = stockValue,
        )
    }
}
