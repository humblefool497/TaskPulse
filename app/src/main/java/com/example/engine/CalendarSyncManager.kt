package com.example.engine

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.model.TaskItem
import org.json.JSONObject
import java.util.Calendar

data class CalendarSyncResult(
    val success: Boolean,
    val message: String,
    val eventId: String? = null
)

class CalendarSyncManager(private val context: Context) {

    /**
     * Adds the task deadline as a calendar event using Android's native CalendarContract.
     * This immediately integrates with whatever calendar accounts (Google, Outlook, Exchange)
     * are logged into the user's Android device.
     */
    fun createDeviceCalendarEvent(task: TaskItem): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "[TaskPulse] ${task.title}")
                putExtra(CalendarContract.Events.DESCRIPTION, "${task.description}\n\nCriticality: ${task.criticality.label}\nCategory: ${task.category}\nAction: ${task.actionNeeded.label}")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.completionDateTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.completionDateTime + 3600_000L) // 1 hour event
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generates a Google Calendar API v3 Event resource JSON payload
     */
    fun buildGoogleCalendarEventPayload(task: TaskItem): JSONObject {
        val payload = JSONObject()
        payload.put("summary", "[TaskPulse] ${task.title}")
        payload.put("description", "Criticality: ${task.criticality.label}\nAction: ${task.actionNeeded.label}\n${task.description}")

        val start = JSONObject()
        start.put("dateTime", formatIso8601(task.completionDateTime))
        payload.put("start", start)

        val end = JSONObject()
        end.put("dateTime", formatIso8601(task.completionDateTime + 3600_000L))
        payload.put("end", end)

        // Color coding for Google Calendar (11 is red/tomato, 6 is orange, 9 is blue, 2 is green)
        val googleColorId = when (task.criticality) {
            com.example.model.Criticality.CRITICAL -> "11"
            com.example.model.Criticality.HIGH -> "6"
            com.example.model.Criticality.MEDIUM -> "9"
            com.example.model.Criticality.LOW -> "2"
        }
        payload.put("colorId", googleColorId)

        return payload
    }

    /**
     * Generates a Microsoft Graph API Calendar Event JSON payload for Outlook
     */
    fun buildOutlookEventPayload(task: TaskItem): JSONObject {
        val payload = JSONObject()
        payload.put("subject", "[TaskPulse] ${task.title}")

        val body = JSONObject()
        body.put("contentType", "Text")
        body.put("content", "Priority: ${task.criticality.label}\nCategory: ${task.category}\nAction: ${task.actionNeeded.label}\n\n${task.description}")
        payload.put("body", body)

        val start = JSONObject()
        start.put("dateTime", formatIso8601(task.completionDateTime))
        start.put("timeZone", "UTC")
        payload.put("start", start)

        val end = JSONObject()
        end.put("dateTime", formatIso8601(task.completionDateTime + 3600_000L))
        end.put("timeZone", "UTC")
        payload.put("end", end)

        val importance = when (task.criticality) {
            com.example.model.Criticality.CRITICAL,
            com.example.model.Criticality.HIGH -> "high"
            com.example.model.Criticality.MEDIUM -> "normal"
            com.example.model.Criticality.LOW -> "low"
        }
        payload.put("importance", importance)

        return payload
    }

    private fun formatIso8601(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(millis))
    }
}
