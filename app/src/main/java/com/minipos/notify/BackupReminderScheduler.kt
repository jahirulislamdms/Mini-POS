package com.minipos.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily backup reminder. Uses a self-rescheduling one-time worker so it fires at the
 * exact chosen time each day (the worker re-enqueues the next day after it runs).
 */
object BackupReminderScheduler {

    const val WORK_NAME = "minipos_backup_reminder"

    /** (Re)schedule the next reminder at [hour]:[minute]. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val delayMillis = millisUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<BackupReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
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
