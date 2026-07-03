package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.minipos.data.entity.CashTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CashTransactionDao {

    @Insert suspend fun insert(txn: CashTransaction): Long
    @Insert suspend fun insertAll(txns: List<CashTransaction>)
    @Delete suspend fun delete(txn: CashTransaction)

    @Query("SELECT * FROM cash_transactions WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Long): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE shopId = :shopId AND createdAt >= :since ORDER BY createdAt DESC")
    fun observeByShopSince(shopId: Long, since: Long): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<CashTransaction>>

    /** All-time totals (Phase 17 petty-cash balance). */
    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM cash_transactions WHERE shopId = :shopId AND type = 'CASH_IN'")
    fun observeTotalIn(shopId: Long): Flow<Long>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM cash_transactions WHERE shopId = :shopId AND type = 'CASH_OUT'")
    fun observeTotalOut(shopId: Long): Flow<Long>

    @Query("SELECT * FROM cash_transactions WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<CashTransaction>

    @Query("DELETE FROM cash_transactions WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)

    /** Undo (Phase 15). */
    @Query("DELETE FROM cash_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
