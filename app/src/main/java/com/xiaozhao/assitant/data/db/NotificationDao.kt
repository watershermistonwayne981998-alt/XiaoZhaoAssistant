package com.xiaozhao.assitant.data.db

import androidx.room.*
import com.xiaozhao.assitant.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 通知记录 DAO
 */
@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity): Long

    @Update
    suspend fun update(entity: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT * FROM notifications ORDER BY post_time DESC")
    fun all(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE package_name = :pkg ORDER BY post_time DESC")
    fun byPackage(pkg: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun byId(id: Long): NotificationEntity?

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun unreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_important = 1")
    fun importantCount(): Flow<Int>

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET is_read = 1")
    suspend fun markAllRead()

    @Query("UPDATE notifications SET is_converted_to_task = 1 WHERE id = :id")
    suspend fun markConverted(id: Long)

    @Query("UPDATE notifications SET is_important = 1 WHERE id = :id")
    suspend fun markImportant(id: Long)

    @Query("SELECT * FROM notifications WHERE title LIKE :keyword OR text LIKE :keyword OR big_text LIKE :keyword ORDER BY post_time DESC")
    fun search(keyword: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int
}
