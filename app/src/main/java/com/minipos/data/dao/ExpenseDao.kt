package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Expense ---
    @Insert suspend fun insert(expense: Expense): Long
    @Update suspend fun update(expense: Expense)
    @Delete suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM expenses WHERE shopId = :shopId AND createdAt BETWEEN :start AND :end")
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long>

    /** All-time expense total (Phase 17 petty-cash balance). */
    @Query("SELECT CAST(COALESCE(SUM(amount), 0) AS INTEGER) FROM expenses WHERE shopId = :shopId")
    fun observeTotal(shopId: Long): Flow<Long>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE shopId = :shopId")
    suspend fun getAllForShop(shopId: Long): List<Expense>

    @Query("DELETE FROM expenses WHERE shopId = :shopId")
    suspend fun deleteExpensesForShop(shopId: Long)

    /** Undo (Phase 15). */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- ExpenseCategory (custom, seeded per shop) ---
    @Insert suspend fun insertCategory(category: ExpenseCategory): Long
    @Insert suspend fun insertCategories(categories: List<ExpenseCategory>)
    @Update suspend fun updateCategory(category: ExpenseCategory)
    @Delete suspend fun deleteCategory(category: ExpenseCategory)

    @Query("SELECT * FROM expense_categories WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun observeCategories(shopId: Long): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getCategory(id: Long): ExpenseCategory?

    @Query("SELECT * FROM expense_categories WHERE shopId = :shopId")
    suspend fun getCategoriesForShop(shopId: Long): List<ExpenseCategory>

    @Query("DELETE FROM expense_categories WHERE shopId = :shopId")
    suspend fun deleteCategoriesForShop(shopId: Long)
}
