package com.minipos.feature.license

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs both the activation gate and the Settings → License Management screen. */
class LicenseViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = ServiceLocator.licenseManager

    val state: StateFlow<LicenseState> =
        manager.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LicenseState.Loading)

    /** Activate / renew / replace — verifies offline and persists on success. */
    fun activate(key: String, onResult: (ActivationResult) -> Unit) = viewModelScope.launch {
        onResult(manager.activate(key))
    }
}
