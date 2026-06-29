package com.minipos.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A top-level category and its sub-categories. */
data class CategoryGroup(val parent: Category, val children: List<Category>)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel : ViewModel() {

    private val repo = ServiceLocator.categoryRepository
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val groups: StateFlow<List<CategoryGroup>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeAll(it) }
        .map { all -> group(all) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun group(all: List<Category>): List<CategoryGroup> {
        val byParent = all.filter { it.parentId != null }.groupBy { it.parentId }
        return all.filter { it.parentId == null }
            .map { parent -> CategoryGroup(parent, byParent[parent.id].orEmpty()) }
    }

    fun addCategory(name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.add(shopId, name, parentId = null)
    }

    fun addSubCategory(parentId: Long, name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.add(shopId, name, parentId = parentId)
    }

    fun rename(category: Category, newName: String) = viewModelScope.launch {
        repo.update(category.copy(name = newName))
    }

    /** Deleting a top-level category also removes its sub-categories. */
    fun delete(category: Category) = viewModelScope.launch {
        if (category.parentId == null) {
            groups.value.firstOrNull { it.parent.id == category.id }
                ?.children
                ?.forEach { repo.delete(it) }
        }
        repo.delete(category)
    }
}
