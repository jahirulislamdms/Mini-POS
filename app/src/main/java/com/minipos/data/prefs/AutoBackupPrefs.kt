package com.minipos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.autoBackupDataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_auto_backup_prefs")

/** Snapshot of the automatic-backup configuration (Phase 32). */
data class AutoBackupSettings(
    val enabled: Boolean,
    /** Persisted SAF tree Uri of the chosen backup folder, or null while none is selected. */
    val folderUri: String?,
    val frequencyDays: Int,
    val hour: Int,
    val minute: Int,
    /** Millis of the last successful automatic backup; 0 = never. */
    val lastSuccessAt: Long,
) {
    /** True when automatic backups can actually run (enabled AND a folder is chosen). */
    val active: Boolean get() = enabled && folderUri != null
}

/**
 * App-wide (not per-shop) automatic-backup settings (Phase 32). Default: enabled, every 1 day at
 * 23:00 (11:00 PM), no folder until the user picks one. Local only; survives restarts.
 */
class AutoBackupPrefs(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("auto_backup_enabled")
    private val keyFolderUri = stringPreferencesKey("auto_backup_folder_uri")
    private val keyFrequencyDays = intPreferencesKey("auto_backup_frequency_days")
    private val keyHour = intPreferencesKey("auto_backup_hour")
    private val keyMinute = intPreferencesKey("auto_backup_minute")
    private val keyLastSuccessAt = longPreferencesKey("auto_backup_last_success_at")

    val settings: Flow<AutoBackupSettings> = context.autoBackupDataStore.data.map { p ->
        AutoBackupSettings(
            enabled = p[keyEnabled] ?: DEFAULT_ENABLED,
            folderUri = p[keyFolderUri]?.takeIf { it.isNotBlank() },
            frequencyDays = (p[keyFrequencyDays] ?: DEFAULT_FREQUENCY_DAYS).coerceAtLeast(1),
            hour = (p[keyHour] ?: DEFAULT_HOUR).coerceIn(0, 23),
            minute = (p[keyMinute] ?: DEFAULT_MINUTE).coerceIn(0, 59),
            lastSuccessAt = p[keyLastSuccessAt] ?: 0L,
        )
    }

    suspend fun setEnabled(value: Boolean) {
        context.autoBackupDataStore.edit { it[keyEnabled] = value }
    }

    suspend fun setFolderUri(uri: String) {
        context.autoBackupDataStore.edit { it[keyFolderUri] = uri }
    }

    suspend fun setFrequencyDays(days: Int) {
        context.autoBackupDataStore.edit { it[keyFrequencyDays] = days.coerceAtLeast(1) }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.autoBackupDataStore.edit {
            it[keyHour] = hour.coerceIn(0, 23)
            it[keyMinute] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setLastSuccessAt(millis: Long) {
        context.autoBackupDataStore.edit { it[keyLastSuccessAt] = millis }
    }

    companion object {
        const val DEFAULT_ENABLED = true
        const val DEFAULT_FREQUENCY_DAYS = 1
        const val DEFAULT_HOUR = 23
        const val DEFAULT_MINUTE = 0
    }
}
