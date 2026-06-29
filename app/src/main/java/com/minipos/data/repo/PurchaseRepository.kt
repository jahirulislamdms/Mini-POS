package com.minipos.data.repo

import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.PurchaseItem
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

/** One line to commit in a purchase (already priced; lineTotal in paisa). */
data class PurchaseLineInput(
    val productId: Long?,
    val name: String,
    val unitPrice: Long,
    val quantity: Double,
    val discount: Long,
    val lineTotal: Long,
)

/** Purchases: reads + the transactional commit (Purchase+items, stock increment, Due). */
class PurchaseRepository(private val db: MiniPosDatabase) {

    private val purchaseDao = db.purchaseDao()
    private val productDao = db.productDao()
    private val movementDao = db.stockMovementDao()
    private val partyDao = db.partyDao()

    fun observeByShop(shopId: Long): Flow<List<Purchase>> = purchaseDao.observeByShop(shopId)
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Purchase>> =
        purchaseDao.observeBetween(shopId, start, end)
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        purchaseDao.observeTotalBetween(shopId, start, end)
    fun observeCashOutBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        purchaseDao.observeCashOutBetween(shopId, start, end)

    suspend fun getById(id: Long): Purchase? = purchaseDao.getById(id)
    fun observeItems(purchaseId: Long): Flow<List<PurchaseItem>> = purchaseDao.observeItems(purchaseId)
    suspend fun getItems(purchaseId: Long): List<PurchaseItem> = purchaseDao.getItems(purchaseId)
    suspend fun getItemsForShop(shopId: Long): List<PurchaseItem> = purchaseDao.getPurchaseItemsForShop(shopId)

    /**
     * Records a purchase atomically: inserts Purchase + items, increments stock and logs a
     * StockMovement per product line, and (for DUE) creates a PAYABLE Due for the unpaid part.
     */
    suspend fun commitPurchase(
        shopId: Long,
        lines: List<PurchaseLineInput>,
        orderDiscount: Long,
        paymentType: PaymentType,
        partyId: Long?,
        paidAmount: Long,
        note: String?,
    ): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val subtotal = lines.sumOf { it.lineTotal }
        val total = (subtotal - orderDiscount).coerceAtLeast(0)
        val paid = if (paymentType == PaymentType.CASH) total else paidAmount.coerceIn(0, total)
        val due = total - paid

        val purchaseId = purchaseDao.insertPurchase(
            Purchase(
                shopId = shopId,
                subtotal = subtotal,
                discount = orderDiscount,
                total = total,
                paymentType = paymentType,
                partyId = if (paymentType == PaymentType.DUE) partyId else null,
                paidAmount = paid,
                dueAmount = due,
                note = note,
                createdAt = now,
            ),
        )

        if (lines.isNotEmpty()) {
            purchaseDao.insertItems(
                lines.map { line ->
                    PurchaseItem(
                        shopId = shopId,
                        purchaseId = purchaseId,
                        productId = line.productId,
                        name = line.name,
                        unitPrice = line.unitPrice,
                        quantity = line.quantity,
                        discount = line.discount,
                        lineTotal = line.lineTotal,
                    )
                },
            )
            lines.forEach { line ->
                val pid = line.productId ?: return@forEach
                productDao.adjustStock(pid, line.quantity, now)
                movementDao.insert(
                    StockMovement(
                        shopId = shopId,
                        productId = pid,
                        change = line.quantity,
                        type = MovementType.PURCHASE,
                        refId = purchaseId,
                        createdAt = now,
                    ),
                )
            }
        }

        if (paymentType == PaymentType.DUE && partyId != null && due > 0) {
            partyDao.insertDue(
                Due(
                    shopId = shopId,
                    partyId = partyId,
                    amount = due,
                    direction = DueDirection.PAYABLE,
                    refType = "PURCHASE",
                    refId = purchaseId,
                    createdAt = now,
                ),
            )
        }

        purchaseId
    }
}
