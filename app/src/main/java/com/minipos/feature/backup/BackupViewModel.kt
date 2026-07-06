package com.minipos.feature.backup

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.data.backup.ImportResult
import com.minipos.data.entity.Shop
import com.minipos.data.prefs.AutoBackupPrefs
import com.minipos.data.prefs.AutoBackupSettings
import com.minipos.notify.AutoBackupOutcome
import com.minipos.notify.AutoBackupRunner
import com.minipos.notify.AutoBackupScheduler
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val backupManager = ServiceLocator.backupManager
    private val shopRepo = ServiceLocator.shopRepository
    private val autoPrefs = ServiceLocator.autoBackupPrefs

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

    // --- Automatic backup (app-wide, Phase 32) ---

    val auto: StateFlow<AutoBackupSettings> = autoPrefs.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AutoBackupSettings(
                enabled = AutoBackupPrefs.DEFAULT_ENABLED,
                folderUri = null,
                frequencyDays = AutoBackupPrefs.DEFAULT_FREQUENCY_DAYS,
                hour = AutoBackupPrefs.DEFAULT_HOUR,
                minute = AutoBackupPrefs.DEFAULT_MINUTE,
                lastSuccessAt = 0L,
            ),
        )

    /** Busy/result of the immediate folder-verification backup (Phase 36). */
    private val _autoBusy = MutableStateFlow(false)
    val autoBusy: StateFlow<Boolean> = _autoBusy

    private val _autoMessage = MutableStateFlow<String?>(null)
    val autoMessage: StateFlow<String?> = _autoMessage

    fun setAutoEnabled(enabled: Boolean) = viewModelScope.launch {
        autoPrefs.setEnabled(enabled)
        resyncSchedulers()
    }

    /**
     * Phase 36: adopt the folder picked with the system folder picker only after verifying it with
     * a real backup (same format/logic as manual backups). On success the folder is persisted
     * (access kept across reboots) and the normal schedule continues; on failure the previous
     * folder (if any) stays in charge and the user sees the reason.
     */
    fun setAutoFolder(uri: Uri) = viewModelScope.launch {
        val context = getApplication<Application>()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        _autoBusy.value = true
        _autoMessage.value = null

        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val previous = autoPrefs.settings.first().folderUri

        when (val outcome = AutoBackupRunner.run(context, uri.toString())) {
            is AutoBackupOutcome.Failure -> {
                // Don't adopt a folder that can't take a backup; drop the new grant.
                if (previous != uri.toString()) {
                    runCatching { context.contentResolver.releasePersistableUriPermission(uri, flags) }
                }
                _autoMessage.value = outcome.reason
            }
            else -> {
                // Success — or NoShop (nothing to back up yet): accept the folder either way.
                if (previous != null && previous != uri.toString()) {
                    runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(previous), flags) }
                }
                autoPrefs.setFolderUri(uri.toString())
                _autoMessage.value = if (outcome is AutoBackupOutcome.Success) {
                    "Backup completed successfully. This folder will be used for automatic backups."
                } else {
                    "Folder saved. Automatic backups start once a shop exists."
                }
                resyncSchedulers()
            }
        }
        _autoBusy.value = false
    }

    fun setAutoFrequency(days: Int) = viewModelScope.launch {
        autoPrefs.setFrequencyDays(days)
        resyncSchedulers()
    }

    fun setAutoTime(hour: Int, minute: Int) = viewModelScope.launch {
        autoPrefs.setTime(hour, minute)
        resyncSchedulers()
    }

    /**
     * Re-align both background jobs after any auto-backup setting change: the auto-backup work
     * itself, and the daily backup reminder (suppressed while auto backup is active, restored
     * per its own prefs when auto backup goes inactive).
     */
    private suspend fun resyncSchedulers() {
        val context = getApplication<Application>()
        AutoBackupScheduler.sync(context)
        val autoActive = autoPrefs.settings.first().active
        val reminderPrefs = ServiceLocator.backupReminderPrefs
        if (!autoActive && reminderPrefs.enabled.first()) {
            BackupReminderScheduler.schedule(context, reminderPrefs.hour.first(), reminderPrefs.minute.first())
        } else {
            BackupReminderScheduler.cancel(context)
        }
    }
}
