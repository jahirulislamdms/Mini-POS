package com.minipos.data.repo

import com.minipos.data.dao.ExpenseDao
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

/** Expenses + custom expense categories (BUILD_PLAN §6.7). */
class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeByShop(shopId: Long): Flow<List<Expense>> = dao.observeByShop(shopId)
    fun observeBetween(shopId: Long, start: Long, end: Long): Flow<List<Expense>> =
        dao.observeBetween(shopId, start, end)
    fun observeTotalBetween(shopId: Long, start: Long, end: Long): Flow<Long> =
        dao.observeTotalBetween(shopId, start, end)
    suspend fun getById(id: Long): Expense? = dao.getById(id)

    suspend fun add(
        shopId: Long,
        categoryId: Long?,
        amount: Long,
        note: String? = null,
        createdAt: Long = System.currentTimeMillis(),
    ): Long =
        dao.insert(
            Expense(
                shopId = shopId,
                categoryId = categoryId,
                amount = amount,
                note = note,
                createdAt = createdAt,
            ),
        )

    suspend fun update(expense: Expense) = dao.update(expense)
    suspend fun delete(expense: Expense) = dao.delete(expense)

    // --- categories ---
    fun observeCategories(shopId: Long): Flow<List<ExpenseCategory>> = dao.observeCategories(shopId)
    suspend fun getCategory(id: Long): ExpenseCategory? = dao.getCategory(id)

    suspend fun addCategory(shopId: Long, name: String): Long =
        dao.insertCategory(ExpenseCategory(shopId = shopId, name = name, createdAt = System.currentTimeMillis()))

    suspend fun updateCategory(category: ExpenseCategory) = dao.updateCategory(category)
    suspend fun deleteCategory(category: ExpenseCategory) = dao.deleteCategory(category)
}
