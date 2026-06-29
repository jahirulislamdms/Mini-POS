package com.minipos.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
