package com.minipos.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.MeasureUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UnitViewModel : ViewModel() {

    private val repo = ServiceLocator.unitRepository
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val units: StateFlow<List<MeasureUnit>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.add(shopId, name)
    }

    fun rename(unit: MeasureUnit, newName: String) = viewModelScope.launch {
        repo.update(unit.copy(name = newName))
    }

    fun delete(unit: MeasureUnit) = viewModelScope.launch {
        repo.delete(unit)
    }
}
