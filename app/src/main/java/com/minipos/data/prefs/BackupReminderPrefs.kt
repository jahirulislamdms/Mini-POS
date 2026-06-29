package com.minipos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_notif_prefs")

/**
 * App-wide (not per-shop) backup-reminder settings: whether the daily reminder is enabled and at
 * what time. Default: enabled, 22:00 (10:00 PM). Local only.
 */
class BackupReminderPrefs(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("backup_reminder_enabled")
    private val keyHour = intPreferencesKey("backup_reminder_hour")
    private val keyMinute = intPreferencesKey("backup_reminder_minute")

    val enabled: Flow<Boolean> = context.notifDataStore.data.map { it[keyEnabled] ?: DEFAULT_ENABLED }
    val hour: Flow<Int> = context.notifDataStore.data.map { it[keyHour] ?: DEFAULT_HOUR }
    val minute: Flow<Int> = context.notifDataStore.data.map { it[keyMinute] ?: DEFAULT_MINUTE }

    suspend fun setEnabled(value: Boolean) {
        context.notifDataStore.edit { it[keyEnabled] = value }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.notifDataStore.edit {
            it[keyHour] = hour
            it[keyMinute] = minute
        }
    }

    companion object {
        const val DEFAULT_ENABLED = true
        const val DEFAULT_HOUR = 22
        const val DEFAULT_MINUTE = 0
    }
}
