package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.PurchaseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Insert suspend fun insertPurchase(purchase: Purchase): Long
    @Insert suspend fun insertItems(items: List<PurchaseItem>)

    @Query("SELECT * FROM purchases WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: Long): Purchase?

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun observeItems(purchaseId: Long): Flow<List<PurchaseItem>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItems(purchaseId: Long): List<PurchaseItem>

    @Query("SELECT * FROM purchases WHERE shopId = :shopId")
    suspend fun getPurchasesForShop(shopId: Long): List<Purchase>

    @Query("SELECT * FROM purchase_items WHERE shopId = :shopId")
    suspend fun getPurchaseItemsForShop(shopId: Long): List<PurchaseItem>

    @Query("SELECT CAST(COALESCE(SUM(total), 0) AS INTEGER) FROM purchases WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end")
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    @Query(
        "SELECT CAST(COALESCE(SUM(paidAmount), 0) AS INTEGER) FROM purchases " +
            "WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end",
    )
    fun observeCashOutBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    @Query("DELETE FROM purchases WHERE shopId = :shopId")
    suspend fun deletePurchasesForShop(shopId: Long)

    @Query("DELETE FROM purchase_items WHERE shopId = :shopId")
    suspend fun deleteItemsForShop(shopId: Long)

    // --- Undo (Phase 15) ---
    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: Long)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsByPurchaseId(purchaseId: Long)
}
