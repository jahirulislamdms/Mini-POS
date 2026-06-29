package com.minipos

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.minipos.notify.BackupReminderScheduler
import com.minipos.notify.Notifier
import com.minipos.notify.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Application entry point. Builds the database, repositories and prefs once at startup
 * via [ServiceLocator] (manual DI per CONVENTIONS §2); sets up the reminder channel + worker.
 */
class MiniPosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifier.ensureChannel(this)
        scheduleReminders()
        scheduleBackupReminder()
    }

    private fun scheduleReminders() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "minipos_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Align the daily backup reminder with the saved enabled/time prefs. */
    private fun scheduleBackupReminder() {
        CoroutineScope(Dispatchers.Default).launch {
            val prefs = ServiceLocator.backupReminderPrefs
            if (prefs.enabled.first()) {
                BackupReminderScheduler.schedule(this@MiniPosApp, prefs.hour.first(), prefs.minute.first())
            } else {
                BackupReminderScheduler.cancel(this@MiniPosApp)
            }
        }
    }
}
