package com.xiaozhao.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaozhao.assistant.App
import com.xiaozhao.assistant.data.entity.AppWhitelistEntity
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.data.entity.TaskEntity
import com.xiaozhao.assistant.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: AppRepository = (application as App).repository
    private val prefs = (application as App).prefs

    // ===== 首页统计 =====
    val todayTodoCount: StateFlow<Int> = repo.todayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCount: StateFlow<Int> = repo.unreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val importantCount: StateFlow<Int> = repo.importantUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentTasks: StateFlow<List<TaskEntity>> = repo.recentTasks(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== 消息页 =====
    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    val distinctApps: StateFlow<List<AppPackageInfoUi>> = repo.distinctApps()
        .map { list -> list.map { AppPackageInfoUi(it.package_name, it.app_name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> =
        combine(_selectedPackage, _searchKeyword) { pkg, keyword -> Pair(pkg, keyword) }
            .flatMapLatest { (pkg, keyword) ->
                when {
                    keyword.isNotBlank() -> repo.searchNotifications(keyword)
                    pkg != null -> repo.notificationsByPackage(pkg)
                    else -> repo.allNotifications()
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPackage(pkg: String?) { _selectedPackage.value = pkg }
    fun search(keyword: String) { _searchKeyword.value = keyword }

    // ===== 待办页 =====
    private val todayRange = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        Pair(start, cal.timeInMillis)
    }

    val pendingTasks: StateFlow<List<TaskEntity>> = repo.tasksByStatus(TaskEntity.STATUS_PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<TaskEntity>> = repo.todayTasks(todayRange.first, todayRange.second)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTasks: StateFlow<List<TaskEntity>> = repo.overdueTasks(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doneTasks: StateFlow<List<TaskEntity>> = repo.tasksByStatus(TaskEntity.STATUS_DONE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== 设置页 =====
    val allWhitelist: StateFlow<List<AppWhitelistEntity>> = repo.allWhitelist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKeywords: StateFlow<List<KeywordRuleEntity>> = repo.allKeywords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _collectionEnabled = MutableStateFlow(
        prefs.getBoolean(App.PREF_COLLECTION_ENABLED, true)
    )
    val collectionEnabled: StateFlow<Boolean> = _collectionEnabled.asStateFlow()

    fun toggleCollection() {
        val newValue = !_collectionEnabled.value
        _collectionEnabled.value = newValue
        prefs.edit().putBoolean(App.PREF_COLLECTION_ENABLED, newValue).apply()
    }

    // ===== 操作 =====

    fun markNotificationRead(id: Long) {
        viewModelScope.launch { repo.markAsRead(id) }
    }

    fun convertToTodo(notification: NotificationEntity) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = if (notification.title.isNotBlank()) notification.title else notification.text.take(50),
                sourceNotificationId = notification.id,
                sourceApp = notification.appName,
                originalText = buildString {
                    if (notification.title.isNotBlank()) append(notification.title).append("\n")
                    append(notification.text)
                    if (!notification.bigText.isNullOrBlank()) append("\n").append(notification.bigText)
                },
                dueTime = null,
                priority = if (notification.isImportant) 2 else 1,
                status = TaskEntity.STATUS_PENDING
            )
            repo.insertTask(task)
            repo.markConverted(notification.id)
        }
    }

    fun confirmTask(id: Long) {
        viewModelScope.launch { repo.confirmTask(id) }
    }

    fun completeTask(id: Long) {
        viewModelScope.launch { repo.markTaskDone(id) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repo.deleteTask(id) }
    }

    fun updateTaskDueTime(id: Long, dueTime: Long?) {
        viewModelScope.launch { repo.updateTaskDueTime(id, dueTime) }
    }

    fun setWhitelistEnabled(pkg: String, appName: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.upsertWhitelist(AppWhitelistEntity(pkg, appName, enabled))
        }
    }

    fun addWhitelistApp(pkg: String, appName: String) {
        viewModelScope.launch {
            repo.upsertWhitelist(AppWhitelistEntity(pkg, appName, false))
        }
    }

    fun addKeyword(keyword: String, type: String) {
        viewModelScope.launch {
            repo.insertKeyword(KeywordRuleEntity(keyword = keyword, type = type, enabled = true))
        }
    }

    fun toggleKeyword(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setKeywordEnabled(id, enabled) }
    }

    fun deleteKeyword(id: Long) {
        viewModelScope.launch { repo.deleteKeyword(id) }
    }

    fun clearAllData() {
        viewModelScope.launch { repo.clearAllData() }
    }

    suspend fun exportData(): String {
        val tasks = repo.getAllTasksOnce()
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"exportTime\": ").append(System.currentTimeMillis()).append(",\n")
        sb.append("  \"tasks\": [\n")
        tasks.forEachIndexed { index, task ->
            sb.append("    ")
            sb.append(taskToJson(task))
            if (index < tasks.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    private fun taskToJson(t: TaskEntity): String {
        return buildString {
            append("{")
            append("\"id\":${t.id},")
            append("\"title\":\"${escape(t.title)}\",")
            append("\"sourceApp\":\"${escape(t.sourceApp ?: "")}\",")
            append("\"originalText\":\"${escape(t.originalText ?: "")}\",")
            append("\"dueTime\":${t.dueTime ?: "null"},")
            append("\"priority\":${t.priority},")
            append("\"status\":\"${t.status}\",")
            append("\"createdAt\":${t.createdAt},")
            append("\"completedAt\":${t.completedAt ?: "null"}")
            append("}")
        }
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "")
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(App.instance) as T
            }
        }
    }
}

data class AppPackageInfoUi(
    val packageName: String,
    val appName: String
)
