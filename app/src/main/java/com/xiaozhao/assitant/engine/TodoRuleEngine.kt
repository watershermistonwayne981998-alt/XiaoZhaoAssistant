package com.xiaozhao.assitant.engine

import com.xiaozhao.assitant.data.entity.KeywordRuleEntity
import com.xiaozhao.assitant.data.entity.TaskEntity
import java.util.*

/**
 * 待办规则引擎
 *
 * 识别逻辑：
 * 1. 如果通知内容同时包含动作词和工作词，则生成"候选待办"
 * 2. 如果同时包含时间词，则自动提取截止时间
 * 3. 如果包含紧急词，则标记为重要
 * 4. 候选待办默认状态为"待确认"，不要直接当成正式待办
 * 5. 钉钉消息使用 fullText（包含所有解析出的文本）进行匹配
 */
class TodoRuleEngine(private val keywords: List<KeywordRuleEntity>) {

    /**
     * 从通知内容生成候选待办
     *
     * @param title 通知标题
     * @param text 通知文本
     * @param bigText 通知大文本
     * @param sourceNotificationId 源通知ID
     * @param sourceApp 源应用名称
     * @param fullText 完整文本（钉钉解析后的所有文本，用于更准确匹配）
     * @return 候选待办，如果不满足条件则返回 null
     */
    fun generateCandidateTask(
        title: String,
        text: String,
        bigText: String?,
        sourceNotificationId: Long,
        sourceApp: String,
        fullText: String? = null
    ): TaskEntity? {
        // 使用 fullText（如果存在）或合并的普通文本进行匹配
        val searchText = buildString {
            appendLine(title)
            appendLine(text)
            if (!bigText.isNullOrEmpty() && bigText != text) {
                appendLine(bigText)
            }
            if (!fullText.isNullOrEmpty()) {
                appendLine(fullText)
            }
        }.lowercase()

        // 分类关键词
        val timeWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_TIME && it.enabled }
            .map { it.keyword.lowercase() }
        val actionWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_ACTION && it.enabled }
            .map { it.keyword.lowercase() }
        val workWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_WORK && it.enabled }
            .map { it.keyword.lowercase() }
        val urgentWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_URGENT && it.enabled }
            .map { it.keyword.lowercase() }

        // 检查是否包含动作词和工作词（必须同时满足）
        val hasAction = actionWords.any { searchText.contains(it) }
        val hasWork = workWords.any { searchText.contains(it) }

        if (!hasAction || !hasWork) {
            return null
        }

        // 提取截止时间
        var dueTime: Long? = null
        for (word in timeWords) {
            if (searchText.contains(word)) {
                dueTime = TimeParser.parseTimeWord(word, Date())
                if (dueTime != null) break
            }
        }

        // 判断优先级
        val hasUrgent = urgentWords.any { searchText.contains(it) }
        val priority = when {
            hasUrgent -> 2 // 重要
            dueTime != null -> 1 // 有截止时间
            else -> 0 // 普通
        }

        // 生成待办标题（使用标题或前50个字符）
        val taskTitle = if (title.isNotBlank()) {
            title.take(50)
        } else {
            text.take(50)
        }

        return TaskEntity(
            title = taskTitle,
            sourceNotificationId = sourceNotificationId,
            sourceApp = sourceApp,
            originalText = fullText ?: "$title $text",
            dueTime = dueTime,
            priority = priority,
            status = TaskEntity.STATUS_PENDING, // 待确认
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )
    }

    /**
     * 从钉钉消息生成候选待办（专用方法，使用 DingTalkMessage 对象）
     */
    fun generateCandidateFromDingTalk(dingTalkMsg: com.xiaozhao.assitant.service.DingTalkMessage): TaskEntity? {
        val searchText = dingTalkMsg.fullText.lowercase()

        // 分类关键词
        val timeWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_TIME && it.enabled }
            .map { it.keyword.lowercase() }
        val actionWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_ACTION && it.enabled }
            .map { it.keyword.lowercase() }
        val workWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_WORK && it.enabled }
            .map { it.keyword.lowercase() }
        val urgentWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_URGENT && it.enabled }
            .map { it.keyword.lowercase() }

        // 检查是否包含动作词和工作词（必须同时满足）
        val hasAction = actionWords.any { searchText.contains(it) }
        val hasWork = workWords.any { searchText.contains(it) }

        if (!hasAction || !hasWork) {
            return null
        }

        // 提取截止时间
        var dueTime: Long? = null
        for (word in timeWords) {
            if (searchText.contains(word)) {
                dueTime = TimeParser.parseTimeWord(word, Date())
                if (dueTime != null) break
            }
        }

        // 判断优先级（钉钉消息：@我或@所有人→重要）
        val hasUrgent = urgentWords.any { searchText.contains(it) } || dingTalkMsg.atMe || dingTalkMsg.atAll
        val priority = when {
            hasUrgent -> 2 // 重要
            dueTime != null -> 1 // 有截止时间
            else -> 0 // 普通
        }

        // 生成待办标题
        val taskTitle = if (dingTalkMsg.content.isNotBlank()) {
            dingTalkMsg.content.take(50)
        } else {
            dingTalkMsg.sender ?: "钉钉消息"
        }

        return TaskEntity(
            title = taskTitle,
            sourceNotificationId = dingTalkMsg.notificationId.toLong(),
            sourceApp = "钉钉",
            originalText = dingTalkMsg.fullText,
            dueTime = dueTime,
            priority = priority,
            status = TaskEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )
    }
}
