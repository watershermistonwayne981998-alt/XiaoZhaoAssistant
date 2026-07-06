package com.xiaozhao.assistant.engine

import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.TaskEntity

/**
 * 待办识别规则引擎
 *
 * 规则：
 * 1. 内容同时包含「动作词」+「工作词」→ 生成候选待办
 * 2. 同时包含「时间词」→ 自动提取截止时间
 * 3. 包含「紧急词」→ 标记为重要
 * 4. 候选待办默认状态为 PENDING（待确认），不直接变成正式待办
 */
class TodoRuleEngine(
    private val keywords: List<KeywordRuleEntity>
) {
    data class MatchResult(
        val isCandidate: Boolean,
        val dueTime: Long?,
        val isImportant: Boolean,
        val matchedActions: List<String>,
        val matchedWork: List<String>,
        val matchedTimes: List<String>,
        val matchedUrgent: List<String>
    )

    private val timeWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_TIME && it.enabled }.map { it.keyword }
    private val actionWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_ACTION && it.enabled }.map { it.keyword }
    private val workWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_WORK && it.enabled }.map { it.keyword }
    private val urgentWords = keywords.filter { it.type == KeywordRuleEntity.TYPE_URGENT && it.enabled }.map { it.keyword }

    /**
     * 分析通知文本，返回匹配结果
     */
    fun analyze(title: String, text: String, bigText: String? = null): MatchResult {
        val fullText = buildString {
            append(title).append(" ")
            append(text)
            if (!bigText.isNullOrBlank()) append(" ").append(bigText)
        }.lowercase()

        val matchedActions = actionWords.filter { fullText.contains(it.lowercase()) }
        val matchedWork = workWords.filter { fullText.contains(it.lowercase()) }
        val matchedTimes = timeWords.filter { fullText.contains(it.lowercase()) }
        val matchedUrgent = urgentWords.filter { fullText.contains(it.lowercase()) }

        // 核心规则：同时包含动作词 + 工作词 → 候选待办
        val isCandidate = matchedActions.isNotEmpty() && matchedWork.isNotEmpty()

        // 提取截止时间
        val dueTime = if (matchedTimes.isNotEmpty()) {
            TimeParser.parseDueTime(fullText)
        } else null

        val isImportant = matchedUrgent.isNotEmpty()

        return MatchResult(
            isCandidate = isCandidate,
            dueTime = dueTime,
            isImportant = isImportant,
            matchedActions = matchedActions,
            matchedWork = matchedWork,
            matchedTimes = matchedTimes,
            matchedUrgent = matchedUrgent
        )
    }

    /**
     * 根据分析结果生成候选待办 TaskEntity
     * 如果不满足候选条件，返回 null
     */
    fun generateCandidateTask(
        title: String,
        text: String,
        bigText: String?,
        sourceNotificationId: Long,
        sourceApp: String,
        now: Long = System.currentTimeMillis()
    ): TaskEntity? {
        val result = analyze(title, text, bigText)
        if (!result.isCandidate) return null

        // 待办标题：优先用通知标题，如果太长则截取
        val taskTitle = if (title.isNotBlank()) {
            if (title.length > 50) title.substring(0, 50) + "…" else title
        } else {
            if (text.length > 50) text.substring(0, 50) + "…" else text
        }

        val originalText = buildString {
            if (title.isNotBlank()) append(title).append("\n")
            append(text)
            if (!bigText.isNullOrBlank()) append("\n").append(bigText)
        }

        return TaskEntity(
            title = taskTitle,
            sourceNotificationId = sourceNotificationId,
            sourceApp = sourceApp,
            originalText = originalText,
            dueTime = result.dueTime,
            priority = if (result.isImportant) 2 else 1,
            status = TaskEntity.STATUS_PENDING,
            createdAt = now
        )
    }
    /**
     * 从钉钉消息生成候选待办（专用方法，使用 DingTalkMessage 对象）
     */
    fun generateCandidateFromDingTalk(dingTalkMsg: com.xiaozhao.assistant.service.DingTalkMessage): TaskEntity? {
        val searchText = dingTalkMsg.fullText.lowercase()

        val matchedActions = actionWords.filter { searchText.contains(it.lowercase()) }
        val matchedWork = workWords.filter { searchText.contains(it.lowercase()) }
        val matchedTimes = timeWords.filter { searchText.contains(it.lowercase()) }
        val matchedUrgent = urgentWords.filter { searchText.contains(it.lowercase()) }

        // 检查是否包含动作词和工作词（必须同时满足）
        val isCandidate = matchedActions.isNotEmpty() && matchedWork.isNotEmpty()
        if (!isCandidate) return null

        // 提取截止时间
        val dueTime = if (matchedTimes.isNotEmpty()) {
            TimeParser.parseDueTime(searchText)
        } else null

        // 判断优先级（钉钉消息：@我或@所有人 → 重要）
        val isImportant = matchedUrgent.isNotEmpty() || dingTalkMsg.atMe || dingTalkMsg.atAll

        // 生成待办标题
        val taskTitle = if (dingTalkMsg.content.isNotBlank()) {
            if (dingTalkMsg.content.length > 50) dingTalkMsg.content.substring(0, 50) + "…" else dingTalkMsg.content
        } else {
            dingTalkMsg.sender
        }

        return TaskEntity(
            title = taskTitle,
            sourceNotificationId = dingTalkMsg.notificationId.toLong(),
            sourceApp = "钉钉",
            originalText = dingTalkMsg.fullText,
            dueTime = dueTime,
            priority = if (isImportant) 2 else 1,
            status = TaskEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
    }
}
