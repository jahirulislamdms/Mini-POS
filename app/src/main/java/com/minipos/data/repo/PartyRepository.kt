package com.minipos.data.repo

import com.minipos.data.dao.PartyDao
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import kotlinx.coroutines.flow.Flow

/** Parties (Customer/Supplier/Employee) plus their dues & payments (BUILD_PLAN §6.8). */
class PartyRepository(private val dao: PartyDao) {

    fun observeParties(shopId: Long): Flow<List<Party>> = dao.observeParties(shopId)
    fun observePartiesByType(shopId: Long, type: PartyType): Flow<List<Party>> =
        dao.observePartiesByType(shopId, type)
    fun observeParty(id: Long): Flow<Party?> = dao.observeParty(id)
    suspend fun getParty(id: Long): Party? = dao.getParty(id)

    suspend fun addParty(party: Party): Long =
        dao.insertParty(party.copy(createdAt = System.currentTimeMillis()))
    suspend fun updateParty(party: Party) = dao.updateParty(party)
    suspend fun deleteParty(party: Party) = dao.deleteParty(party)

    // --- dues ---
    fun observeDues(shopId: Long): Flow<List<Due>> = dao.observeDues(shopId)
    fun observeDuesForParty(shopId: Long, partyId: Long): Flow<List<Due>> =
        dao.observeDuesForParty(shopId, partyId)
    fun observeDueTotal(shopId: Long, direction: DueDirection): Flow<Long> =
        dao.observeDueTotal(shopId, direction)
    fun observePartyDueTotal(shopId: Long, partyId: Long, direction: DueDirection): Flow<Long> =
        dao.observePartyDueTotal(shopId, partyId, direction)
    suspend fun insertDue(due: Due): Long = dao.insertDue(due)

    /** A manually-entered due (e.g. opening balance), not tied to a sale/purchase. */
    suspend fun addManualDue(shopId: Long, partyId: Long, amount: Long, direction: DueDirection): Long =
        dao.insertDue(
            Due(
                shopId = shopId,
                partyId = partyId,
                amount = amount,
                direction = direction,
                refType = "MANUAL",
                createdAt = System.currentTimeMillis(),
            ),
        )

    // --- payments ---
    fun observePayments(shopId: Long): Flow<List<DuePayment>> = dao.observePayments(shopId)
    fun observePaymentsForParty(shopId: Long, partyId: Long): Flow<List<DuePayment>> =
        dao.observePaymentsForParty(shopId, partyId)
    fun observePaymentTotal(shopId: Long, direction: PaymentDirection): Flow<Long> =
        dao.observePaymentTotal(shopId, direction)
    fun observePartyPaymentTotal(shopId: Long, partyId: Long, direction: PaymentDirection): Flow<Long> =
        dao.observePartyPaymentTotal(shopId, partyId, direction)
    fun observePaymentTotalBetween(shopId: Long, direction: PaymentDirection, start: Long, end: Long): Flow<Long> =
        dao.observePaymentTotalBetween(shopId, direction, start, end)
    suspend fun insertPayment(payment: DuePayment): Long = dao.insertPayment(payment)

    suspend fun recordPayment(shopId: Long, partyId: Long, amount: Long, direction: PaymentDirection): Long =
        dao.insertPayment(
            DuePayment(
                shopId = shopId,
                partyId = partyId,
                amount = amount,
                direction = direction,
                createdAt = System.currentTimeMillis(),
            ),
        )
}
