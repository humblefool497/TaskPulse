package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String, // required, max 120 chars
    val description: String = "", // multi-line, optional
    val criticality: Criticality = Criticality.MEDIUM,
    val actionNeeded: ActionNeeded = ActionNeeded.TO_ACT_MYSELF,
    val taskOriginatedFrom: String = "Self", // Boss, Client, Meeting, Email, Family, etc.
    val category: String = "Official", // Official, Personal, Finance/Bills, Health, etc.
    val completionDateTime: Long, // deadline, required, epoch millis
    val reminderOffsetsMinutes: List<Long> = listOf(0L), // at deadline (0), 15m, 60m, 1440m (1d), etc.
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val assignedToName: String? = null,
    val assignedToContact: String? = null,
    val followUpDate: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<String> = emptyList(),
    val attachments: List<AttachmentItem> = emptyList(),
    val notes: List<ProgressNote> = emptyList(),
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceCustomDays: Int = 0, // e.g. every N days or day of month
    val estimatedEffortMinutes: Int? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val addToCalendar: Boolean = false,
    val calendarEventId: String? = null
) {
    val isOverdue: Boolean
        get() = !status.isFinished && completionDateTime < System.currentTimeMillis()

    val subtasksProgress: Float
        get() = if (subtasks.isEmpty()) 0f else subtasks.count { it.isCompleted }.toFloat() / subtasks.size

    val completedSubtaskCount: Int
        get() = subtasks.count { it.isCompleted }

    val totalSubtaskCount: Int
        get() = subtasks.size
}
