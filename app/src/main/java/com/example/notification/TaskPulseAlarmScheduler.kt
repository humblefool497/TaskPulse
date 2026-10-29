package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.TaskItem
import com.example.receiver.AlarmReceiver

class TaskPulseAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminders(task: TaskItem) {
        if (task.status.isFinished || task.isDeleted) {
            cancelTaskReminders(task.id)
            return
        }

        val now = System.currentTimeMillis()

        // 1. Deadline reminder
        if (task.completionDateTime > now) {
            scheduleSingleAlarm(
                requestCode = task.id.toInt(),
                triggerTimeMillis = task.completionDateTime,
                taskId = task.id
            )
        }

        // 2. Offsets (e.g. 15m, 60m, 1440m before)
        task.reminderOffsetsMinutes.forEachIndexed { index, minutesBefore ->
            if (minutesBefore > 0) {
                val trigger = task.completionDateTime - (minutesBefore * 60_000L)
                if (trigger > now) {
                    val reqCode = (task.id * 100 + index + 1).toInt()
                    scheduleSingleAlarm(
                        requestCode = reqCode,
                        triggerTimeMillis = trigger,
                        taskId = task.id
                    )
                }
            }
        }

        // 3. Follow-up reminder if set
        val followUp = task.followUpDate
        if (followUp != null && followUp > now) {
            val reqCode = (task.id * 100 + 99).toInt()
            scheduleSingleAlarm(
                requestCode = reqCode,
                triggerTimeMillis = followUp,
                taskId = task.id
            )
        }
    }

    private fun scheduleSingleAlarm(requestCode: Int, triggerTimeMillis: Long, taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for restricted exact alarm permission
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminders(taskId: Long) {
        // Cancel main alarm
        cancelSingleAlarm(taskId.toInt(), taskId)
        // Cancel possible offset alarms
        for (i in 1..10) {
            cancelSingleAlarm((taskId * 100 + i).toInt(), taskId)
        }
        // Cancel follow-up alarm
        cancelSingleAlarm((taskId * 100 + 99).toInt(), taskId)
    }

    private fun cancelSingleAlarm(requestCode: Int, taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
