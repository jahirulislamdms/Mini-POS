package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.MeasureUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasureUnitDao {

    @Insert suspend fun insert(unit: MeasureUnit): Long
    @Insert suspend fun insertAll(units: List<MeasureUnit>)
    @Update suspend fun update(unit: MeasureUnit)
    @Delete suspend fun delete(unit: MeasureUnit)

    @Query("SELECT * FROM units WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun observeByShop(shopId: Long): Flow<List<MeasureUnit>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: Long): MeasureUnit?

    @Query("SELECT * FROM units WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<MeasureUnit>

    @Query("DELETE FROM units WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
