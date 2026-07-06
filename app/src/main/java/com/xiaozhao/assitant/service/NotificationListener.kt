package com.xiaozhao.assitant.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xiaozhao.assitant.App
import com.xiaozhao.assitant.data.entity.KeywordRuleEntity
import com.xiaozhao.assitant.data.entity.NotificationEntity
import com.xiaozhao.assitant.engine.TodoRuleEngine
import com.xiaozhao.assitant.util.NotificationUtils
import kotlinx.coroutines.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 通知监听服务
 *
 * 设计要点：
 * - onNotificationPosted 只做轻量提取，内容入队 Channel
 * - 后台协程从 Channel 取出，异步做白名单检查、去重、写库、规则引擎
 * - 不在回调线程做任何重任务（DB 操作、正则匹配等）
 * - 去重依赖 Room unique index（packageName + notificationId + postTime + title + text）
 * - 钉钉通知深度解析：提取 extras 中的结构化字段
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        
        // 钉钉包名
        const val DINGTALK_PACKAGE = "com.alibaba.android.rimet"
    }

    /** 通知处理队列：onNotificationPosted 投递，后台协程消费 */
    private val queue = Channel<StatusBarNotification>(capacity = Channel.UNLIMITED)

    /** 服务级协程作用域 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processorJob: Job? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "通知监听服务已连接")
        startProcessor()
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "通知监听服务已断开")
        processorJob?.cancel()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 过滤掉自身通知
        if (sbn.packageName == packageName) return

        // 非阻塞投递到队列
        scope.launch {
            queue.send(sbn)
        }
    }

    /**
     * 启动后台处理协程
     */
    private fun startProcessor() {
        processorJob = scope.launch {
            for (sbn in queue) {
                try {
                    processNotification(sbn)
                } catch (e: Exception) {
                    Log.e(TAG, "处理通知失败: ${sbn.packageName}", e)
                }
            }
        }
    }

    /**
     * 异步处理单条通知：白名单 → 去重写入 → 规则引擎 → 生成候选待办
     */
    private suspend fun processNotification(sbn: StatusBarNotification) {
        val repo = App.instance.repository
        val notification = sbn.notification

        // 1. 检查采集开关
        if (!App.instance.prefs.getBoolean(App.PREF_COLLECTION_ENABLED, true)) {
            return
        }

        // 2. 白名单检查
        val whitelistTotal = repo.whitelistCount()
        if (whitelistTotal > 0) {
            if (!repo.whitelistEnabled(sbn.packageName)) return
        }

        // 3. 提取通知内容（基础字段）
        val extracted = NotificationUtils.extractText(sbn)
        if (extracted.title.isBlank() && extracted.text.isBlank()) return

        val appName = NotificationUtils.getAppName(this, sbn.packageName)

        // 4. 钉钉通知深度解析
        var sender: String? = null
        var conversationName: String? = null
        var messageType: String? = null
        var isGroup = false
        var atAll = false
        var atMe = false
        var fullText = "${extracted.title} ${extracted.text}".trim()

        if (sbn.packageName == DINGTALK_PACKAGE) {
            val dingTalkMsg = DingTalkNotificationParser.parse(notification)
            if (dingTalkMsg != null) {
                sender = dingTalkMsg.sender
                conversationName = dingTalkMsg.conversationName
                messageType = dingTalkMsg.type.name
                isGroup = dingTalkMsg.isGroup
                atAll = dingTalkMsg.atAll
                atMe = dingTalkMsg.atMe
                fullText = dingTalkMsg.fullText

                Log.d(TAG, "钉钉消息解析: sender=$sender, type=${dingTalkMsg.type}, atMe=$atMe, fullText=$fullText")
            }
        }

        // 5. 构建 NotificationEntity
        val entity = NotificationEntity(
            packageName = sbn.packageName,
            appName = appName,
            title = extracted.title,
            text = extracted.text,
            bigText = extracted.bigText,
            postTime = sbn.postTime,
            notificationKey = sbn.key,
            notificationId = sbn.id,
            isRead = false,
            isConvertedToTask = false,
            isImportant = atMe || atAll, // @我或@所有人标记为重要
            createdAt = System.currentTimeMillis(),
            sender = sender,
            conversationName = conversationName,
            messageType = messageType,
            isGroup = isGroup,
            atAll = atAll,
            atMe = atMe,
            fullText = fullText
        )

        // 6. 写入数据库（Room unique index 自动去重）
        val rowId = repo.insertNotification(entity)
        if (rowId == -1L) {
            // 去重命中（唯一索引冲突），跳过
            return
        }

        // 7. 规则引擎分析（使用 fullText 进行更准确的待办识别）
        val allKeywords = mutableListOf<KeywordRuleEntity>()
        allKeywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_TIME))
        allKeywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_ACTION))
        allKeywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_WORK))
        allKeywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_URGENT))

        val engine = TodoRuleEngine(allKeywords)
        val candidate = engine.generateCandidateTask(
            title = entity.title,
            text = entity.text,
            bigText = entity.bigText,
            sourceNotificationId = rowId,
            sourceApp = entity.appName,
            fullText = fullText // 传递完整文本给规则引擎
        )

        if (candidate != null) {
            // 8. 保存候选待办（状态 = PENDING 待确认）
            repo.insertTask(candidate)

            // 9. 标记通知已转待办
            repo.markConverted(rowId)

            // 10. 如果紧急词命中，标记通知为重要
            if (candidate.priority >= 2) {
                repo.markImportant(rowId)
            }
        }
    }

    override fun onDestroy() {
        processorJob?.cancel()
        scope.cancel()
        queue.close()
        super.onDestroy()
    }
}
