package com.xiaozhao.assistant

import android.app.Application
import android.content.SharedPreferences
import com.xiaozhao.assistant.data.db.AppDatabase
import com.xiaozhao.assistant.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var prefs: SharedPreferences
        private set

    /** 应用级协程作用域，用于后台初始化等 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        prefs = getSharedPreferences("xiaozhao_prefs", MODE_PRIVATE)
        repository = AppRepository(AppDatabase.get(this))

        // 初始化默认关键词
        appScope.launch {
            repository.seedDefaultKeywordsIfEmpty()
        }
    }

    companion object {
        lateinit var instance: App
            private set

        const val PREF_COLLECTION_ENABLED = "collection_enabled"
    }
}
