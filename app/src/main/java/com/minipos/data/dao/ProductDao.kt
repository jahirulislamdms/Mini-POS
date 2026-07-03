package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert suspend fun insert(product: Product): Long
    @Update suspend fun update(product: Product)
    @Delete suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun observeByShop(shopId: Long): Flow<List<Product>>

    @Query(
        "SELECT * FROM products WHERE shopId = :shopId AND name LIKE '%' || :query || '%' " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun search(shopId: Long, query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE shopId = :shopId AND categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun observeByCategory(shopId: Long, categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<Product?>

    /** Products at or below their effective low-stock threshold (falls back to the shop default). */
    @Query(
        "SELECT * FROM products WHERE shopId = :shopId AND lowStockAlertEnabled = 1 " +
            "AND stock <= COALESCE(lowStockThreshold, :shopDefault) ORDER BY stock ASC",
    )
    fun observeLowStock(shopId: Long, shopDefault: Double): Flow<List<Product>>

    @Query("UPDATE products SET stock = stock + :delta, updatedAt = :at WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double, at: Long)

    @Query("SELECT COUNT(*) FROM products WHERE shopId = :shopId")
    fun observeCount(shopId: Long): Flow<Int>

    @Query("SELECT CAST(COALESCE(SUM(stock * buyPrice), 0) AS INTEGER) FROM products WHERE shopId = :shopId")
    fun observeStockValue(shopId: Long): Flow<Long>

    @Query("DELETE FROM products WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)

    // --- Barcodes (Phase 28) ---
    @Query("SELECT * FROM products WHERE shopId = :shopId AND barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(shopId: Long, barcode: String): Product?

    @Query("SELECT * FROM products WHERE barcode IS NULL OR barcode = ''")
    suspend fun getAllWithoutBarcode(): List<Product>

    @Query("UPDATE products SET barcode = :barcode WHERE id = :id")
    suspend fun setBarcode(id: Long, barcode: String)
}
