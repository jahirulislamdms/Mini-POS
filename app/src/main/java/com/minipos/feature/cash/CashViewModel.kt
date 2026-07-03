package com.minipos.feature.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.Money
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.CashType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CashTotals(val cashIn: Long, val cashOut: Long, val net: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class CashViewModel : ViewModel() {

    private val repo = ServiceLocator.cashRepository
    private val balanceRepo = ServiceLocator.balanceRepository
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val transactions: StateFlow<List<CashTransaction>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<CashTotals> = transactions
        .map { list ->
            val cashIn = list.filter { it.type == CashType.CASH_IN }.sumOf { it.amount }
            val cashOut = list.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount }
            CashTotals(cashIn, cashOut, cashIn - cashOut)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashTotals(0, 0, 0))

    /**
     * Add a Cash In / Cash Out. Cash Out can never exceed the Current Balance (Phase 17: balance
     * never goes negative). [onResult] reports success or a message so the dialog can stay open.
     */
    fun add(amount: Long, type: CashType, note: String?, onResult: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            val shopId = shopIdState.value ?: return@launch onResult(false, "No shop selected.")
            if (type == CashType.CASH_OUT) {
                val balance = balanceRepo.getBalance(shopId)
                if (amount > balance) {
                    onResult(false, "Not enough balance. Current balance is ${Money.format(balance)}.")
                    return@launch
                }
            }
            repo.add(shopId, amount, type, note)
            onResult(true, null)
        }

    fun delete(txn: CashTransaction) = viewModelScope.launch { repo.delete(txn) }
}
