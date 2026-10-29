package com.example.data

import com.example.engine.RecurrenceEngine
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.RecurrenceType
import com.example.model.TaskItem
import com.example.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(
    private val taskDao: TaskDao,
    private val recurrenceEngine: RecurrenceEngine = RecurrenceEngine()
) {
    val activeTasks: Flow<List<TaskItem>> = taskDao.getActiveTasks()
    val deletedTasks: Flow<List<TaskItem>> = taskDao.getDeletedTasks()
    val archivedTasks: Flow<List<TaskItem>> = taskDao.getArchivedTasks()
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()
    val distinctOrigins: Flow<List<String>> = taskDao.getDistinctOrigins()
    val distinctCategories: Flow<List<String>> = taskDao.getDistinctCategories()

    suspend fun getTaskById(id: Long): TaskItem? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Long): Flow<TaskItem?> = taskDao.getTaskByIdFlow(id)

    suspend fun insertTask(task: TaskItem): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskItem) = taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun completeTask(task: TaskItem): TaskItem? {
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(completedTask)

        // Handle recurrence: generate the next occurrence if recurring
        if (task.recurrence != RecurrenceType.NONE) {
            val nextTask = recurrenceEngine.generateNextOccurrence(task)
            if (nextTask != null) {
                val newId = taskDao.insertTask(nextTask)
                return nextTask.copy(id = newId)
            }
        }
        return null
    }

    suspend fun snoozeTask(task: TaskItem, snoozeDurationMillis: Long) {
        val updatedDeadline = System.currentTimeMillis() + snoozeDurationMillis
        val snoozed = task.copy(
            completionDateTime = updatedDeadline,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(snoozed)
    }

    suspend fun moveToTrash(task: TaskItem) {
        val trashed = task.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(trashed)
    }

    suspend fun restoreFromTrash(task: TaskItem) {
        val restored = task.copy(
            isDeleted = false,
            deletedAt = null,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(restored)
    }

    suspend fun archiveTask(task: TaskItem) {
        val archived = task.copy(
            isArchived = true,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(archived)
    }

    suspend fun unarchiveTask(task: TaskItem) {
        val unarchived = task.copy(
            isArchived = false,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(unarchived)
    }

    suspend fun permanentlyDelete(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun emptyTrash(): Int {
        return taskDao.emptyTrash()
    }

    suspend fun bulkComplete(tasks: List<TaskItem>) {
        tasks.forEach { completeTask(it) }
    }

    suspend fun bulkDelete(tasks: List<TaskItem>) {
        tasks.forEach { moveToTrash(it) }
    }

    suspend fun bulkUpdateCriticality(tasks: List<TaskItem>, criticality: Criticality) {
        tasks.forEach { task ->
            taskDao.updateTask(task.copy(criticality = criticality, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun bulkUpdateDeadline(tasks: List<TaskItem>, newDeadline: Long) {
        tasks.forEach { task ->
            taskDao.updateTask(task.copy(completionDateTime = newDeadline, updatedAt = System.currentTimeMillis()))
        }
    }

    // Backup & Restore: Export database to JSON
    suspend fun exportToJson(tasks: List<TaskItem>): String {
        val array = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("criticality", t.criticality.name)
            obj.put("actionNeeded", t.actionNeeded.name)
            obj.put("taskOriginatedFrom", t.taskOriginatedFrom)
            obj.put("category", t.category)
            obj.put("completionDateTime", t.completionDateTime)
            obj.put("status", t.status.name)
            obj.put("assignedToName", t.assignedToName ?: "")
            obj.put("assignedToContact", t.assignedToContact ?: "")
            obj.put("followUpDate", t.followUpDate ?: 0L)
            obj.put("isPinned", t.isPinned)
            obj.put("recurrence", t.recurrence.name)
            obj.put("estimatedEffortMinutes", t.estimatedEffortMinutes ?: 0)
            obj.put("createdAt", t.createdAt)
            array.put(obj)
        }
        return array.toString(2)
    }

    // Import from JSON
    suspend fun importFromJson(jsonString: String): Int {
        val array = JSONArray(jsonString)
        val importedTasks = mutableListOf<TaskItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "")
            if (title.isNotBlank()) {
                val criticality = try {
                    Criticality.valueOf(obj.optString("criticality", Criticality.MEDIUM.name))
                } catch (e: Exception) {
                    Criticality.MEDIUM
                }
                val actionNeeded = try {
                    ActionNeeded.valueOf(obj.optString("actionNeeded", ActionNeeded.TO_ACT_MYSELF.name))
                } catch (e: Exception) {
                    ActionNeeded.TO_ACT_MYSELF
                }
                val status = try {
                    TaskStatus.valueOf(obj.optString("status", TaskStatus.NOT_STARTED.name))
                } catch (e: Exception) {
                    TaskStatus.NOT_STARTED
                }
                val recurrence = try {
                    RecurrenceType.valueOf(obj.optString("recurrence", RecurrenceType.NONE.name))
                } catch (e: Exception) {
                    RecurrenceType.NONE
                }
                importedTasks.add(
                    TaskItem(
                        title = title,
                        description = obj.optString("description", ""),
                        criticality = criticality,
                        actionNeeded = actionNeeded,
                        taskOriginatedFrom = obj.optString("taskOriginatedFrom", "Self"),
                        category = obj.optString("category", "Official"),
                        completionDateTime = obj.optLong("completionDateTime", System.currentTimeMillis() + 86400000L),
                        status = status,
                        assignedToName = obj.optString("assignedToName").takeIf { it.isNotBlank() },
                        assignedToContact = obj.optString("assignedToContact").takeIf { it.isNotBlank() },
                        followUpDate = obj.optLong("followUpDate").takeIf { it > 0 },
                        isPinned = obj.optBoolean("isPinned", false),
                        recurrence = recurrence,
                        estimatedEffortMinutes = obj.optInt("estimatedEffortMinutes").takeIf { it > 0 }
                    )
                )
            }
        }
        if (importedTasks.isNotEmpty()) {
            taskDao.insertTasks(importedTasks)
        }
        return importedTasks.size
    }
}
