package com.minipos.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.entity.ShopSettings
import com.minipos.data.prefs.BackupReminderPrefs
import com.minipos.notify.BackupReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val shopRepo = ServiceLocator.shopRepository
    private val reminderPrefs = ServiceLocator.backupReminderPrefs
    private val shopIdState = MutableStateFlow<Long?>(null)

    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val settings: StateFlow<ShopSettings?> = shopIdState
        .filterNotNull()
        .flatMapLatest { shopRepo.observeSettings(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLowStockDefault(value: Double) = viewModelScope.launch {
        settings.value?.let { shopRepo.updateSettings(it.copy(lowStockDefault = value)) }
    }

    fun setLowStockNotify(enabled: Boolean) = viewModelScope.launch {
        settings.value?.let { shopRepo.updateSettings(it.copy(lowStockNotify = enabled)) }
    }

    fun setDueNotify(enabled: Boolean) = viewModelScope.launch {
        settings.value?.let { shopRepo.updateSettings(it.copy(dueNotify = enabled)) }
    }

    // --- Backup reminder (app-wide) ---
    val backupReminderEnabled: StateFlow<Boolean> = reminderPrefs.enabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderPrefs.DEFAULT_ENABLED)
    val backupReminderHour: StateFlow<Int> = reminderPrefs.hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderPrefs.DEFAULT_HOUR)
    val backupReminderMinute: StateFlow<Int> = reminderPrefs.minute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderPrefs.DEFAULT_MINUTE)

    fun setBackupReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        reminderPrefs.setEnabled(enabled)
        val context = getApplication<Application>()
        // Phase 32: while automatic backup is active the reminder stays cancelled.
        val autoActive = ServiceLocator.autoBackupPrefs.settings.first().active
        if (enabled && !autoActive) {
            BackupReminderScheduler.schedule(context, backupReminderHour.value, backupReminderMinute.value)
        } else {
            BackupReminderScheduler.cancel(context)
        }
    }

    fun setBackupReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        reminderPrefs.setTime(hour, minute)
        val autoActive = ServiceLocator.autoBackupPrefs.settings.first().active
        if (backupReminderEnabled.value && !autoActive) {
            BackupReminderScheduler.schedule(getApplication(), hour, minute)
        }
    }
}
