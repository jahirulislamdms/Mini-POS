package com.minipos.feature.due

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.Party
import com.minipos.data.entity.PaymentDirection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One line in a party's statement; [signedAmount]/[runningBalance] use the you'll-receive perspective. */
data class StatementEntry(
    val createdAt: Long,
    val label: String,
    val signedAmount: Long,
    val runningBalance: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PartyDetailViewModel : ViewModel() {

    private val repo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val partyIdState = MutableStateFlow<Long?>(null)

    fun set(shopId: Long, partyId: Long) {
        shopIdState.value = shopId
        partyIdState.value = partyId
    }

    val party: StateFlow<Party?> = partyIdState.filterNotNull()
        .flatMapLatest { repo.observeParty(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val key = combine(shopIdState.filterNotNull(), partyIdState.filterNotNull()) { s, p -> s to p }

    private val dues = key.flatMapLatest { (s, p) -> repo.observeDuesForParty(s, p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val payments = key.flatMapLatest { (s, p) -> repo.observePaymentsForParty(s, p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entries: StateFlow<List<StatementEntry>> = combine(dues, payments) { d, p ->
        buildStatement(d, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current net (positive = you'll receive, negative = you'll give). */
    val net: StateFlow<Long> = combine(dues, payments) { d, p ->
        d.sumOf { if (it.direction == DueDirection.RECEIVABLE) it.amount else -it.amount } +
            p.sumOf { if (it.direction == PaymentDirection.GIVEN) it.amount else -it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun recordPayment(direction: PaymentDirection, amount: Long) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val partyId = partyIdState.value ?: return@launch
        repo.recordPayment(shopId, partyId, amount, direction)
    }

    fun addDue(direction: DueDirection, amount: Long) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val partyId = partyIdState.value ?: return@launch
        repo.addManualDue(shopId, partyId, amount, direction)
    }

    fun updateParty(party: Party) = viewModelScope.launch { repo.updateParty(party) }
    fun deleteParty(party: Party) = viewModelScope.launch { repo.deleteParty(party) }

    private fun buildStatement(dues: List<Due>, payments: List<DuePayment>): List<StatementEntry> {
        data class Raw(val createdAt: Long, val label: String, val signed: Long)

        val raw = buildList {
            dues.forEach { due ->
                if (due.direction == DueDirection.RECEIVABLE) {
                    add(Raw(due.createdAt, labelForDue(due.refType, receivable = true), due.amount))
                } else {
                    add(Raw(due.createdAt, labelForDue(due.refType, receivable = false), -due.amount))
                }
            }
            payments.forEach { pay ->
                if (pay.direction == PaymentDirection.RECEIVED) {
                    add(Raw(pay.createdAt, "Payment received", -pay.amount))
                } else {
                    add(Raw(pay.createdAt, "Payment given", pay.amount))
                }
            }
        }.sortedBy { it.createdAt }

        var running = 0L
        val withRunning = raw.map {
            running += it.signed
            StatementEntry(it.createdAt, it.label, it.signed, running)
        }
        return withRunning.reversed()
    }

    private fun labelForDue(refType: String?, receivable: Boolean): String = when (refType) {
        "SALE" -> "Sale (due)"
        "PURCHASE" -> "Purchase (due)"
        else -> if (receivable) "Due added (you'll get)" else "Due added (you'll give)"
    }
}
