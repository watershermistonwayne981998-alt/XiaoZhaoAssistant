package com.xiaozhao.assistant.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 关键词规则实体
 * type: TIME(时间词) / ACTION(动作词) / WORK(工作词) / URGENT(紧急词)
 */
@Entity(tableName = "keyword_rules")
data class KeywordRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val keyword: String,

    /** TIME / ACTION / WORK / URGENT */
    val type: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true
) {
    companion object {
        const val TYPE_TIME = "TIME"
        const val TYPE_ACTION = "ACTION"
        const val TYPE_WORK = "WORK"
        const val TYPE_URGENT = "URGENT"
    }
}
