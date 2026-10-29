package com.example.data

import androidx.room.TypeConverter
import com.example.model.AttachmentItem
import com.example.model.ProgressNote
import com.example.model.Subtask
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromLongList(list: List<Long>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toLongList(data: String?): List<Long> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<Long>()
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            list.add(array.getLong(i))
        }
        return list
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromSubtaskList(subtasks: List<Subtask>?): String {
        if (subtasks == null) return "[]"
        val array = JSONArray()
        subtasks.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("isCompleted", it.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubtaskList(data: String?): List<Subtask> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<Subtask>()
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Subtask(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            )
        }
        return list
    }

    @TypeConverter
    fun fromAttachmentList(attachments: List<AttachmentItem>?): String {
        if (attachments == null) return "[]"
        val array = JSONArray()
        attachments.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("uriOrUrl", it.uriOrUrl)
            obj.put("type", it.type)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toAttachmentList(data: String?): List<AttachmentItem> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<AttachmentItem>()
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AttachmentItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    uriOrUrl = obj.optString("uriOrUrl", ""),
                    type = obj.optString("type", "URL")
                )
            )
        }
        return list
    }

    @TypeConverter
    fun fromProgressNoteList(notes: List<ProgressNote>?): String {
        if (notes == null) return "[]"
        val array = JSONArray()
        notes.forEach {
            val obj = JSONObject()
            obj.put("timestamp", it.timestamp)
            obj.put("note", it.note)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toProgressNoteList(data: String?): List<ProgressNote> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<ProgressNote>()
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ProgressNote(
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
            )
        }
        return list
    }
}
