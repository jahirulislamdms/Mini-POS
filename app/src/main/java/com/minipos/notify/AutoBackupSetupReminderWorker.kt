package com.minipos.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minipos.ServiceLocator
import kotlinx.coroutines.flow.first

/**
 * Daily 11:00 AM nudge (Phase 36): automatic backup is enabled but no backup folder has been
 * chosen yet, so backups can't run. Repeats every day until a folder is selected (the reminder is
 * cancelled by [AutoBackupScheduler.sync] as soon as one is). Tapping the notification opens
 * Settings → Backup & restore.
 */
class AutoBackupSetupReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        ServiceLocator.init(context) // idempotent

        val settings = ServiceLocator.autoBackupPrefs.settings.first()
        if (settings.enabled && settings.folderUri == null) {
            Notifier.show(
                context,
                Notifier.ID_AUTO_BACKUP_SETUP,
                "Finish automatic backup setup",
                "Choose a backup folder so MINI POS can back up your data automatically.",
                openBackup = true,
            )
            // Keep nudging daily until a folder is selected.
            AutoBackupScheduler.scheduleSetupReminder(context)
        }
        return Result.success()
    }
}
