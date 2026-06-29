package com.minipos.feature.due

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A party with its net balance (positive = you'll receive, negative = you'll give). */
data class PartyRow(val party: Party, val net: Long)

/** Shop-wide headline totals. */
data class DueHeadline(val youllReceive: Long, val youllGive: Long)

/**
 * Net balance for a party from the "you'll receive" perspective:
 * (RECEIVABLE dues − RECEIVED payments) − (PAYABLE dues − GIVEN payments).
 */
internal fun netOf(partyId: Long, dues: List<Due>, payments: List<DuePayment>): Long {
    val recvDue = dues.filter { it.partyId == partyId && it.direction == DueDirection.RECEIVABLE }.sumOf { it.amount }
    val payDue = dues.filter { it.partyId == partyId && it.direction == DueDirection.PAYABLE }.sumOf { it.amount }
    val received = payments.filter { it.partyId == partyId && it.direction == PaymentDirection.RECEIVED }.sumOf { it.amount }
    val given = payments.filter { it.partyId == partyId && it.direction == PaymentDirection.GIVEN }.sumOf { it.amount }
    return (recvDue - received) - (payDue - given)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DueLedgerViewModel : ViewModel() {

    private val repo = ServiceLocator.partyRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    private val typeState = MutableStateFlow(PartyType.CUSTOMER)

    val selectedType: StateFlow<PartyType> = typeState

    fun setShop(shopId: Long) { shopIdState.value = shopId }
    fun setType(type: PartyType) { typeState.value = type }

    private val parties = shopIdState.filterNotNull()
        .flatMapLatest { repo.observeParties(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dues = shopIdState.filterNotNull()
        .flatMapLatest { repo.observeDues(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val payments = shopIdState.filterNotNull()
        .flatMapLatest { repo.observePayments(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val headline: StateFlow<DueHeadline> = combine(dues, payments) { d, p ->
        val recvDue = d.filter { it.direction == DueDirection.RECEIVABLE }.sumOf { it.amount }
        val received = p.filter { it.direction == PaymentDirection.RECEIVED }.sumOf { it.amount }
        val payDue = d.filter { it.direction == DueDirection.PAYABLE }.sumOf { it.amount }
        val given = p.filter { it.direction == PaymentDirection.GIVEN }.sumOf { it.amount }
        DueHeadline(youllReceive = recvDue - received, youllGive = payDue - given)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueHeadline(0, 0))

    val partyRows: StateFlow<List<PartyRow>> =
        combine(parties, dues, payments, typeState) { parties, d, p, type ->
            parties.filter { it.type == type }
                .map { PartyRow(it, netOf(it.id, d, p)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addParty(name: String, phone: String?, address: String?, type: PartyType) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.addParty(
            Party(shopId = shopId, name = name, phone = phone, address = address, type = type, createdAt = 0),
        )
    }

    fun updateParty(party: Party) = viewModelScope.launch { repo.updateParty(party) }
    fun deleteParty(party: Party) = viewModelScope.launch { repo.deleteParty(party) }
}
