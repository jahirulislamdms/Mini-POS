package com.minipos.data.repo

import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.Due
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Sale
import com.minipos.data.entity.SaleItem
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

/** One line to commit in a sale (already priced; lineTotal in paisa). */
data class SaleLineInput(
    val productId: Long?,   // null for a quick/custom line
    val name: String,
    val unitPrice: Long,
    val quantity: Double,
    val discount: Long,
    val lineTotal: Long,
)

/** Sales: reads + the transactional commit (Sale+items, stock decrement, Due). */
class SaleRepository(private val db: MiniPosDatabase) {

    private val saleDao = db.saleDao()
    private val productDao = db.productDao()
    private val movementDao = db.stockMovementDao()
    private val partyDao = db.partyDao()

    fun observeByShop(shopId: Long): Flow<List<Sale>> = saleDao.observeByShop(shopId)
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Sale>> =
        saleDao.observeBetween(shopId, start, end)
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        saleDao.observeTotalBetween(shopId, start, end)
    fun observeCashInBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        saleDao.observeCashInBetween(shopId, start, end)
    fun observeProfitBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        saleDao.observeProfitBetween(shopId, start, end)
    fun observeProfitByPaymentBetween(shopId: Long, paymentType: PaymentType, start: Long, end: Long): Flow<Long> =
        saleDao.observeProfitBetweenByPayment(shopId, paymentType, start, end)

    suspend fun getById(id: Long): Sale? = saleDao.getById(id)
    fun observeItems(saleId: Long): Flow<List<SaleItem>> = saleDao.observeItems(saleId)
    suspend fun getItems(saleId: Long): List<SaleItem> = saleDao.getItems(saleId)
    suspend fun getItemsForShop(shopId: Long): List<SaleItem> = saleDao.getSaleItemsForShop(shopId)

    /**
     * Records a sale atomically: inserts the Sale + its items, decrements stock and logs a
     * StockMovement per product line, and (for DUE) creates a RECEIVABLE Due for the unpaid part.
     * Returns the new saleId.
     */
    suspend fun commitSale(
        shopId: Long,
        lines: List<SaleLineInput>,
        orderDiscount: Long,
        paymentType: PaymentType,
        partyId: Long?,
        paidAmount: Long,
        isQuickSale: Boolean,
        note: String?,
    ): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val subtotal = lines.sumOf { it.lineTotal }
        val total = (subtotal - orderDiscount).coerceAtLeast(0)
        val paid = if (paymentType == PaymentType.CASH) total else paidAmount.coerceIn(0, total)
        val due = total - paid

        val saleId = saleDao.insertSale(
            Sale(
                shopId = shopId,
                subtotal = subtotal,
                discount = orderDiscount,
                total = total,
                paymentType = paymentType,
                partyId = if (paymentType == PaymentType.DUE) partyId else null,
                paidAmount = paid,
                dueAmount = due,
                isQuickSale = isQuickSale,
                note = note,
                createdAt = now,
            ),
        )

        if (lines.isNotEmpty()) {
            saleDao.insertItems(
                lines.map { line ->
                    SaleItem(
                        shopId = shopId,
                        saleId = saleId,
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
                productDao.adjustStock(pid, -line.quantity, now)
                movementDao.insert(
                    StockMovement(
                        shopId = shopId,
                        productId = pid,
                        change = -line.quantity,
                        type = MovementType.SALE,
                        refId = saleId,
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
                    direction = DueDirection.RECEIVABLE,
                    refType = "SALE",
                    refId = saleId,
                    createdAt = now,
                ),
            )
        }

        saleId
    }
}
