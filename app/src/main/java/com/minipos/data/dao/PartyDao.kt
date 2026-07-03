package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.Party
import com.minipos.data.entity.PartyType
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {

    // --- Party ---
    @Insert suspend fun insertParty(party: Party): Long
    @Update suspend fun updateParty(party: Party)
    @Delete suspend fun deleteParty(party: Party)

    @Query("SELECT * FROM parties WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun observeParties(shopId: Long): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE shopId = :shopId AND type = :type ORDER BY name COLLATE NOCASE ASC")
    fun observePartiesByType(shopId: Long, type: PartyType): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getParty(id: Long): Party?

    @Query("SELECT * FROM parties WHERE id = :id")
    fun observeParty(id: Long): Flow<Party?>

    @Query("SELECT * FROM parties WHERE shopId = :shopId")
    suspend fun getPartiesForShop(shopId: Long): List<Party>

    @Query("DELETE FROM parties WHERE shopId = :shopId")
    suspend fun deletePartiesForShop(shopId: Long)

    // --- Due ---
    @Insert suspend fun insertDue(due: Due): Long

    @Query("SELECT * FROM dues WHERE shopId = :shopId AND partyId = :partyId ORDER BY createdAt DESC")
    fun observeDuesForParty(shopId: Long, partyId: Long): Flow<List<Due>>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM dues WHERE shopId = :shopId AND direction = :direction")
    fun observeDueTotal(shopId: Long, direction: DueDirection): Flow<Long>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM dues WHERE shopId = :shopId AND partyId = :partyId AND direction = :direction")
    fun observePartyDueTotal(shopId: Long, partyId: Long, direction: DueDirection): Flow<Long>

    @Query("SELECT * FROM dues WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeDues(shopId: Long): Flow<List<Due>>

    @Query("SELECT * FROM dues WHERE shopId = :shopId")
    suspend fun getDuesForShop(shopId: Long): List<Due>

    @Query("DELETE FROM dues WHERE shopId = :shopId")
    suspend fun deleteDuesForShop(shopId: Long)

    // --- Undo (Phase 15): remove the due (and its payments) created by a sale/purchase ---
    @Query("SELECT * FROM dues WHERE shopId = :shopId AND refType = :refType AND refId = :refId")
    suspend fun getDuesByRef(shopId: Long, refType: String, refId: Long): List<Due>

    @Query("DELETE FROM dues WHERE id = :id")
    suspend fun deleteDueById(id: Long)

    @Query("DELETE FROM due_payments WHERE dueId = :dueId")
    suspend fun deletePaymentsByDueId(dueId: Long)

    // --- DuePayment ---
    @Insert suspend fun insertPayment(payment: DuePayment): Long

    @Query("SELECT * FROM due_payments WHERE shopId = :shopId AND partyId = :partyId ORDER BY createdAt DESC")
    fun observePaymentsForParty(shopId: Long, partyId: Long): Flow<List<DuePayment>>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM due_payments WHERE shopId = :shopId AND direction = :direction")
    fun observePaymentTotal(shopId: Long, direction: PaymentDirection): Flow<Long>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM due_payments WHERE shopId = :shopId AND partyId = :partyId AND direction = :direction")
    fun observePartyPaymentTotal(shopId: Long, partyId: Long, direction: PaymentDirection): Flow<Long>

    @Query("SELECT * FROM due_payments WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observePayments(shopId: Long): Flow<List<DuePayment>>

    @Query("SELECT * FROM due_payments WHERE shopId = :shopId")
    suspend fun getPaymentsForShop(shopId: Long): List<DuePayment>

    @Query(
        "SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM due_payments " +
            "WHERE shopId = :shopId AND direction = :direction AND createdAt BETWEEN :start AND :end",
    )
    fun observePaymentTotalBetween(shopId: Long, direction: PaymentDirection, start: Long, end: Long): Flow<Long>

    @Query("DELETE FROM due_payments WHERE shopId = :shopId")
    suspend fun deletePaymentsForShop(shopId: Long)
}
