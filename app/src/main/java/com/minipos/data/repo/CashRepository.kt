package com.minipos.data.repo

import com.minipos.data.dao.CashTransactionDao
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.CashType
import kotlinx.coroutines.flow.Flow

/** Manual cash adjustments (Cash In / Cash Out). Affects Current Balance only. */
class CashRepository(private val dao: CashTransactionDao) {

    fun observeByShop(shopId: Long): Flow<List<CashTransaction>> = dao.observeByShop(shopId)
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<CashTransaction>> =
        dao.observeBetween(shopId, start, end)

    suspend fun add(shopId: Long, amount: Long, type: CashType, note: String?): Long =
        dao.insert(
            CashTransaction(
                shopId = shopId,
                amount = amount,
                type = type,
                note = note,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun delete(txn: CashTransaction) = dao.delete(txn)
}
