package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.TaskPulseDatabase
import com.example.notification.TaskPulseAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            val database = TaskPulseDatabase.getDatabase(context)
            val scheduler = TaskPulseAlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeTasks = database.taskDao().getActiveTasks().firstOrNull() ?: emptyList()
                    activeTasks.forEach { task ->
                        if (!task.status.isFinished && !task.isDeleted) {
                            scheduler.scheduleTaskReminders(task)
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
}
