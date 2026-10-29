package com.example

import com.example.engine.NaturalLanguageTaskParser
import com.example.engine.RecurrenceEngine
import com.example.engine.TaskDuplicateDetector
import com.example.model.ActionNeeded
import com.example.model.Criticality
import com.example.model.DateBucket
import com.example.model.RecurrenceType
import com.example.model.Subtask
import com.example.model.TaskItem
import com.example.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TaskPulseUnitTest {

    private val recurrenceEngine = RecurrenceEngine()
    private val nlpParser = NaturalLanguageTaskParser()
    private val duplicateDetector = TaskDuplicateDetector()

    @Test
    fun testRecurrenceDailyAdvancesByOneDay() {
        val now = System.currentTimeMillis()
        val next = recurrenceEngine.calculateNextDeadline(now, RecurrenceType.DAILY)
        val diff = next - now
        // Approximately 24 hours (86,400,000 ms)
        assertTrue(diff >= 86_400_000L - 1000L)
    }

    @Test
    fun testRecurrenceGeneratesNewTaskWithResetSubtasks() {
        val originalTask = TaskItem(
            id = 42,
            title = "Weekly Status Report",
            completionDateTime = System.currentTimeMillis(),
            recurrence = RecurrenceType.WEEKLY,
            subtasks = listOf(
                Subtask(title = "Gather metrics", isCompleted = true),
                Subtask(title = "Send to leadership", isCompleted = true)
            )
        )

        val nextTask = recurrenceEngine.generateNextOccurrence(originalTask)
        assertNotNull(nextTask)
        assertEquals(0L, nextTask!!.id)
        assertEquals("Weekly Status Report", nextTask.title)
        assertEquals(TaskStatus.NOT_STARTED, nextTask.status)
        // Subtasks should be reset to isCompleted = false
        assertEquals(2, nextTask.subtasks.size)
        assertFalse(nextTask.subtasks[0].isCompleted)
        assertFalse(nextTask.subtasks[1].isCompleted)
    }

    @Test
    fun testNaturalLanguageParsingHighPriorityAndFollowUp() {
        val input = "Call vendor tomorrow 5pm high follow-up"
        val parsed = nlpParser.parse(input)

        assertEquals(Criticality.HIGH, parsed.criticality)
        assertEquals(ActionNeeded.FOLLOW_UP_NEEDED, parsed.actionNeeded)
        assertTrue(parsed.title.contains("Call vendor", ignoreCase = true))

        val cal = Calendar.getInstance().apply { timeInMillis = parsed.deadlineMillis }
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testNaturalLanguageParsingCriticalFinanceTask() {
        val input = "Pay electric bill today critical"
        val parsed = nlpParser.parse(input)

        assertEquals(Criticality.CRITICAL, parsed.criticality)
        assertEquals("Finance/Bills", parsed.category)
    }

    @Test
    fun testDuplicateDetectionCatchesSimilarTitles() {
        val activeTasks = listOf(
            TaskItem(
                id = 1,
                title = "Pay electric utility bill",
                completionDateTime = System.currentTimeMillis()
            ),
            TaskItem(
                id = 2,
                title = "Call dental clinic",
                completionDateTime = System.currentTimeMillis()
            )
        )

        val duplicates = duplicateDetector.findPotentialDuplicates(
            candidateTitle = "Pay electricity utility bills",
            activeTasks = activeTasks
        )

        assertEquals(1, duplicates.size)
        assertEquals(1L, duplicates[0].id)
    }

    @Test
    fun testDateBucketingLogic() {
        val now = System.currentTimeMillis()
        val overdueTask = TaskItem(
            id = 1,
            title = "Overdue task",
            completionDateTime = now - 3600_000L,
            status = TaskStatus.NOT_STARTED
        )
        assertTrue(overdueTask.isOverdue)

        val completedTask = TaskItem(
            id = 2,
            title = "Done task",
            completionDateTime = now - 3600_000L,
            status = TaskStatus.COMPLETED
        )
        assertFalse(completedTask.isOverdue)
    }
}
