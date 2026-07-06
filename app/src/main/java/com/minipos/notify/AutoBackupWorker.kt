package com.minipos.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minipos.ServiceLocator
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Creates an automatic backup of the current shop into the user-chosen SAF folder (Phase 32),
 * via [AutoBackupRunner] — same zip format as manual backups, newest [AutoBackupRunner.KEEP_COUNT]
 * kept. Then re-schedules the next run. Failures notify the user; a failed run retries next day.
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        ServiceLocator.init(context) // idempotent

        val prefs = ServiceLocator.autoBackupPrefs
        val settings = prefs.settings.first()
        if (!settings.enabled) return Result.success()
        // No folder yet: nothing to back up — the daily setup reminder (Phase 36) nudges the user.
        val folderUri = settings.folderUri ?: return Result.success()

        val success = when (val outcome = AutoBackupRunner.run(context, folderUri)) {
            AutoBackupOutcome.Success -> true
            // Fresh install before first-run setup: no error notification, just try again later.
            AutoBackupOutcome.NoShop -> false
            is AutoBackupOutcome.Failure -> {
                Notifier.show(context, Notifier.ID_AUTO_BACKUP, "Automatic backup failed", outcome.reason)
                false
            }
        }

        if (success) {
            AutoBackupScheduler.sync(context)
        } else {
            if (settings.lastSuccessAt > 0 &&
                System.currentTimeMillis() - settings.lastSuccessAt >= STALE_AFTER_MILLIS
            ) {
                Notifier.show(
                    context,
                    Notifier.ID_AUTO_BACKUP_STALE,
                    "Automatic backup needs attention",
                    "No automatic backup has been created for 3 days. Open MINI POS and check the backup settings.",
                )
            }
            AutoBackupScheduler.scheduleRetryNextDay(context, settings.hour, settings.minute)
        }
        return Result.success()
    }

    private companion object {
        val STALE_AFTER_MILLIS: Long = TimeUnit.DAYS.toMillis(3)
    }
}
