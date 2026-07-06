package com.xiaozhao.assistant.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xiaozhao.assistant.App
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.engine.TodoRuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 钉钉无障碍服务
 * 
 * 读取钉钉聊天界面当前屏幕上的消息
 * 注意：只能读取当前屏幕显示的消息，无法读取历史消息
 */
class DingTalkAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DingTalkA11y"
        private const val DINGTALK_PACKAGE = "com.alibaba.android.rimet"
        private val processedCache = ConcurrentHashMap<String, Long>()
        private const val CACHE_CLEANUP_INTERVAL = 10 * 60 * 1000L
        private var lastCleanup = 0L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != DINGTALK_PACKAGE) return
        
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }
        
        cleanupCache()
        val messages = extractMessages()
        
        if (messages.isNotEmpty()) {
            Log.d(TAG, "提取到 ${messages.size} 条消息")
            messages.forEach { processMessage(it) }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "服务被中断")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "钉钉无障碍服务已连接")
        tryRootKeepAlive()
    }

    private fun extractMessages(): List<MessageData> {
        val result = mutableListOf<MessageData>()
        val root = rootInActiveWindow ?: return result
        
        try {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            collectTextNodes(root, nodes)
            
            for (node in nodes) {
                val text = node.text?.toString() ?: continue
                if (text.length < 5 || text.length > 500) continue
                if (text.contains("\n")) continue
                
                val key = "${text.hashCode()}_${System.currentTimeMillis() / 60000}"
                if (processedCache.containsKey(key)) continue
                
                result.add(MessageData(text, "", System.currentTimeMillis()))
                processedCache[key] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取失败", e)
        } finally {
            root.recycle()
        }
        return result
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.childCount == 0) {
            if (node.text != null) out.add(AccessibilityNodeInfo.obtain(node))
            return
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                collectTextNodes(it, out)
                it.recycle()
            }
        }
    }

    private fun processMessage(msg: MessageData) {
        scope.launch {
            try {
                val repo = App.instance.repository
                if (!App.instance.prefs.getBoolean(App.PREF_COLLECTION_ENABLED, true)) return@launch
                if (repo.whitelistCount() > 0 && !repo.whitelistEnabled(DINGTALK_PACKAGE)) return@launch
                
                val entity = NotificationEntity(
                    packageName = DINGTALK_PACKAGE,
                    appName = "钉钉",
                    title = if (msg.sender.isNotEmpty()) msg.sender else "钉钉消息",
                    text = msg.content,
                    bigText = "",
                    postTime = msg.timestamp,
                    notificationKey = "a11y_${msg.timestamp}_${msg.content.hashCode()}",
                    notificationId = (msg.timestamp % Int.MAX_VALUE).toInt(),
                    isRead = false,
                    isConvertedToTask = false,
                    isImportant = false,
                    createdAt = System.currentTimeMillis()
                )
                
                val rowId = repo.insertNotification(entity)
                if (rowId == -1L) return@launch
                
                val keywords = mutableListOf<KeywordRuleEntity>()
                keywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_TIME))
                keywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_ACTION))
                keywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_WORK))
                keywords.addAll(repo.keywordsByType(KeywordRuleEntity.TYPE_URGENT))
                
                val candidate = TodoRuleEngine(keywords).generateCandidateTask(
                    title = entity.title,
                    text = entity.text,
                    bigText = entity.bigText,
                    sourceNotificationId = rowId,
                    sourceApp = entity.appName
                )
                
                if (candidate != null) {
                    repo.insertTask(candidate)
                    repo.markConverted(rowId)
                    if (candidate.priority >= 2) repo.markImportant(rowId)
                    Log.d(TAG, "生成候选待办: ${candidate.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败", e)
            }
        }
    }

    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        if (now - lastCleanup < CACHE_CLEANUP_INTERVAL) return
        lastCleanup = now
        val it = processedCache.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > CACHE_CLEANUP_INTERVAL) it.remove()
        }
    }

    private fun tryRootKeepAlive() {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            if (p.waitFor() == 0) {
                Log.i(TAG, "检测到 root 权限")
            }
        } catch (e: Exception) {
            Log.w(TAG, "无 root 权限: ${e.message}")
        }
    }

    private data class MessageData(val content: String, val sender: String, val timestamp: Long)
}
