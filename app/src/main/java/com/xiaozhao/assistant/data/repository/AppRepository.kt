package com.xiaozhao.assistant.data.repository

import com.xiaozhao.assistant.data.db.AppDatabase
import com.xiaozhao.assistant.data.db.NotificationDao
import com.xiaozhao.assistant.data.entity.AppWhitelistEntity
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.data.entity.TaskEntity
import com.xiaozhao.assistant.engine.KeywordSets
import kotlinx.coroutines.flow.Flow

/**
 * 统一数据访问入口
 * 所有 UI 层和 Service 层通过 Repository 操作数据
 */
class AppRepository(
    private val db: AppDatabase
) {
    private val notificationDao: NotificationDao = db.notificationDao()
    private val taskDao = db.taskDao()
    private val whitelistDao = db.whitelistDao()
    private val keywordDao = db.keywordDao()

    // ===== Notification =====

    suspend fun insertNotification(n: NotificationEntity): Long {
        return notificationDao.insert(n)
    }

    fun allNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAll()

    fun notificationsByPackage(pkg: String) = notificationDao.getByPackage(pkg)

    fun searchNotifications(keyword: String) = notificationDao.search(keyword)

    fun distinctApps() = notificationDao.getDistinctApps()

    suspend fun markAsRead(id: Long) = notificationDao.markAsRead(id)

    suspend fun markConverted(id: Long) = notificationDao.markConverted(id)

    suspend fun markImportant(id: Long) = notificationDao.markImportant(id)

    fun unreadCount(): Flow<Int> = notificationDao.unreadCount()

    fun importantUnreadCount(): Flow<Int> = notificationDao.importantUnreadCount()

    suspend fun getNotification(id: Long) = notificationDao.getById(id)

    suspend fun deleteAllNotifications() = notificationDao.deleteAll()

    // ===== Task =====

    suspend fun insertTask(t: TaskEntity): Long = taskDao.insert(t)

    fun tasksByStatus(status: String) = taskDao.getByStatus(status)

    fun todayTasks(dayStart: Long, dayEnd: Long) = taskDao.getTodayTasks(dayStart, dayEnd)

    fun overdueTasks(now: Long) = taskDao.getOverdueTasks(now)

    fun recentTasks(limit: Int) = taskDao.getRecentTasks(limit)

    fun todayCount(): Flow<Int> = taskDao.todayCount()

    suspend fun markTaskDone(id: Long) = taskDao.markDone(id)

    suspend fun confirmTask(id: Long) = taskDao.confirmTask(id)

    suspend fun updateTaskDueTime(id: Long, dueTime: Long?) = taskDao.updateDueTime(id, dueTime)

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)

    suspend fun getTask(id: Long) = taskDao.getById(id)

    suspend fun deleteAllTasks() = taskDao.deleteAll()

    suspend fun getAllTasksOnce() = taskDao.getAllOnce()

    // ===== Whitelist =====

    fun allWhitelist() = whitelistDao.getAll()

    fun enabledWhitelist() = whitelistDao.getEnabledFlow()

    suspend fun whitelistEnabled(pkg: String): Boolean = whitelistDao.isEnabled(pkg) ?: false

    suspend fun upsertWhitelist(app: AppWhitelistEntity) = whitelistDao.upsert(app)

    suspend fun setWhitelistEnabled(pkg: String, enabled: Boolean) =
        whitelistDao.setEnabled(pkg, enabled)

    suspend fun deleteWhitelist(pkg: String) = whitelistDao.delete(pkg)

    suspend fun whitelistCount(): Int = whitelistDao.count()

    // ===== Keywords =====

    fun allKeywords() = keywordDao.getAll()

    fun enabledKeywords() = keywordDao.getAllEnabled()

    suspend fun keywordsByType(type: String) = keywordDao.getByType(type)

    suspend fun insertKeyword(rule: KeywordRuleEntity) = keywordDao.insert(rule)

    suspend fun setKeywordEnabled(id: Long, enabled: Boolean) = keywordDao.setEnabled(id, enabled)

    suspend fun deleteKeyword(id: Long) = keywordDao.deleteById(id)

    suspend fun keywordCount() = keywordDao.count()

    /**
     * 初始化默认关键词规则（首次安装时调用）
     */
    suspend fun seedDefaultKeywordsIfEmpty() {
            suspend fun seedDefaultKeywordsIfEmpty() {
        if (keywordDao.count() > 0) return
        val defaults = mutableListOf<KeywordRuleEntity>()
        KeywordSets.TIME_WORDS.forEach { defaults.add(KeywordRuleEntity(keyword = it, type = KeywordRuleEntity.TYPE_TIME)) }
        KeywordSets.ACTION_WORDS.forEach { defaults.add(KeywordRuleEntity(keyword = it, type = KeywordRuleEntity.TYPE_ACTION)) }
        KeywordSets.WORK_WORDS.forEach { defaults.add(KeywordRuleEntity(keyword = it, type = KeywordRuleEntity.TYPE_WORK)) }
        KeywordSets.URGENT_WORDS.forEach { defaults.add(KeywordRuleEntity(keyword = it, type = KeywordRuleEntity.TYPE_URGENT)) }
        keywordDao.insertAll(defaults)
    }

    }

    // ===== Bulk operations =====

    suspend fun clearAllData() {
        notificationDao.deleteAll()
        taskDao.deleteAll()
    }
}
