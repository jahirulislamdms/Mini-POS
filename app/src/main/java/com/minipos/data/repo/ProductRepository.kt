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

    /** Product History (Phase 13): movements within a window (newest first). */
    fun observeMovementsForProductSince(shopId: Long, productId: Long, since: Long): Flow<List<StockMovement>> =
        movementDao.observeByProductSince(shopId, productId, since)

    companion object {
        /** Product History window — the screen shows only the last 30 days (older rows are kept). */
        const val MOVEMENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }

    // --- Barcodes (Phase 28) ---

    suspend fun getByBarcode(shopId: Long, barcode: String): Product? =
        productDao.getByBarcode(shopId, barcode)

    /** True when another product of the shop already uses [barcode]. */
    suspend fun isBarcodeTaken(shopId: Long, barcode: String, excludeProductId: Long): Boolean =
        productDao.getByBarcode(shopId, barcode)?.let { it.id != excludeProductId } == true

    /** A new numeric CODE-128 barcode, guaranteed unique within the shop. */
    suspend fun generateBarcode(shopId: Long): String {
        while (true) {
            val code = buildString {
                append("2")                                            // internal-code prefix
                append((System.currentTimeMillis() / 1000) % 1_000_000_000) // 9 digits
                append((100..999).random())                            // 3 random digits
            }
            if (productDao.getByBarcode(shopId, code) == null) return code
        }
    }

    /** [requested] trimmed, or a freshly generated unique barcode when blank. */
    suspend fun ensureBarcode(shopId: Long, requested: String?): String =
        requested?.trim().takeUnless { it.isNullOrEmpty() } ?: generateBarcode(shopId)

    /** Backfill: give every product without a barcode (old installs / restored backups) one. */
    suspend fun backfillMissingBarcodes() {
        productDao.getAllWithoutBarcode().forEach { p ->
            productDao.setBarcode(p.id, generateBarcode(p.shopId))
        }
    }

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
