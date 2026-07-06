package com.xiaozhao.assistant.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App 白名单实体
 * 只有 enabled=true 的 App 的通知才会被采集
 */
@Entity(tableName = "app_whitelist")
data class AppWhitelistEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = false
)
