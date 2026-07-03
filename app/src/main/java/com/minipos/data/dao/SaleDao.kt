package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.minipos.data.entity.PaymentType
import com.minipos.data.entity.Sale
import com.minipos.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert suspend fun insertSale(sale: Sale): Long
    @Insert suspend fun insertItems(items: List<SaleItem>)

    @Query("SELECT * FROM sales WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun observeItems(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItems(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sales WHERE shopId = :shopId")
    suspend fun getSalesForShop(shopId: Long): List<Sale>

    @Query("SELECT * FROM sale_items WHERE shopId = :shopId")
    suspend fun getSaleItemsForShop(shopId: Long): List<SaleItem>

    @Query("SELECT CAST(COALESCE(SUM(total), 0) AS INTEGER) FROM sales WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end")
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    @Query(
        "SELECT CAST(COALESCE(SUM(paidAmount), 0) AS INTEGER) FROM sales " +
            "WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end",
    )
    fun observeCashInBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    /** All-time cash received from sales (Phase 17 petty-cash balance). */
    @Query("SELECT CAST(COALESCE(SUM(paidAmount), 0) AS INTEGER) FROM sales WHERE shopId = :shopId")
    fun observeTotalPaid(shopId: Long): Flow<Long>

    /** Profit = Σ(lineTotal − buyPrice×qty) over sale items in range (quick lines have 0 cost). */
    @Query(
        "SELECT CAST(COALESCE(SUM(si.lineTotal - COALESCE(p.buyPrice, 0) * si.quantity), 0) AS INTEGER) " +
            "FROM sale_items si JOIN sales s ON si.saleId = s.id LEFT JOIN products p ON si.productId = p.id " +
            "WHERE s.shopId = :shopId AND s.createdAt BETWEEN :start AND :end",
    )
    fun observeProfitBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    @Query(
        "SELECT CAST(COALESCE(SUM(si.lineTotal - COALESCE(p.buyPrice, 0) * si.quantity), 0) AS INTEGER) " +
            "FROM sale_items si JOIN sales s ON si.saleId = s.id LEFT JOIN products p ON si.productId = p.id " +
            "WHERE s.shopId = :shopId AND s.paymentType = :paymentType AND s.createdAt BETWEEN :start AND :end",
    )
    fun observeProfitBetweenByPayment(shopId: Long, paymentType: PaymentType, start: Long, end: Long): Flow<Long>

    @Query("DELETE FROM sales WHERE shopId = :shopId")
    suspend fun deleteSalesForShop(shopId: Long)

    @Query("DELETE FROM sale_items WHERE shopId = :shopId")
    suspend fun deleteItemsForShop(shopId: Long)

    // --- Undo (Phase 15) ---
    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsBySaleId(saleId: Long)
}
