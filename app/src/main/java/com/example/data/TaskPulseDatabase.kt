package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.RecurrenceType
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TaskItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaskPulseDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskPulseDatabase? = null

        fun getDatabase(context: Context): TaskPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskPulseDatabase::class.java,
                    "taskpulse_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial high-value sample tasks so user immediately experiences the dashboard
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialTasks(database.taskDao())
                    }
                }
            }
        }

        private suspend fun populateInitialTasks(dao: TaskDao) {
            val now = System.currentTimeMillis()
            val hour = 3600_000L
            val day = 86400_000L

            val initialTasks = listOf(
                TaskItem(
                    title = "Review & approve Q4 strategic executive deck",
                    description = "Analyze revenue projections, staffing plan, and prepare talking points for tomorrow's board review.",
                    criticality = Criticality.CRITICAL,
                    actionNeeded = ActionNeeded.IMMEDIATE_ATTENTION,
                    taskOriginatedFrom = "Boss",
                    category = "Official",
                    completionDateTime = now + (3 * hour),
                    isPinned = true,
                    subtasks = listOf(
                        Subtask(title = "Check slide 4 financial summary", isCompleted = true),
                        Subtask(title = "Cross-reference headcount model", isCompleted = false),
                        Subtask(title = "Send sign-off confirmation", isCompleted = false)
                    ),
                    tags = listOf("Executive", "HighPriority", "Q4")
                ),
                TaskItem(
                    title = "Follow up with Acme Corp on pending enterprise contract",
                    description = "Verify legal review clauses with compliance team and call VP of Procurement.",
                    criticality = Criticality.HIGH,
                    actionNeeded = ActionNeeded.FOLLOW_UP_NEEDED,
                    taskOriginatedFrom = "Client",
                    category = "Official",
                    completionDateTime = now + (6 * hour),
                    followUpDate = now + (4 * hour),
                    subtasks = listOf(
                        Subtask(title = "Email legal team", isCompleted = true),
                        Subtask(title = "Phone call with Procurement", isCompleted = false)
                    ),
                    tags = listOf("Sales", "Legal")
                ),
                TaskItem(
                    title = "Pay quarterly advance tax & property utility bill",
                    description = "Download invoice statements and process clearance payment through direct banking portal.",
                    criticality = Criticality.HIGH,
                    actionNeeded = ActionNeeded.TO_ACT_MYSELF,
                    taskOriginatedFrom = "Email",
                    category = "Finance/Bills",
                    completionDateTime = now + (2 * day),
                    recurrence = RecurrenceType.MONTHLY,
                    tags = listOf("Tax", "Bills")
                ),
                TaskItem(
                    title = "Assign mobile UI QA audit to testing lead",
                    description = "Provide Figma design specs and test device matrix for the v2.4 build release.",
                    criticality = Criticality.MEDIUM,
                    actionNeeded = ActionNeeded.ASSIGN_TO_OTHERS,
                    taskOriginatedFrom = "Meeting",
                    category = "Official",
                    completionDateTime = now + (1 * day),
                    assignedToName = "Alex Rivera",
                    assignedToContact = "alex.rivera@enterprise.com",
                    tags = listOf("Release", "QA")
                ),
                TaskItem(
                    title = "Schedule annual health checkup & blood test",
                    description = "Book fasting appointment at central clinic for Saturday morning.",
                    criticality = Criticality.MEDIUM,
                    actionNeeded = ActionNeeded.TO_ACT_MYSELF,
                    taskOriginatedFrom = "Self",
                    category = "Health",
                    completionDateTime = now + (4 * day),
                    tags = listOf("Wellness")
                ),
                TaskItem(
                    title = "Old pending invoice verification",
                    description = "This was due yesterday from the supplier meeting.",
                    criticality = Criticality.CRITICAL,
                    actionNeeded = ActionNeeded.FOLLOW_UP_NEEDED,
                    taskOriginatedFrom = "Vendor",
                    category = "Finance/Bills",
                    completionDateTime = now - (14 * hour),
                    status = TaskStatus.NOT_STARTED,
                    tags = listOf("Overdue", "Urgent")
                )
            )
            dao.insertTasks(initialTasks)
        }
    }
}
