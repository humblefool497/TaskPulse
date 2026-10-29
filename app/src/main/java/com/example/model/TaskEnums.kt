package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Criticality(val label: String, val level: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1);

    fun getColor(isDark: Boolean = false): Color {
        return when (this) {
            CRITICAL -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            HIGH -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
            MEDIUM -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            LOW -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
        }
    }

    fun getContainerColor(isDark: Boolean = false): Color {
        return when (this) {
            CRITICAL -> if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
            HIGH -> if (isDark) Color(0xFF431407) else Color(0xFFFFEDD5)
            MEDIUM -> if (isDark) Color(0xFF172554) else Color(0xFFDBEAFE)
            LOW -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
        }
    }

    fun getIcon(): ImageVector {
        return when (this) {
            CRITICAL -> Icons.Filled.NotificationImportant
            HIGH -> Icons.Filled.PriorityHigh
            MEDIUM -> Icons.Filled.Flag
            LOW -> Icons.Filled.Bookmark
        }
    }
}

enum class ActionNeeded(val label: String) {
    IMMEDIATE_ATTENTION("Immediate Attention / Completion Needed"),
    TO_ACT_MYSELF("To Act Myself"),
    FOLLOW_UP_NEEDED("Follow-up Needed"),
    ASSIGN_TO_OTHERS("To Assign to Someone Else");

    fun getShortLabel(): String {
        return when (this) {
            IMMEDIATE_ATTENTION -> "Immediate"
            TO_ACT_MYSELF -> "Self"
            FOLLOW_UP_NEEDED -> "Follow-up"
            ASSIGN_TO_OTHERS -> "Assigned"
        }
    }
}

enum class TaskStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    WAITING_ON_SOMEONE("Waiting on Someone"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    val isFinished: Boolean
        get() = this == COMPLETED || this == CANCELLED
}

enum class RecurrenceType(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM_INTERVAL("Custom Interval");
}

data class ReminderOffset(
    val id: String,
    val label: String,
    val minutesBefore: Long
)

data class Subtask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

data class AttachmentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val uriOrUrl: String,
    val type: String // URL, Image, PDF, File
)

data class ProgressNote(
    val timestamp: Long = System.currentTimeMillis(),
    val note: String
)

enum class DateBucket(val title: String) {
    OVERDUE("Overdue"),
    TODAY("Due Today"),
    THIS_WEEK("Due This Week"),
    NEXT_WEEK("Due Next Week"),
    NEXT_N_DAYS("Next 30 Days"),
    PENDING_FOLLOW_UP("Pending Follow-ups"),
    SOMEDAY("Later / No Deadline");
}
