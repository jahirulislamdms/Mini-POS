package com.minipos.data.repo

import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.ActivityUndo
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.Expense
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.Sale
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

/**
 * Activities & Undo (Phase 15). Aggregation reads come from the existing tables; each undo
 * **reverses and removes** the original transaction in one DB transaction (so balances, reports
 * and ledgers all re-aggregate correctly without touching their code) and records an
 * [ActivityUndo] audit row. Existing transaction creation logic is untouched.
 */
class ActivityRepository(private val db: MiniPosDatabase) {

    private val saleDao = db.saleDao()
    private val purchaseDao = db.purchaseDao()
    private val expenseDao = db.expenseDao()
    private val cashDao = db.cashTransactionDao()
    private val movementDao = db.stockMovementDao()
    private val productDao = db.productDao()
    private val partyDao = db.partyDao()
    private val undoDao = db.activityUndoDao()

    // --- Reads (last-N-days windows) ---
    fun observeSalesSince(shopId: Long, since: Long): Flow<List<Sale>> =
        saleDao.observeBetween(shopId, since, Long.MAX_VALUE)
    fun observePurchasesSince(shopId: Long, since: Long): Flow<List<Purchase>> =
        purchaseDao.observeBetween(shopId, since, Long.MAX_VALUE)
    fun observeExpensesSince(shopId: Long, since: Long): Flow<List<Expense>> =
        expenseDao.observeBetween(shopId, since, Long.MAX_VALUE)
    fun observeCashSince(shopId: Long, since: Long): Flow<List<CashTransaction>> =
        cashDao.observeByShopSince(shopId, since)
    fun observeAdjustmentsSince(shopId: Long, since: Long): Flow<List<StockMovement>> =
        movementDao.observeAdjustmentsSince(shopId, since)
    fun observeUndosSince(shopId: Long, since: Long): Flow<List<ActivityUndo>> =
        undoDao.observeSince(shopId, since)

    // --- Undo (reverse + remove + audit) ---

    /** Undo a sale: re-add stock, drop its due/payments + movements + items, delete the sale. */
    suspend fun undoSale(shopId: Long, saleId: Long, description: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        saleDao.getItems(saleId).forEach { item ->
            item.productId?.let { pid -> productDao.adjustStock(pid, item.quantity, now) }
        }
        movementDao.deleteSaleMovements(saleId)
        partyDao.getDuesByRef(shopId, "SALE", saleId).forEach { due ->
            partyDao.deletePaymentsByDueId(due.id)
            partyDao.deleteDueById(due.id)
        }
        saleDao.deleteItemsBySaleId(saleId)
        saleDao.deleteSaleById(saleId)
        recordUndo(shopId, "SELL", saleId, description, now)
    }

    /** Undo a purchase: subtract the added stock, drop its due/payments + movements + items, delete it. */
    suspend fun undoPurchase(shopId: Long, purchaseId: Long, description: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        purchaseDao.getItems(purchaseId).forEach { item ->
            item.productId?.let { pid -> productDao.adjustStock(pid, -item.quantity, now) }
        }
        movementDao.deletePurchaseMovements(purchaseId)
        partyDao.getDuesByRef(shopId, "PURCHASE", purchaseId).forEach { due ->
            partyDao.deletePaymentsByDueId(due.id)
            partyDao.deleteDueById(due.id)
        }
        purchaseDao.deleteItemsByPurchaseId(purchaseId)
        purchaseDao.deletePurchaseById(purchaseId)
        recordUndo(shopId, "BUY", purchaseId, description, now)
    }

    suspend fun undoExpense(shopId: Long, expenseId: Long, description: String) = db.withTransaction {
        expenseDao.deleteById(expenseId)
        recordUndo(shopId, "EXPENSE", expenseId, description, System.currentTimeMillis())
    }

    suspend fun undoCash(shopId: Long, cashId: Long, targetType: String, description: String) = db.withTransaction {
        cashDao.deleteById(cashId)
        recordUndo(shopId, targetType, cashId, description, System.currentTimeMillis())
    }

    /** Undo a manual stock adjustment: apply the opposite delta and remove the movement. */
    suspend fun undoStockAdjustment(shopId: Long, movementId: Long, description: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        movementDao.getById(movementId)?.let { m ->
            productDao.adjustStock(m.productId, -m.change, now)
            movementDao.deleteById(movementId)
        }
        recordUndo(shopId, "STOCK_ADJUSTMENT", movementId, description, now)
    }

    private suspend fun recordUndo(shopId: Long, targetType: String, refId: Long, description: String, at: Long) {
        undoDao.insert(
            ActivityUndo(
                shopId = shopId,
                targetType = targetType,
                targetRefId = refId,
                description = description,
                createdAt = at,
            ),
        )
    }
}
