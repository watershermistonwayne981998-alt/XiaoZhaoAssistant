package com.xiaozhao.assitant.data.repository

import com.xiaozhao.assitant.data.db.AppDatabase
import com.xiaozhao.assitant.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 应用数据仓库
 * 统一管理所有数据操作
 */
class AppRepository(private val db: AppDatabase) {
    private val notificationDao = db.notificationDao()
    private val taskDao = db.taskDao()
    private val whitelistDao = db.whitelistDao()
    private val keywordRuleDao = db.keywordRuleDao()

    // ==================== 通知操作 ====================

    suspend fun insertNotification(entity: NotificationEntity): Long {
        return notificationDao.insert(entity)
    }

    fun allNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.all()
    }

    fun unreadCount(): Flow<Int> {
        return notificationDao.unreadCount()
    }

    suspend fun markRead(id: Long) {
        notificationDao.markRead(id)
    }

    suspend fun markAllRead() {
        notificationDao.markAllRead()
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.delete(id)
    }

    suspend fun markConverted(notificationId: Long) {
        notificationDao.markConverted(notificationId)
    }

    suspend fun markImportant(notificationId: Long) {
        notificationDao.markImportant(notificationId)
    }

    fun searchNotifications(keyword: String): Flow<List<NotificationEntity>> {
        return notificationDao.search("%${keyword}%")
    }

    fun notificationsByApp(packageName: String): Flow<List<NotificationEntity>> {
        return notificationDao.byPackage(packageName)
    }

    // ==================== 待办操作 ====================

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insert(task)
    }

    fun allTasks(): Flow<List<TaskEntity>> {
        return taskDao.all()
    }

    fun pendingTasks(): Flow<List<TaskEntity>> {
        return taskDao.byStatus(TaskEntity.STATUS_PENDING)
    }

    fun todayTasks(): Flow<List<TaskEntity>> {
        return taskDao.todayTasks()
    }

    fun overdueTasks(): Flow<List<TaskEntity>> {
        return taskDao.overdueTasks()
    }

    fun completedTasks(): Flow<List<TaskEntity>> {
        return taskDao.byStatus(TaskEntity.STATUS_DONE)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
    }

    suspend fun completeTask(id: Long) {
        taskDao.markComplete(id, System.currentTimeMillis())
    }

    suspend fun deleteTask(id: Long) {
        taskDao.delete(id)
    }

    fun pendingCount(): Flow<Int> {
        return taskDao.countByStatus(TaskEntity.STATUS_PENDING)
    }

    fun todayCount(): Flow<Int> {
        return taskDao.todayCount()
    }

    fun overdueCount(): Flow<Int> {
        return taskDao.overdueCount()
    }

    fun importantCount(): Flow<Int> {
        return notificationDao.importantCount()
    }

    // ==================== 白名单操作 ====================

    suspend fun insertWhitelist(entity: AppWhitelistEntity) {
        whitelistDao.insert(entity)
    }

    suspend fun updateWhitelist(entity: AppWhitelistEntity) {
        whitelistDao.update(entity)
    }

    suspend fun deleteWhitelist(packageName: String) {
        whitelistDao.delete(packageName)
    }

    fun allWhitelist(): Flow<List<AppWhitelistEntity>> {
        return whitelistDao.all()
    }

    suspend fun whitelistCount(): Int {
        return whitelistDao.count()
    }

    suspend fun whitelistEnabled(packageName: String): Boolean {
        return whitelistDao.isEnabled(packageName)
    }

    // ==================== 关键词规则操作 ====================

    suspend fun insertKeyword(rule: KeywordRuleEntity) {
        keywordRuleDao.insert(rule)
    }

    suspend fun updateKeyword(rule: KeywordRuleEntity) {
        keywordRuleDao.update(rule)
    }

    suspend fun deleteKeyword(id: Long) {
        keywordRuleDao.delete(id)
    }

    fun allKeywords(): Flow<List<KeywordRuleEntity>> {
        return keywordRuleDao.all()
    }

    suspend fun keywordsByType(type: Int): List<KeywordRuleEntity> {
        return keywordRuleDao.byType(type)
    }

    suspend fun initDefaultKeywords() {
        val defaults = listOf(
            // 时间词
            KeywordRuleEntity(keyword = "今天", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "明天", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "后天", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "本周", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "下周", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "月底", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "上午", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "下午", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "晚上", type = KeywordRuleEntity.TYPE_TIME),
            KeywordRuleEntity(keyword = "下班前", type = KeywordRuleEntity.TYPE_TIME),
            // 动作词
            KeywordRuleEntity(keyword = "提交", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "发送", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "报送", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "审批", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "确认", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "修改", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "补充", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "整理", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "开会", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "提醒", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "处理", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "反馈", type = KeywordRuleEntity.TYPE_ACTION),
            KeywordRuleEntity(keyword = "上传", type = KeywordRuleEntity.TYPE_ACTION),
            // 工作词
            KeywordRuleEntity(keyword = "材料", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "文件", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "合同", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "报销", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "采购", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "项目", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "会议", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "请示", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "流程", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "付款", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "发票", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "比选", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "询价", type = KeywordRuleEntity.TYPE_WORK),
            KeywordRuleEntity(keyword = "清单", type = KeywordRuleEntity.TYPE_WORK),
            // 紧急词
            KeywordRuleEntity(keyword = "尽快", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "马上", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "抓紧", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "今天内", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "务必", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "别忘了", type = KeywordRuleEntity.TYPE_URGENT),
            KeywordRuleEntity(keyword = "提醒一下", type = KeywordRuleEntity.TYPE_URGENT)
        )
        defaults.forEach { insertKeyword(it) }
    }

    // ==================== 数据管理 ====================

    suspend fun clearAllData() {
        notificationDao.deleteAll()
        taskDao.deleteAll()
    }

    suspend fun clearNotifications() {
        notificationDao.deleteAll()
    }

    suspend fun clearTasks() {
        taskDao.deleteAll()
    }
}
