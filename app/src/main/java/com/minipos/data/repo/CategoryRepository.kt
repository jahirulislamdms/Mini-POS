package com.minipos.data.repo

import com.minipos.data.dao.CategoryDao
import com.minipos.data.entity.Category
import kotlinx.coroutines.flow.Flow

/** Fully custom categories & sub-categories (BUILD_PLAN §3). */
class CategoryRepository(private val dao: CategoryDao) {

    fun observeTopLevel(shopId: Long): Flow<List<Category>> = dao.observeTopLevel(shopId)
    fun observeSubCategories(shopId: Long, parentId: Long): Flow<List<Category>> =
        dao.observeSubCategories(shopId, parentId)
    fun observeAll(shopId: Long): Flow<List<Category>> = dao.observeAll(shopId)
    suspend fun getById(id: Long): Category? = dao.getById(id)

    suspend fun add(shopId: Long, name: String, parentId: Long? = null): Long =
        dao.insert(Category(shopId = shopId, name = name, parentId = parentId, createdAt = System.currentTimeMillis()))

    suspend fun update(category: Category) = dao.update(category)
    suspend fun delete(category: Category) = dao.delete(category)
}
