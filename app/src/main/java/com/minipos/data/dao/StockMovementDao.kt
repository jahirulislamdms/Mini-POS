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

    @Query("SELECT * FROM stock_movements WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<StockMovement>

    @Query("DELETE FROM stock_movements WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
