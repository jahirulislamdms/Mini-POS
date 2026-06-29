package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert suspend fun insert(category: Category): Long
    @Update suspend fun update(category: Category)
    @Delete suspend fun delete(category: Category)

    /** Top-level categories (parentId IS NULL) for a shop. */
    @Query("SELECT * FROM categories WHERE shopId = :shopId AND parentId IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun observeTopLevel(shopId: Long): Flow<List<Category>>

    /** Sub-categories of a given parent. */
    @Query("SELECT * FROM categories WHERE shopId = :shopId AND parentId = :parentId ORDER BY name COLLATE NOCASE ASC")
    fun observeSubCategories(shopId: Long, parentId: Long): Flow<List<Category>>

    /** All categories (top-level and sub) for a shop. */
    @Query("SELECT * FROM categories WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(shopId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<Category>

    @Query("DELETE FROM categories WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
