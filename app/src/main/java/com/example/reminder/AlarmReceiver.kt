package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", 0L)
        val title = intent.getStringExtra("EXTRA_REMINDER_TITLE") ?: "Lời nhắc LifeLog"
        val description = intent.getStringExtra("EXTRA_REMINDER_DESC") ?: "Đã đến thời gian nhắc nhở của bạn."
        val enableSound = intent.getBooleanExtra("EXTRA_REMINDER_SOUND", true)
        val enableVibration = intent.getBooleanExtra("EXTRA_REMINDER_VIBRATE", true)

        val channelId = if (enableSound && enableVibration) {
            "lifelog_reminder_channel_sound_vibrate"
        } else if (enableSound) {
            "lifelog_reminder_channel_sound_only"
        } else if (enableVibration) {
            "lifelog_reminder_channel_vibrate_only"
        } else {
            "lifelog_reminder_channel_silent"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Lời nhắc nhở cá nhân"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Thông báo nhắc nhở từ ứng dụng LifeLog"
                enableVibration(enableVibration)
                if (enableVibration) {
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                } else {
                    vibrationPattern = longArrayOf(0)
                }

                if (enableSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTapIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingTapIntent)
            .setAutoCancel(true)

        if (enableVibration) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500))
        }

        if (enableSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        notificationManager.notify(reminderId.toInt(), builder.build())
    }
}
