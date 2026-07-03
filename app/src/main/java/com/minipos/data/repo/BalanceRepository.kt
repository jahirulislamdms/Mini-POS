package com.minipos.data.repo

import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.PaymentDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Current Balance = the shop's **petty cash** (Phase 17). It grows from sales cash (paid now +
 * customer due payments received) and manual Cash In; it shrinks only from Expenses and manual
 * Cash Out. **Buying never reduces it** (cash buys and supplier due payments are excluded).
 *
 * Single source of truth used by the Home dashboard AND the expense / cash-out guards, so the
 * displayed balance and the "never negative" checks always agree.
 */
class BalanceRepository(db: MiniPosDatabase) {

    private val saleDao = db.saleDao()
    private val expenseDao = db.expenseDao()
    private val cashDao = db.cashTransactionDao()
    private val partyDao = db.partyDao()

    fun observeBalance(shopId: Long): Flow<Long> = combine(
        saleDao.observeTotalPaid(shopId),
        partyDao.observePaymentTotal(shopId, PaymentDirection.RECEIVED),
        cashDao.observeTotalIn(shopId),
        cashDao.observeTotalOut(shopId),
        expenseDao.observeTotal(shopId),
    ) { salePaid, received, cashIn, cashOut, expenses ->
        salePaid + received + cashIn - cashOut - expenses
    }

    /** A one-shot snapshot of the current balance, for validating a new expense / cash-out. */
    suspend fun getBalance(shopId: Long): Long = observeBalance(shopId).first()
}
