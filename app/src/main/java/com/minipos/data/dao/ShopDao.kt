package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.Shop
import com.minipos.data.entity.ShopSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // --- Shop ---
    @Insert suspend fun insertShop(shop: Shop): Long
    @Update suspend fun updateShop(shop: Shop)
    @Delete suspend fun deleteShop(shop: Shop)

    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    fun observeShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getShop(shopId: Long): Shop?

    @Query("SELECT * FROM shops WHERE id = :shopId")
    fun observeShop(shopId: Long): Flow<Shop?>

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun shopCount(): Int

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShopById(shopId: Long)

    // --- ShopSettings ---
    @Insert suspend fun insertSettings(settings: ShopSettings): Long
    @Update suspend fun updateSettings(settings: ShopSettings)

    @Query("SELECT * FROM shop_settings WHERE shopId = :shopId LIMIT 1")
    suspend fun getSettings(shopId: Long): ShopSettings?

    @Query("SELECT * FROM shop_settings WHERE shopId = :shopId LIMIT 1")
    fun observeSettings(shopId: Long): Flow<ShopSettings?>

    @Query("DELETE FROM shop_settings WHERE shopId = :shopId")
    suspend fun deleteSettingsForShop(shopId: Long)
}
