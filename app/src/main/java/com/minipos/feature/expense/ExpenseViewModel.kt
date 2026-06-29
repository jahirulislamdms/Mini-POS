package com.minipos.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.DateFilter
import com.minipos.core.util.DateUtil
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An expense plus its resolved category name. */
data class ExpenseRow(val expense: Expense, val categoryName: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel : ViewModel() {

    private val repo = ServiceLocator.expenseRepository

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

    val categories: StateFlow<List<ExpenseCategory>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val expensesInRange: StateFlow<List<Expense>> =
        combine(shopIdState.filterNotNull(), filterState, customStartState, customEndState) { shop, f, cs, ce ->
            Triple(shop, f, cs to ce)
        }.flatMapLatest { (shop, f, custom) ->
            val (start, end) = DateUtil.rangeFor(f, customStart = custom.first, customEnd = custom.second)
            repo.observeBetween(shop, start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rows: StateFlow<List<ExpenseRow>> =
        combine(expensesInRange, categories) { expenses, cats ->
            val names = cats.associateBy({ it.id }, { it.name })
            expenses.map { ExpenseRow(it, it.categoryId?.let(names::get)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val total: StateFlow<Long> = expensesInRange
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun add(amount: Long, categoryId: Long?, note: String?, date: Long) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.add(shopId, categoryId, amount, note, date)
    }

    fun update(expense: Expense) = viewModelScope.launch { repo.update(expense) }
    fun delete(expense: Expense) = viewModelScope.launch { repo.delete(expense) }
}
