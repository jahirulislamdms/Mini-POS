package com.minipos.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minipos.ServiceLocator
import com.minipos.data.prefs.AutoBackupSettings
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the automatic backup job (Phase 32). Like [BackupReminderScheduler] it uses a
 * self-rescheduling one-time worker so backups run at the exact configured time; the worker
 * re-enqueues the next run after each attempt.
 */
object AutoBackupScheduler {

    const val WORK_NAME = "minipos_auto_backup"
    const val SETUP_REMINDER_WORK_NAME = "minipos_auto_backup_setup_reminder"

    /** Daily time of the "choose a backup folder" reminder (Phase 36): 11:00 AM. */
    private const val SETUP_REMINDER_HOUR = 11
    private const val SETUP_REMINDER_MINUTE = 0

    /**
     * Aligns the background job with the saved settings: cancelled while disabled or while no
     * folder is chosen; runs immediately when a scheduled backup was missed (device off / app not
     * running at the scheduled time); otherwise waits until the next scheduled date + time.
     * Safe to call from app start, the Backup settings screen and the worker itself.
     */
    suspend fun sync(context: Context) {
        ServiceLocator.init(context) // idempotent
        val settings = ServiceLocator.autoBackupPrefs.settings.first()

        // Phase 36: while enabled but folder-less, nudge daily at 11:00 AM to finish the setup;
        // the reminder stops automatically once a valid folder is chosen (or the switch goes off).
        if (settings.enabled && settings.folderUri == null) {
            scheduleSetupReminder(context)
        } else {
            cancelSetupReminder(context)
        }

        if (!settings.active) {
            cancel(context)
            return
        }
        val delay = (nextRunAt(settings) - System.currentTimeMillis()).coerceAtLeast(0)
        enqueue(context, delay)
    }

    /** (Re)schedule the next 11:00 AM setup reminder ([AutoBackupSetupReminderWorker]). */
    fun scheduleSetupReminder(context: Context) {
        val request = OneTimeWorkRequestBuilder<AutoBackupSetupReminderWorker>()
            .setInitialDelay(millisUntilNext(SETUP_REMINDER_HOUR, SETUP_REMINDER_MINUTE), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SETUP_REMINDER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelSetupReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SETUP_REMINDER_WORK_NAME)
    }

    /** After a failed attempt: retry at the next occurrence of [hour]:[minute] (never a hot loop). */
    fun scheduleRetryNextDay(context: Context, hour: Int, minute: Int) {
        enqueue(context, millisUntilNext(hour, minute))
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(context: Context, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Next due moment: [AutoBackupSettings.frequencyDays] after the last successful backup's date
     * at the configured time — in the past when a backup was missed, which makes [sync] run it
     * right away. Before the first ever backup: the next occurrence of the configured time.
     */
    private fun nextRunAt(s: AutoBackupSettings): Long {
        val zone = ZoneId.systemDefault()
        return if (s.lastSuccessAt > 0) {
            Instant.ofEpochMilli(s.lastSuccessAt).atZone(zone).toLocalDate()
                .plusDays(s.frequencyDays.toLong())
                .atTime(s.hour, s.minute)
                .atZone(zone).toInstant().toEpochMilli()
        } else {
            val now = ZonedDateTime.now()
            var next = now.withHour(s.hour).withMinute(s.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            next.toInstant().toEpochMilli()
        }
    }

    private fun millisUntilNext(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }
}
