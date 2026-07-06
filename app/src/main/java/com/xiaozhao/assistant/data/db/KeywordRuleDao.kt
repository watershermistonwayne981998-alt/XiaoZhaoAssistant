package com.xiaozhao.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaozhao.assistant.data.entity.KeywordRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: KeywordRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<KeywordRuleEntity>)

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1 ORDER BY type, keyword")
    fun getAllEnabled(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules ORDER BY type, keyword")
    fun getAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1 AND type = :type")
    suspend fun getByType(type: String): List<KeywordRuleEntity>

    @Query("UPDATE keyword_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM keyword_rules")
    suspend fun count(): Int
}
