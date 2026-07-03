package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.minipos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert suspend fun insert(movement: StockMovement): Long

    @Query("SELECT * FROM stock_movements WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE shopId = :shopId AND productId = :productId ORDER BY createdAt DESC")
    fun observeByProduct(shopId: Long, productId: Long): Flow<List<StockMovement>>

    /** Product history within a time window (newest first) — used by the Product History screen. */
    @Query(
        "SELECT * FROM stock_movements WHERE shopId = :shopId AND productId = :productId " +
            "AND createdAt >= :since ORDER BY createdAt DESC",
    )
    fun observeByProductSince(shopId: Long, productId: Long, since: Long): Flow<List<StockMovement>>

    /** Stock-adjustment activities within a window (Phase 15 Activities list). */
    @Query(
        "SELECT * FROM stock_movements WHERE shopId = :shopId AND type = 'ADJUSTMENT' " +
            "AND createdAt >= :since ORDER BY createdAt DESC",
    )
    fun observeAdjustmentsSince(shopId: Long, since: Long): Flow<List<StockMovement>>

    // --- Undo (Phase 15) ---
    @Query("SELECT * FROM stock_movements WHERE id = :id")
    suspend fun getById(id: Long): StockMovement?

    @Query("DELETE FROM stock_movements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stock_movements WHERE refId = :saleId AND type = 'SALE'")
    suspend fun deleteSaleMovements(saleId: Long)

    @Query("DELETE FROM stock_movements WHERE refId = :purchaseId AND type = 'PURCHASE'")
    suspend fun deletePurchaseMovements(purchaseId: Long)

    @Query("SELECT * FROM stock_movements WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<StockMovement>

    @Query("DELETE FROM stock_movements WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
