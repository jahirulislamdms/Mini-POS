package com.minipos.notify

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.minipos.ServiceLocator
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Result of one automatic-backup attempt, with a human-readable reason on failure (Phase 36). */
sealed interface AutoBackupOutcome {
    data object Success : AutoBackupOutcome

    /** No shop exists yet (fresh install before first-run setup) — nothing to back up. */
    data object NoShop : AutoBackupOutcome
    data class Failure(val reason: String) : AutoBackupOutcome
}

/**
 * Executes one automatic backup of the current shop into a SAF folder — the exact same zip format
 * and logic as manual backups ([com.minipos.data.backup.BackupManager.export]) — then prunes old
 * automatic backups. Extracted from [AutoBackupWorker] in Phase 36 so the first-time folder
 * verification (Backup settings) and the scheduled worker share one code path.
 */
object AutoBackupRunner {

    /**
     * Automatic backups are named "MiniPOSAuto_<shop>_<timestamp>.zip". Manual backups always
     * start with "MiniPOS_" (next char is '_'), so retention can never delete a manual file.
     */
    const val AUTO_PREFIX = "MiniPOSAuto_"
    const val KEEP_COUNT = 15
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /** Backs up the current shop into [treeUriString]; updates lastSuccessAt on success. */
    suspend fun run(context: Context, treeUriString: String): AutoBackupOutcome {
        ServiceLocator.init(context) // idempotent

        val folder = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) }.getOrNull()
        if (folder == null || !folder.exists() || !folder.isDirectory) {
            return AutoBackupOutcome.Failure("Storage unavailable — the backup folder can't be found.")
        }
        if (!folder.canWrite()) {
            return AutoBackupOutcome.Failure("Folder access denied — MINI POS can't write to this folder.")
        }
        val shopId = ServiceLocator.currentShopManager.currentShopId.first()
            ?: return AutoBackupOutcome.NoShop

        return try {
            val shopName = ServiceLocator.database.shopDao().getShop(shopId)?.name ?: "shop"
            val file = folder.createFile("application/zip", autoFileName(shopName))
                ?: return AutoBackupOutcome.Failure("Folder access denied — could not create a file in this folder.")
            try {
                ServiceLocator.backupManager.export(shopId, file.uri)
            } catch (t: Throwable) {
                runCatching { file.delete() } // don't leave a half-written zip behind
                throw t
            }
            pruneOldBackups(folder)
            ServiceLocator.autoBackupPrefs.setLastSuccessAt(System.currentTimeMillis())
            AutoBackupOutcome.Success
        } catch (t: Throwable) {
            AutoBackupOutcome.Failure(reasonFor(t))
        }
    }

    private fun reasonFor(t: Throwable): String {
        val msg = t.message ?: ""
        return when {
            msg.contains("ENOSPC", ignoreCase = true) || msg.contains("no space", ignoreCase = true) ->
                "Insufficient storage space in the backup folder."
            t is SecurityException ->
                "Folder access denied — permission to the folder was lost."
            else -> "Backup failed: ${msg.ifBlank { t.javaClass.simpleName }}"
        }
    }

    fun autoFileName(shopName: String): String {
        val safe = shopName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "shop" }
        return "$AUTO_PREFIX${safe}_${STAMP.format(LocalDateTime.now())}.zip"
    }

    /** Keep the newest [KEEP_COUNT] automatic backups in [folder]; delete the rest. */
    fun pruneOldBackups(folder: DocumentFile) {
        folder.listFiles()
            .filter {
                val name = it.name ?: ""
                it.isFile && name.startsWith(AUTO_PREFIX) && name.endsWith(".zip")
            }
            .sortedWith(
                compareByDescending<DocumentFile> { it.lastModified() }
                    .thenByDescending { it.name ?: "" },
            )
            .drop(KEEP_COUNT)
            .forEach { runCatching { it.delete() } }
    }
}
