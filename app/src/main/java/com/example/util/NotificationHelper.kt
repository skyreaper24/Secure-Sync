package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

object NotificationHelper {
    private const val CHANNEL_ID = "securesync_channel"
    private const val CHANNEL_NAME = "SecureSync Push Notifications"
    private const val CHANNEL_DESC = "Notifies on real-time database synchronization events."
    private var isChannelCreated = false

    fun createNotificationChannel(context: Context) {
        if (isChannelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            isChannelCreated = true
        }
    }

    fun triggerNotification(context: Context, title: String, content: String) {
        try {
            createNotificationChannel(context)
            
            // Check POST_NOTIFICATIONS permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                val check = context.checkSelfPermission(permission)
                if (check != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted. Suppressing notification.")
                    return
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                // Using timestamp as unique ID
                notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException triggering notification: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error triggering notification: ${e.message}")
        }
    }
}
