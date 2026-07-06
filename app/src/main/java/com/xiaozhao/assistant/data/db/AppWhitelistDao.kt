package com.xiaozhao.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaozhao.assistant.data.entity.AppWhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppWhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AppWhitelistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppWhitelistEntity>)

    @Query("SELECT * FROM app_whitelist ORDER BY app_name")
    fun getAll(): Flow<List<AppWhitelistEntity>>

    @Query("SELECT * FROM app_whitelist WHERE enabled = 1")
    suspend fun getEnabled(): List<AppWhitelistEntity>

    @Query("SELECT * FROM app_whitelist WHERE enabled = 1")
    fun getEnabledFlow(): Flow<List<AppWhitelistEntity>>

    @Query("SELECT enabled FROM app_whitelist WHERE package_name = :pkg LIMIT 1")
    suspend fun isEnabled(pkg: String): Boolean?

    @Query("UPDATE app_whitelist SET enabled = :enabled WHERE package_name = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("DELETE FROM app_whitelist WHERE package_name = :pkg")
    suspend fun delete(pkg: String)

    @Query("SELECT COUNT(*) FROM app_whitelist")
    suspend fun count(): Int
}
