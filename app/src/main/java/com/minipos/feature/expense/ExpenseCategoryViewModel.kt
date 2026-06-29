package com.minipos.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.ExpenseCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseCategoryViewModel : ViewModel() {

    private val repo = ServiceLocator.expenseRepository
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val categories: StateFlow<List<ExpenseCategory>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.addCategory(shopId, name)
    }

    fun rename(category: ExpenseCategory, newName: String) = viewModelScope.launch {
        repo.updateCategory(category.copy(name = newName))
    }

    fun delete(category: ExpenseCategory) = viewModelScope.launch {
        repo.deleteCategory(category)
    }
}
