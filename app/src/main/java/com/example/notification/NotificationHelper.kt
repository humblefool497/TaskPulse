package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Criticality
import com.example.model.TaskItem
import com.example.receiver.AlarmReceiver

class NotificationHelper(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_CRITICAL = "channel_taskpulse_critical"
        const val CHANNEL_HIGH = "channel_taskpulse_high"
        const val CHANNEL_MEDIUM = "channel_taskpulse_medium"
        const val CHANNEL_LOW = "channel_taskpulse_low"
        const val CHANNEL_DIGEST = "channel_taskpulse_digest"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SNOOZE_MILLIS = "extra_snooze_millis"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val criticalChannel = NotificationChannel(
                    CHANNEL_CRITICAL,
                    "Critical Tasks & Immediate Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alerts for Critical and Immediate Attention tasks"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setSound(alarmSound, audioAttributes)
                }

                val highChannel = NotificationChannel(
                    CHANNEL_HIGH,
                    "High Priority Tasks",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for High priority deadlines"
                    enableVibration(true)
                    setSound(alarmSound, audioAttributes)
                }

                val mediumChannel = NotificationChannel(
                    CHANNEL_MEDIUM,
                    "Medium Priority Tasks",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Standard task reminders"
                }

                val lowChannel = NotificationChannel(
                    CHANNEL_LOW,
                    "Low Priority Tasks",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Gentle reminders for low priority tasks"
                }

                val digestChannel = NotificationChannel(
                    CHANNEL_DIGEST,
                    "Daily Digest & Evening Review",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Morning focus digest and evening progress review"
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannels(
                    listOf(criticalChannel, highChannel, mediumChannel, lowChannel, digestChannel)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showTaskReminderNotification(task: TaskItem) {
        val channelId = when (task.criticality) {
            Criticality.CRITICAL -> CHANNEL_CRITICAL
            Criticality.HIGH -> CHANNEL_HIGH
            Criticality.MEDIUM -> CHANNEL_MEDIUM
            Criticality.LOW -> CHANNEL_LOW
        }

        // Tap opens MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done action
        val completeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + 10000).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 1 Hour action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SNOOZE_MILLIS, 3600_000L) // 1 hour
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + 20000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("[${task.criticality.label}] ${task.title}")
            .setContentText("${task.category} • ${task.actionNeeded.label}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${task.title}\n\n${task.description.ifBlank { "Action: " + task.actionNeeded.label }}\nCategory: ${task.category} | From: ${task.taskOriginatedFrom}")
            )
            .setPriority(if (task.criticality == Criticality.CRITICAL) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 1h", snoozePendingIntent)

        try {
            notificationManager.notify(task.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showMorningDigest(todayCount: Int, overdueCount: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999901,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = when {
            overdueCount > 0 -> "You have $todayCount tasks due today, and $overdueCount overdue tasks that need attention."
            todayCount > 0 -> "You have $todayCount tasks scheduled for today. Let's make it a productive day!"
            else -> "Your schedule is clear for today. Great job keeping on top of your responsibilities!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TaskPulse Morning Focus")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(999901, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEveningReview(unfinishedCount: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999902,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (unfinishedCount > 0) {
            "You have $unfinishedCount unfinished tasks from today. Open TaskPulse to wrap them up or move to tomorrow."
        } else {
            "All planned tasks completed today! Well done."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TaskPulse Evening Review")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(999902, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
