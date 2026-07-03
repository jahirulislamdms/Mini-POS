package com.minipos.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.minipos.MainActivity
import com.minipos.R

/** Local notifications for low-stock & due reminders (P12.2). Offline; no network. */
object Notifier {

    const val CHANNEL_ID = "reminders"
    const val ID_LOW_STOCK = 1001
    const val ID_DUE = 1002
    const val ID_BACKUP = 1003

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Low-stock and due reminders" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent(context, id))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /**
     * Tap action (Phase 26): open MINI POS exactly like tapping its launcher icon — cold-launch
     * when closed, bring the existing task to the foreground when backgrounded/open (with
     * `singleTask` on MainActivity, never a second instance). Immutable PendingIntent per
     * Android 12+ requirements.
     */
    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
