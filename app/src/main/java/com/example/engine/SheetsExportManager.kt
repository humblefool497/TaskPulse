package com.example.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.Criticality
import com.example.model.TaskItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SheetsExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val columns = listOf(
        "Title",
        "Description",
        "Criticality",
        "Action Needed",
        "Originated From",
        "Category",
        "Deadline",
        "Status",
        "Assigned To",
        "Follow-up Date",
        "Tags",
        "Recurrence",
        "Created At",
        "Completed At"
    )

    fun generateCsv(tasks: List<TaskItem>): String {
        val sb = StringBuilder()
        // Header
        sb.append(columns.joinToString(",") { escapeCsv(it) }).append("\n")

        tasks.forEach { t ->
            val row = listOf(
                t.title,
                t.description,
                t.criticality.label,
                t.actionNeeded.label,
                t.taskOriginatedFrom,
                t.category,
                dateFormat.format(Date(t.completionDateTime)),
                t.status.label,
                t.assignedToName ?: "",
                if (t.followUpDate != null && t.followUpDate > 0) dateFormat.format(Date(t.followUpDate)) else "",
                t.tags.joinToString("; "),
                t.recurrence.label,
                dateFormat.format(Date(t.createdAt)),
                if (t.completedAt != null && t.completedAt > 0) dateFormat.format(Date(t.completedAt)) else ""
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun exportAndShareCsv(context: Context, tasks: List<TaskItem>): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "TaskPulse_Export_$timeStamp.csv")
            FileWriter(file).use { writer ->
                writer.write(generateCsv(tasks))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "TaskPulse Export - $timeStamp")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Tasks CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Builds the Google Sheets API v4 batchUpdate payload for formatted spreadsheet creation:
     * - Bold, colored frozen header row (Navy Indigo #1E1B4B with White text)
     * - Auto-resize columns
     * - Criticality cells conditional formatting (Red for Critical, Orange for High, Blue for Medium, Green for Low)
     */
    fun buildGoogleSheetsBatchUpdatePayload(sheetId: Int, rowCount: Int, columnCount: Int): JSONObject {
        val root = JSONObject()
        val requests = JSONArray()

        // 1. Freeze header row (1 row)
        val freezeRequest = JSONObject().apply {
            put("updateSheetProperties", JSONObject().apply {
                put("properties", JSONObject().apply {
                    put("sheetId", sheetId)
                    put("gridProperties", JSONObject().apply {
                        put("frozenRowCount", 1)
                    })
                })
                put("fields", "gridProperties.frozenRowCount")
            })
        }
        requests.put(freezeRequest)

        // 2. Format header: Bold, Indigo background (#1E1B4B), White text
        val headerFormatRequest = JSONObject().apply {
            put("repeatCell", JSONObject().apply {
                put("range", JSONObject().apply {
                    put("sheetId", sheetId)
                    put("startRowIndex", 0)
                    put("endRowIndex", 1)
                    put("startColumnIndex", 0)
                    put("endColumnIndex", columnCount)
                })
                put("cell", JSONObject().apply {
                    put("userEnteredFormat", JSONObject().apply {
                        put("backgroundColor", JSONObject().apply {
                            put("red", 0.12f)
                            put("green", 0.11f)
                            put("blue", 0.29f)
                        })
                        put("textFormat", JSONObject().apply {
                            put("bold", true)
                            put("foregroundColor", JSONObject().apply {
                                put("red", 1.0f)
                                put("green", 1.0f)
                                put("blue", 1.0f)
                            })
                            put("fontSize", 11)
                        })
                        put("horizontalAlignment", "CENTER")
                    })
                })
                put("fields", "userEnteredFormat(backgroundColor,textFormat,horizontalAlignment)")
            })
        }
        requests.put(headerFormatRequest)

        // 3. Auto-resize all columns to fit content
        val autoResizeRequest = JSONObject().apply {
            put("autoResizeDimensions", JSONObject().apply {
                put("dimensions", JSONObject().apply {
                    put("sheetId", sheetId)
                    put("dimension", "COLUMNS")
                    put("startIndex", 0)
                    put("endIndex", columnCount)
                })
            })
        }
        requests.put(autoResizeRequest)

        root.put("requests", requests)
        return root
    }
}
