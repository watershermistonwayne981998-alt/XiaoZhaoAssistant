package com.xiaozhao.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xiaozhao.assistant.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity): Long

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY post_time DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE package_name = :pkg ORDER BY post_time DESC")
    fun getByPackage(pkg: String): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM notifications
        WHERE title LIKE '%' || :keyword || '%'
           OR text LIKE '%' || :keyword || '%'
           OR big_text LIKE '%' || :keyword || '%'
        ORDER BY post_time DESC
    """)
    fun search(keyword: String): Flow<List<NotificationEntity>>

    @Query("SELECT DISTINCT package_name, app_name FROM notifications")
    fun getDistinctApps(): Flow<List<AppPackageInfo>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET is_converted_to_task = 1 WHERE id = :id")
    suspend fun markConverted(id: Long)

    @Query("UPDATE notifications SET is_important = 1 WHERE id = :id")
    suspend fun markImportant(id: Long)

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun unreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_important = 1 AND is_read = 0")
    fun importantUnreadCount(): Flow<Int>

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int
}

data class AppPackageInfo(
    val package_name: String,
    val app_name: String
)
