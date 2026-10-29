package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, completionDateTime ASC")
    fun getActiveTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks ORDER BY completionDateTime ASC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskItem?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<TaskItem?>

    @Query("SELECT DISTINCT taskOriginatedFrom FROM tasks WHERE isDeleted = 0 AND taskOriginatedFrom != ''")
    fun getDistinctOrigins(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM tasks WHERE isDeleted = 0 AND category != ''")
    fun getDistinctCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskItem>): List<Long>

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeOldDeletedTasks(cutoffMillis: Long): Int

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun emptyTrash(): Int
}
