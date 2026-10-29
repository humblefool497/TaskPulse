package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.data.TaskPulseDatabase
import com.example.data.TaskRepository
import com.example.data.UserPreferencesRepository
import com.example.engine.CalendarSyncManager
import com.example.engine.NaturalLanguageTaskParser
import com.example.engine.RecurrenceEngine
import com.example.engine.SheetsExportManager
import com.example.engine.TaskDuplicateDetector
import com.example.notification.NotificationHelper
import com.example.notification.TaskPulseAlarmScheduler
import com.example.worker.DailyDigestWorker

class TaskPulseApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    lateinit var database: TaskPulseDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var alarmScheduler: TaskPulseAlarmScheduler
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var recurrenceEngine: RecurrenceEngine
        private set

    lateinit var naturalLanguageTaskParser: NaturalLanguageTaskParser
        private set

    lateinit var duplicateDetector: TaskDuplicateDetector
        private set

    lateinit var sheetsExportManager: SheetsExportManager
        private set

    lateinit var calendarSyncManager: CalendarSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = TaskPulseDatabase.getDatabase(this)
        recurrenceEngine = RecurrenceEngine()
        taskRepository = TaskRepository(database.taskDao(), recurrenceEngine)
        userPreferencesRepository = UserPreferencesRepository(this)
        alarmScheduler = TaskPulseAlarmScheduler(this)
        notificationHelper = NotificationHelper(this)
        naturalLanguageTaskParser = NaturalLanguageTaskParser()
        duplicateDetector = TaskDuplicateDetector()
        sheetsExportManager = SheetsExportManager()
        calendarSyncManager = CalendarSyncManager(this)

        // Initialize channels and background worker
        notificationHelper.createNotificationChannels()
        try {
            DailyDigestWorker.schedule(this)
        } catch (t: Throwable) {
            // WorkManager may be uninitialized or unsupported in certain testing environments
            t.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: TaskPulseApplication
            private set
    }
}
