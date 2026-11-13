package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END, CASE WHEN priority = 'URGENT' THEN 1 WHEN priority = 'HIGH' THEN 2 WHEN priority = 'MEDIUM' THEN 3 ELSE 4 END, CASE WHEN dueDate > 0 THEN dueDate ELSE 9999999999999 END ASC, createdTimestamp DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' ORDER BY CASE WHEN priority = 'URGENT' THEN 1 WHEN priority = 'HIGH' THEN 2 WHEN priority = 'MEDIUM' THEN 3 ELSE 4 END, createdTimestamp DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED'")
    fun getPendingTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    fun getCompletedTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE priority = 'URGENT' AND status != 'COMPLETED'")
    fun getUrgentTaskCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE tasks SET status = :status, completedTimestamp = :completedTime WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedTime: Long)
}
