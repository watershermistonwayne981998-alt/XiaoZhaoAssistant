package com.xiaozhao.assistant.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xiaozhao.assistant.App
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.engine.TodoRuleEngine
import com.xiaozhao.assistant.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * 通知监听服务
 *
 * 设计要点：
 * - onNotificationPosted 只做轻量提取，内容入队 Channel
 * - 后台协程从 Channel 取出，异步做白名单检查、去重、写库、规则引擎
 * - 不在回调线程做任何重任务（DB 操作、正则匹配等）
 * - 去重依赖 Room unique index（packageName + notificationId + postTime + title + text）
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
    }

    /** 通知处理队列：onNotificationPosted 投递，后台协程消费 */
    private val queue = Channel<NotificationEntity>(capacity = Channel.UNLIMITED)

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

        // 过滤掉前台服务常驻通知
        if (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return

        val extracted = NotificationUtils.extractText(sbn)
        if (extracted.title.isBlank() && extracted.text.isBlank()) return

        val appName = NotificationUtils.getAppName(this, sbn.packageName)

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
            isImportant = false,
            createdAt = System.currentTimeMillis()
        )

        // 非阻塞投递到队列
        scope.launch {
            queue.send(entity)
        }
    }

    /**
     * 启动后台处理协程
     */
    private fun startProcessor() {
        processorJob = scope.launch {
            for (entity in queue) {
                try {
                    processNotification(entity)
                } catch (e: Exception) {
                    Log.e(TAG, "处理通知失败: ${entity.title}", e)
                }
            }
        }
    }

    /**
     * 异步处理单条通知：白名单 → 去重写入 → 规则引擎 → 生成候选待办
     */
    private suspend fun processNotification(entity: NotificationEntity) {
        val repo = App.instance.repository

        // 1. 检查采集开关
        if (!App.instance.prefs.getBoolean(App.PREF_COLLECTION_ENABLED, true)) {
            return
        }

        // 2. 白名单检查
        //    白名单表为空 → 默认全部采集（首次安装行为）
        //    白名单表非空 → 只采集 enabled=true 的包名
        val whitelistTotal = repo.whitelistCount()
        if (whitelistTotal > 0) {
            if (!repo.whitelistEnabled(entity.packageName)) return
        }

        // 3. 写入数据库（Room unique index 自动去重）
        val rowId = repo.insertNotification(entity)
        if (rowId == -1L) {
            // 去重命中（唯一索引冲突），跳过
            return
        }

        // 4. 规则引擎分析
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
            sourceApp = entity.appName
        )

        if (candidate != null) {
            // 5. 保存候选待办（状态 = PENDING 待确认）
            repo.insertTask(candidate)

            // 6. 标记通知已转待办
            repo.markConverted(rowId)

            // 7. 如果紧急词命中，标记通知为重要
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
