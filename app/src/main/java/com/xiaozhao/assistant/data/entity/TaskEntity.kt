package com.xiaozhao.assistant.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办任务实体
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    @ColumnInfo(name = "source_notification_id")
    val sourceNotificationId: Long? = null,

    @ColumnInfo(name = "source_app")
    val sourceApp: String? = null,

    @ColumnInfo(name = "original_text")
    val originalText: String? = null,

    @ColumnInfo(name = "due_time")
    val dueTime: Long? = null,

    /** 1=普通, 2=重要 */
    val priority: Int = 1,

    /**
     * PENDING(待确认) / TODAY(今日待办) / DONE(已完成)
     * PENDING: 规则引擎自动生成的候选待办，需用户确认
     * TODAY: 用户确认后或已到今日的待办
     * DONE: 已完成
     */
    val status: String = STATUS_PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_TODAY = "TODAY"
        const val STATUS_DONE = "DONE"
    }
}
