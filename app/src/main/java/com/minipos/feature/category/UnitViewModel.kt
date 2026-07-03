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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UnitViewModel : ViewModel() {

    private val repo = ServiceLocator.unitRepository
    private val shopRepo = ServiceLocator.shopRepository
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val units: StateFlow<List<MeasureUnit>> = shopIdState
        .filterNotNull()
        .flatMapLatest { repo.observeByShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The shop's default unit for new products (Phase 31). */
    val defaultUnit: StateFlow<String> = shopIdState
        .filterNotNull()
        .flatMapLatest { shopRepo.observeSettings(it) }
        .map { it?.defaultUnit ?: "pcs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Make [name] the default unit — newly created products will pre-select it. */
    fun setDefault(name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        shopRepo.getSettings(shopId)?.let { shopRepo.updateSettings(it.copy(defaultUnit = name)) }
    }

    fun add(name: String) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        repo.add(shopId, name)
    }

    fun rename(unit: MeasureUnit, newName: String) = viewModelScope.launch {
        repo.update(unit.copy(name = newName))
        // Keep the default pointing at the renamed unit (Phase 31).
        if (unit.name == defaultUnit.value) setDefault(newName)
    }

    fun delete(unit: MeasureUnit) = viewModelScope.launch {
        repo.delete(unit)
    }
}
