package com.xiaozhao.assistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xiaozhao.assistant.data.entity.AppWhitelistEntity
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import com.xiaozhao.assistant.data.entity.NotificationEntity
import com.xiaozhao.assistant.data.entity.TaskEntity

@Database(
    entities = [
        NotificationEntity::class,
        TaskEntity::class,
        AppWhitelistEntity::class,
        KeywordRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun taskDao(): TaskDao
    abstract fun whitelistDao(): AppWhitelistDao
    abstract fun keywordDao(): KeywordRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xiaozhao.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
