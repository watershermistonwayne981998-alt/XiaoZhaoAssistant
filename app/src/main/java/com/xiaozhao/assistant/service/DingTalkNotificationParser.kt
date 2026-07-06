package com.xiaozhao.assistant.service

import android.app.Notification
import android.os.Bundle
import android.util.Log
import com.xiaozhao.assistant.data.entity.NotificationEntity

/**
 * 钉钉通知深度解析器
 * 
 * 钉钉通知的 Bundle extras 里包含结构化字段：
 * - android.title: 发送人/群组名
 * - android.text: 消息摘要
 * - android.bigText: 可能包含更多内容
 * - android.subText: 会话名
 * - android.summaryText: 消息类型（如"消息"、"钉钉"）
 * - android.infoText: 额外信息
 * - android.progress: 进度（文件传输时）
 * - android.largeIcon: 头像
 * 
 * 钉钉特有的 extras key（可能因版本变化）：
 * - conversation_id: 会话ID
 * - message_id: 消息ID
 * - sender_id: 发送者ID
 * - message_type: 消息类型（文本/图片/文件等）
 * - at_all: 是否@所有人
 * - is_group: 是否群聊
 */
object DingTalkNotificationParser {
    
    private const val TAG = "DingTalkParser"
    
    // 钉钉包名
    const val DINGTALK_PACKAGE = "com.alibaba.android.rimet"
    
    /**
     * 从 Notification 的 extras 中深度解析钉钉消息
     * 
     * @return 解析后的结构化数据
     */
    fun parse(notification: Notification): DingTalkMessage? {
        val extras = notification.extras ?: return null
        
        // 基础字段（Android 标准）
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val infoText = extras.getString(Notification.EXTRA_INFO_TEXT) ?: ""
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT) ?: ""
        
        // 钉钉可能使用的自定义 extras（需要实验验证）
        val conversationId = extras.getString("conversation_id") ?: ""
        val messageId = extras.getString("message_id") ?: ""
        val senderId = extras.getString("sender_id") ?: ""
        val messageType = extras.getString("message_type") ?: ""
        val isGroup = extras.getBoolean("is_group", false)
        val atAll = extras.getBoolean("at_all", false)
        val atMe = extras.getBoolean("at_me", false)
        
        // 合并所有可用文本用于待办识别
        val fullText = buildString {
            appendLine(title)
            appendLine(text)
            if (bigText.isNotEmpty() && bigText != text) {
                appendLine(bigText)
            }
            if (subText.isNotEmpty()) {
                appendLine(subText)
            }
        }.trim()
        
        // 提取发送人（title 通常是发送人/群组名）
        val sender = extractSender(title)
        
        // 提取消息内容（text 或 bigText）
        val content = if (bigText.isNotEmpty() && bigText != text) bigText else text
        
        // 判断消息类型
        val type = detectMessageType(messageType, content, subText)
        
        // 提取@信息
        val atInfo = extractAtInfo(content, atAll, atMe)
        
        return DingTalkMessage(
            notificationId = notification.hashCode(),
            postTime = notification.`when`,
            sender = sender,
            conversationName = subText.ifEmpty { title },
            content = content,
            fullText = fullText,
            type = type,
            isGroup = isGroup,
            atAll = atAll,
            atMe = atMe,
            atList = atInfo,
            conversationId = conversationId,
            messageId = messageId,
            rawExtras = extrasToString(extras) // 保存原始 extras 用于调试
        )
    }
    
    /**
     * 从标题中提取发送人
     * 钉钉标题格式可能是：
     * - "张三"（单聊）
     * - "群名(张三)"（群聊，显示发送人）
     * - "张三: 消息摘要"（部分版本）
     */
    private fun extractSender(title: String): String {
        // 群聊格式："群名(张三)" → 提取"张三"
        val groupRegex = """\(([^)]+)\)$""".toRegex()
        val groupMatch = groupRegex.find(title)
        if (groupMatch != null) {
            return groupMatch.groupValues[1]
        }
        
        // 格式："张三: 消息摘要" → 提取"张三"
        val senderRegex = """^([^:：]+)[:：]""".toRegex()
        val senderMatch = senderRegex.find(title)
        if (senderMatch != null) {
            return senderMatch.groupValues[1].trim()
        }
        
        // 默认为标题
        return title
    }
    
    /**
     * 检测消息类型
     */
    private fun detectMessageType(
        messageType: String,
        content: String,
        subText: String
    ): MessageType {
        // 根据 messageType 字段判断
        if (messageType.isNotEmpty()) {
            return when (messageType.lowercase()) {
                "text" -> MessageType.TEXT
                "image" -> MessageType.IMAGE
                "file" -> MessageType.FILE
                "audio" -> MessageType.AUDIO
                "video" -> MessageType.VIDEO
                "link" -> MessageType.LINK
                "markdown" -> MessageType.MARKDOWN
                "action_card" -> MessageType.ACTION_CARD
                else -> MessageType.UNKNOWN
            }
        }
        
        // 根据内容关键词判断
        return when {
            content.contains("[图片]") || content.contains("发送了一张图片") -> MessageType.IMAGE
            content.contains("[文件]") || content.contains("发送了一个文件") -> MessageType.FILE
            content.contains("[语音]") || content.contains("发送了一段语音") -> MessageType.AUDIO
            content.contains("[视频]") || content.contains("发送了一个视频") -> MessageType.VIDEO
            content.contains("[链接]") -> MessageType.LINK
            content.contains("会议") || content.contains("日程") -> MessageType.MEETING
            content.contains("@所有人") || content.contains("@all") -> MessageType.AT_ALL
            else -> MessageType.TEXT
        }
    }
    
    /**
     * 提取@信息
     */
    private fun extractAtInfo(content: String, atAll: Boolean, atMe: Boolean): List<String> {
        val atList = mutableListOf<String>()
        
        if (atAll) {
            atList.add("@所有人")
        }
        
        // 提取 @xxx 格式
        val atRegex = """@(\S+)""".toRegex()
        atRegex.findAll(content).forEach { match ->
            atList.add(match.groupValues[1])
        }
        
        return atList.distinct()
    }
    
    /**
     * 将 extras 转换为字符串（用于调试）
     */
    private fun extrasToString(extras: Bundle): String {
        val sb = StringBuilder()
        for (key in extras.keySet()) {
            val value = extras.get(key)
            sb.append("$key: $value\n")
        }
        return sb.toString()
    }
    
    /**
     * 判断是否是工作相关消息（用于优先级判断）
     */
    fun isWorkRelated(message: DingTalkMessage): Boolean {
        val workKeywords = listOf(
            "会议", "审批", "请假", "报销", "项目", "任务", 
            "deadline", "截止", "提交", "发送", "报送",
            "材料", "文件", "合同", "付款", "发票"
        )
        
        return workKeywords.any { keyword ->
            message.content.contains(keyword) || message.fullText.contains(keyword)
        }
    }
    
    /**
     * 从消息内容中提取待办关键词（增强版）
     */
    fun extractTodoKeywords(message: DingTalkMessage): List<String> {
        val keywords = mutableListOf<String>()
        val text = message.fullText
        
        // 时间词
        val timeWords = listOf("今天", "明天", "后天", "本周", "下周", "月底", 
            "上午", "下午", "晚上", "下班前", "10点", "12点", "5点")
        timeWords.forEach { word ->
            if (text.contains(word)) keywords.add(word)
        }
        
        // 动作词
        val actionWords = listOf("提交", "发送", "报送", "审批", "确认", "修改", 
            "补充", "整理", "开会", "提醒", "处理", "反馈", "上传")
        actionWords.forEach { word ->
            if (text.contains(word)) keywords.add(word)
        }
        
        // 工作词
        val workWords = listOf("材料", "文件", "合同", "报销", "采购", "项目", 
            "会议", "请示", "流程", "付款", "发票", "比选", "询价", "清单")
        workWords.forEach { word ->
            if (text.contains(word)) keywords.add(word)
        }
        
        // 紧急词
        val urgentWords = listOf("尽快", "马上", "抓紧", "今天内", "务必", "别忘了", "提醒一下")
        urgentWords.forEach { word ->
            if (text.contains(word)) keywords.add(word)
        }
        
        return keywords.distinct()
    }
}

/**
 * 钉钉消息结构化数据
 */
data class DingTalkMessage(
    val notificationId: Int,
    val postTime: Long,
    val sender: String,           // 发送人
    val conversationName: String,  // 会话名（群名或单聊名）
    val content: String,           // 消息内容
    val fullText: String,         // 完整文本（用于待办识别）
    val type: MessageType,        // 消息类型
    val isGroup: Boolean,         // 是否群聊
    val atAll: Boolean,           // 是否@所有人
    val atMe: Boolean,            // 是否@我
    val atList: List<String>,     // @列表
    val conversationId: String,   // 会话ID
    val messageId: String,        // 消息ID
    val rawExtras: String         // 原始 extras（用于调试）
)

/**
 * 消息类型
 */
enum class MessageType {
    TEXT,           // 文本
    IMAGE,          // 图片
    FILE,           // 文件
    AUDIO,          // 语音
    VIDEO,          // 视频
    LINK,           // 链接
    MARKDOWN,       // Markdown
    ACTION_CARD,    // 行动卡片
    MEETING,        // 会议
    AT_ALL,         // @所有人
    UNKNOWN         // 未知
}
