package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.TaskPulseDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyDigestWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = TaskPulseDatabase.getDatabase(context)
            val dao = database.taskDao()
            val notificationHelper = NotificationHelper(context)

            // 1. Purge trash older than 30 days
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            dao.purgeOldDeletedTasks(thirtyDaysAgo)

            // 2. Check current time for morning / evening digest
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            val activeTasks = dao.getActiveTasks().firstOrNull() ?: emptyList()
            val now = System.currentTimeMillis()

            val overdueCount = activeTasks.count { it.completionDateTime < now && !it.status.isFinished }

            val startOfToday = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfToday = startOfToday + 86400_000L
            val todayTasks = activeTasks.filter { it.completionDateTime in startOfToday until endOfToday }
            val todayCount = todayTasks.size

            if (hour in 6..11) {
                // Morning window: trigger morning digest
                notificationHelper.showMorningDigest(todayCount, overdueCount)
            } else if (hour in 18..23) {
                // Evening window: trigger evening review for unfinished
                val unfinishedToday = todayTasks.count { !it.status.isFinished }
                if (unfinishedToday > 0) {
                    notificationHelper.showEveningReview(unfinishedToday)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "taskpulse_daily_digest_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(
                1, TimeUnit.HOURS // checks hourly to fire in user-configured morning and evening slots
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
