package com.example.engine

import com.example.model.ActionNeeded
import com.example.model.Criticality
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTaskDraft(
    val title: String,
    val criticality: Criticality = Criticality.MEDIUM,
    val actionNeeded: ActionNeeded = ActionNeeded.TO_ACT_MYSELF,
    val category: String = "Official",
    val originatedFrom: String = "Self",
    val deadlineMillis: Long
)

class NaturalLanguageTaskParser {

    fun parse(input: String): ParsedTaskDraft {
        var text = input.trim()
        if (text.isEmpty()) {
            return ParsedTaskDraft(
                title = "New Task",
                deadlineMillis = defaultDeadlineMillis()
            )
        }

        var detectedCriticality = Criticality.MEDIUM
        var detectedAction = ActionNeeded.TO_ACT_MYSELF
        var detectedCategory = "Official"
        var detectedOrigin = "Self"

        // 1. Criticality detection
        val lower = text.lowercase(Locale.ROOT)
        when {
            lower.contains(Regex("\\b(critical|urgent|asap|p0)\\b")) -> {
                detectedCriticality = Criticality.CRITICAL
                text = text.replace(Regex("(?i)\\b(critical|urgent|asap|p0)\\b"), "")
            }
            lower.contains(Regex("\\b(high|important|p1)\\b")) -> {
                detectedCriticality = Criticality.HIGH
                text = text.replace(Regex("(?i)\\b(high|important|p1)\\b"), "")
            }
            lower.contains(Regex("\\b(low|minor|p3)\\b")) -> {
                detectedCriticality = Criticality.LOW
                text = text.replace(Regex("(?i)\\b(low|minor|p3)\\b"), "")
            }
            lower.contains(Regex("\\b(medium|normal|p2)\\b")) -> {
                detectedCriticality = Criticality.MEDIUM
                text = text.replace(Regex("(?i)\\b(medium|normal|p2)\\b"), "")
            }
        }

        // 2. Action Needed detection
        when {
            lower.contains(Regex("\\b(immediate|immediate attention)\\b")) -> {
                detectedAction = ActionNeeded.IMMEDIATE_ATTENTION
                text = text.replace(Regex("(?i)\\b(immediate|immediate attention)\\b"), "")
            }
            lower.contains(Regex("\\b(follow-up|followup|follow up)\\b")) -> {
                detectedAction = ActionNeeded.FOLLOW_UP_NEEDED
                text = text.replace(Regex("(?i)\\b(follow-up|followup|follow up)\\b"), "")
            }
            lower.contains(Regex("\\b(assign|delegate)\\b")) -> {
                detectedAction = ActionNeeded.ASSIGN_TO_OTHERS
                text = text.replace(Regex("(?i)\\b(assign|delegate)\\b"), "")
            }
        }

        // 3. Category detection
        when {
            lower.contains(Regex("\\b(bill|finance|tax|payment|invoice|bank)\\b")) -> {
                detectedCategory = "Finance/Bills"
            }
            lower.contains(Regex("\\b(health|doctor|workout|gym|medicine|clinic)\\b")) -> {
                detectedCategory = "Health"
            }
            lower.contains(Regex("\\b(meeting|discussion|sync|standup|call)\\b")) -> {
                detectedCategory = "Meetings"
            }
            lower.contains(Regex("\\b(family|kids|school|parent)\\b")) -> {
                detectedCategory = "Family"
            }
            lower.contains(Regex("\\b(personal|home|groceries|shopping|errand)\\b")) -> {
                detectedCategory = "Personal"
            }
        }

        // 4. Origin detection
        when {
            lower.contains(Regex("\\b(boss|manager|director)\\b")) -> {
                detectedOrigin = "Boss"
            }
            lower.contains(Regex("\\b(client|customer)\\b")) -> {
                detectedOrigin = "Client"
            }
            lower.contains(Regex("\\b(email|inbox)\\b")) -> {
                detectedOrigin = "Email"
            }
            lower.contains(Regex("\\b(vendor|supplier)\\b")) -> {
                detectedOrigin = "Vendor"
            }
        }

        // 5. Date & Time parsing
        val calendar = Calendar.getInstance()
        var dateAdjusted = false

        if (lower.contains(Regex("\\btomorrow\\b"))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateAdjusted = true
            text = text.replace(Regex("(?i)\\btomorrow\\b"), "")
        } else if (lower.contains(Regex("\\bday after tomorrow\\b"))) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            dateAdjusted = true
            text = text.replace(Regex("(?i)\\bday after tomorrow\\b"), "")
        } else if (lower.contains(Regex("\\bnext week\\b"))) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            dateAdjusted = true
            text = text.replace(Regex("(?i)\\bnext week\\b"), "")
        } else if (lower.contains(Regex("\\btoday\\b|\\btonight\\b"))) {
            dateAdjusted = true
            text = text.replace(Regex("(?i)\\b(today|tonight)\\b"), "")
        }

        // Parse time pattern (e.g. 5pm, 5:30pm, 10am, 17:00)
        val timePattern = Pattern.compile("(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b")
        val matcher = timePattern.matcher(text)
        var timeFound = false

        if (matcher.find()) {
            val hourStr = matcher.group(1)
            val minStr = matcher.group(2)
            val ampm = matcher.group(3)

            var hour = hourStr?.toIntOrNull() ?: 17
            val min = minStr?.toIntOrNull() ?: 0

            if (ampm != null) {
                if (ampm.equals("pm", ignoreCase = true) && hour < 12) {
                    hour += 12
                } else if (ampm.equals("am", ignoreCase = true) && hour == 12) {
                    hour = 0
                }
            } else if (hour <= 7) {
                // heuristic: 1 to 7 without am/pm likely means afternoon/evening
                hour += 12
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, min)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            timeFound = true
            text = text.replaceRange(matcher.start(), matcher.end(), "")
        }

        if (!timeFound) {
            // Default to 5:00 PM if time not stated
            if (!dateAdjusted) {
                // If neither date nor time was mentioned, default to 3 hours from now
                calendar.add(Calendar.HOUR_OF_DAY, 3)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 17)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        // Clean up remaining text to form the title
        val cleanTitle = text
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(Regex("^[\\s,\\.-]+|[\\s,\\.-]+$"), "")
            .capitalizeFirstLetter()

        return ParsedTaskDraft(
            title = if (cleanTitle.isNotBlank()) cleanTitle else "Task on ${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}",
            criticality = detectedCriticality,
            actionNeeded = detectedAction,
            category = detectedCategory,
            originatedFrom = detectedOrigin,
            deadlineMillis = calendar.timeInMillis
        )
    }

    private fun defaultDeadlineMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 4)
        return cal.timeInMillis
    }

    private fun String.capitalizeFirstLetter(): String {
        return if (isNotEmpty()) this.substring(0, 1).uppercase(Locale.ROOT) + this.substring(1) else this
    }
}
