package com.minipos.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minipos.ServiceLocator
import kotlinx.coroutines.flow.first

/**
 * Posts the daily "back up your data" reminder (local only), then re-schedules itself for the next
 * day if still enabled (P-postrelease-3). Gated by [BackupReminderPrefs].enabled.
 */
class BackupReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        ServiceLocator.init(context) // idempotent

        val prefs = ServiceLocator.backupReminderPrefs
        if (prefs.enabled.first()) {
            Notifier.show(
                context,
                Notifier.ID_BACKUP,
                "Backup reminder",
                "Reminder: Please back up your MINI POS data to keep your records safe.",
            )
            // Schedule the next day's reminder.
            BackupReminderScheduler.schedule(context, prefs.hour.first(), prefs.minute.first())
        }
        return Result.success()
    }
}
