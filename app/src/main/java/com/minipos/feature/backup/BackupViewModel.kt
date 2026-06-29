package com.minipos.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.backup.ImportResult
import com.minipos.data.entity.Shop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModel : ViewModel() {

    private val backupManager = ServiceLocator.backupManager
    private val shopRepo = ServiceLocator.shopRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    val shop: StateFlow<Shop?> = shopIdState.filterNotNull()
        .flatMapLatest { shopRepo.observeShop(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun suggestedFileName(): String {
        val name = (shop.value?.name ?: "shop").replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "shop" }
        val date = LocalDate.now().toString().replace("-", "")
        return "MiniPOS_${name}_$date.zip"
    }

    fun export(uri: Uri) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        _busy.value = true
        _message.value = null
        _message.value = runCatching { backupManager.export(shopId, uri) }
            .fold(
                onSuccess = { "Backup saved ($it rows)." },
                onFailure = { "Backup failed: ${it.message}" },
            )
        _busy.value = false
    }

    fun import(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        _message.value = null
        _message.value = runCatching { backupManager.import(uri) }
            .fold(
                onSuccess = { result ->
                    when (result) {
                        is ImportResult.Success -> "Restored ${result.rows} rows into a new shop. Switched to it."
                        is ImportResult.Failure -> result.message
                    }
                },
                onFailure = { "Restore failed: ${it.message}" },
            )
        _busy.value = false
    }
}
