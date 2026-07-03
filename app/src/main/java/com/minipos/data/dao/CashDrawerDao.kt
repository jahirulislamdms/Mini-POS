package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minipos.data.entity.CashDrawerOpening
import kotlinx.coroutines.flow.Flow

@Dao
interface CashDrawerDao {

    /** One opening per shop+day (unique index) — re-entering replaces the old value. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(opening: CashDrawerOpening)

    @Query("SELECT * FROM cash_drawer_openings WHERE shopId = :shopId AND dayStart = :dayStart")
    fun observeForDay(shopId: Long, dayStart: Long): Flow<CashDrawerOpening?>

    @Query("SELECT * FROM cash_drawer_openings WHERE shopId = :shopId AND dayStart BETWEEN :start AND :end")
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<CashDrawerOpening>>

    @Query("DELETE FROM cash_drawer_openings WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
