package com.xiaozhao.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaozhao.assistant.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'TODAY'
          AND (due_time IS NULL OR due_time >= :dayStart)
          AND (due_time IS NULL OR due_time < :dayEnd)
        ORDER BY due_time ASC, created_at DESC
    """)
    fun getTodayTasks(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'TODAY'
          AND due_time IS NOT NULL
          AND due_time < :now
        ORDER BY due_time ASC
    """)
    fun getOverdueTasks(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTasks(limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("UPDATE tasks SET status = 'DONE', completed_at = :time WHERE id = :id")
    suspend fun markDone(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'TODAY' WHERE id = :id")
    suspend fun confirmTask(id: Long)

    @Query("UPDATE tasks SET due_time = :dueTime WHERE id = :id")
    suspend fun updateDueTime(id: Long, dueTime: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'TODAY'")
    fun todayCount(): Flow<Int>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    suspend fun getAllOnce(): List<TaskEntity>
}
