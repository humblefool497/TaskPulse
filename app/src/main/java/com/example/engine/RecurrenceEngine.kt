package com.example.engine

import com.example.model.RecurrenceType
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskStatus
import java.util.Calendar

class RecurrenceEngine {

    fun calculateNextDeadline(currentDeadline: Long, recurrence: RecurrenceType, customDays: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDeadline
        }

        when (recurrence) {
            RecurrenceType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            RecurrenceType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
            }
            RecurrenceType.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
            }
            RecurrenceType.CUSTOM_INTERVAL -> {
                val step = if (customDays > 0) customDays else 1
                calendar.add(Calendar.DAY_OF_YEAR, step)
            }
            RecurrenceType.NONE -> {
                return currentDeadline
            }
        }

        // If the calculated deadline is still in the past, roll it forward until it's in the future
        val now = System.currentTimeMillis()
        while (calendar.timeInMillis <= now) {
            when (recurrence) {
                RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RecurrenceType.YEARLY -> calendar.add(Calendar.YEAR, 1)
                RecurrenceType.CUSTOM_INTERVAL -> calendar.add(Calendar.DAY_OF_YEAR, if (customDays > 0) customDays else 1)
                RecurrenceType.NONE -> break
            }
        }

        return calendar.timeInMillis
    }

    fun generateNextOccurrence(task: TaskItem): TaskItem? {
        if (task.recurrence == RecurrenceType.NONE) return null

        val nextDeadline = calculateNextDeadline(task.completionDateTime, task.recurrence, task.recurrenceCustomDays)

        // Reset subtasks for the new occurrence
        val resetSubtasks = task.subtasks.map { Subtask(title = it.title, isCompleted = false) }

        return task.copy(
            id = 0,
            completionDateTime = nextDeadline,
            status = TaskStatus.NOT_STARTED,
            subtasks = resetSubtasks,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = null,
            calendarEventId = null
        )
    }
}
