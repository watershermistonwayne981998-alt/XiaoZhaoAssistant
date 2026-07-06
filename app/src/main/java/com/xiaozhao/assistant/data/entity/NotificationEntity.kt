package com.xiaozhao.assistant.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知记录实体
 * 去重键: packageName + notificationId + postTime + title + text
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["package_name", "notification_id", "post_time", "title", "text"], unique = true),
        Index(value = ["post_time"]),
        Index(value = ["package_name"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    val title: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "big_text")
    val bigText: String? = null,

    @ColumnInfo(name = "post_time")
    val postTime: Long,

    @ColumnInfo(name = "notification_key")
    val notificationKey: String,

    @ColumnInfo(name = "notification_id")
    val notificationId: Int,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "is_converted_to_task")
    val isConvertedToTask: Boolean = false,

    @ColumnInfo(name = "is_important")
    val isImportant: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // 钉钉消息解析字段
    @ColumnInfo(name = "sender")
    val sender: String? = null,

    @ColumnInfo(name = "conversation_name")
    val conversationName: String? = null,

    @ColumnInfo(name = "message_type")
    val messageType: String? = null,

    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    @ColumnInfo(name = "at_all")
    val atAll: Boolean = false,

    @ColumnInfo(name = "at_me")
    val atMe: Boolean = false,

    @ColumnInfo(name = "full_text")
    val fullText: String? = null
)
