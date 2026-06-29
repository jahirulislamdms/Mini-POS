package com.minipos.data.repo

import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.Product
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

/** Products + their stock movements (BUILD_PLAN §6.4, §9). */
class ProductRepository(private val db: MiniPosDatabase) {

    private val productDao = db.productDao()
    private val movementDao = db.stockMovementDao()

    fun observeByShop(shopId: Long): Flow<List<Product>> = productDao.observeByShop(shopId)
    fun search(shopId: Long, query: String): Flow<List<Product>> = productDao.search(shopId, query)
    fun observeByCategory(shopId: Long, categoryId: Long): Flow<List<Product>> =
        productDao.observeByCategory(shopId, categoryId)
    fun observeById(id: Long): Flow<Product?> = productDao.observeById(id)
    suspend fun getById(id: Long): Product? = productDao.getById(id)

    fun observeCount(shopId: Long): Flow<Int> = productDao.observeCount(shopId)
    fun observeStockValue(shopId: Long): Flow<Long> = productDao.observeStockValue(shopId)
    fun observeLowStock(shopId: Long, shopDefault: Double): Flow<List<Product>> =
        productDao.observeLowStock(shopId, shopDefault)

    fun observeMovements(shopId: Long): Flow<List<StockMovement>> = movementDao.observeByShop(shopId)
    fun observeMovementsForProduct(shopId: Long, productId: Long): Flow<List<StockMovement>> =
        movementDao.observeByProduct(shopId, productId)

    /** Insert a product; logs an INITIAL stock movement if it starts with stock. */
    suspend fun add(product: Product): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val toInsert = product.copy(createdAt = now, updatedAt = now)
        val id = productDao.insert(toInsert)
        if (toInsert.stock != 0.0) {
            movementDao.insert(
                StockMovement(
                    shopId = toInsert.shopId,
                    productId = id,
                    change = toInsert.stock,
                    type = MovementType.INITIAL,
                    createdAt = now,
                ),
            )
        }
        id
    }

    suspend fun update(product: Product) =
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(product: Product) = productDao.delete(product)

    /** Apply a stock delta and record the movement, atomically. */
    suspend fun adjustStock(
        shopId: Long,
        productId: Long,
        delta: Double,
        type: MovementType,
        refId: Long? = null,
        note: String? = null,
    ) = db.withTransaction {
        val now = System.currentTimeMillis()
        productDao.adjustStock(productId, delta, now)
        movementDao.insert(
            StockMovement(
                shopId = shopId,
                productId = productId,
                change = delta,
                type = type,
                refId = refId,
                note = note,
                createdAt = now,
            ),
        )
    }
}
