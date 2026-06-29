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

    @Query("SELECT * FROM cash_transactions WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<CashTransaction>

    @Query("DELETE FROM cash_transactions WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
