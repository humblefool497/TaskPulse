package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.TaskPulseDatabase
import com.example.data.TaskRepository
import com.example.notification.NotificationHelper
import com.example.notification.TaskPulseAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE_REMINDER = "com.example.taskpulse.ACTION_FIRE_REMINDER"
        const val ACTION_COMPLETE = "com.example.taskpulse.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.taskpulse.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val database = TaskPulseDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao())
        val notificationHelper = NotificationHelper(context)
        val alarmScheduler = TaskPulseAlarmScheduler(context)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE_REMINDER -> {
                        val task = repository.getTaskById(taskId)
                        if (task != null && !task.status.isFinished && !task.isDeleted) {
                            notificationHelper.showTaskReminderNotification(task)
                        }
                    }
                    ACTION_COMPLETE -> {
                        val task = repository.getTaskById(taskId)
                        if (task != null) {
                            repository.completeTask(task)
                            alarmScheduler.cancelTaskReminders(taskId)
                            NotificationManagerCompat.from(context).cancel(taskId.toInt())
                        }
                    }
                    ACTION_SNOOZE -> {
                        val snoozeMillis = intent.getLongExtra(NotificationHelper.EXTRA_SNOOZE_MILLIS, 3600_000L)
                        val task = repository.getTaskById(taskId)
                        if (task != null) {
                            repository.snoozeTask(task, snoozeMillis)
                            val updated = repository.getTaskById(taskId)
                            if (updated != null) {
                                alarmScheduler.scheduleTaskReminders(updated)
                            }
                            NotificationManagerCompat.from(context).cancel(taskId.toInt())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
