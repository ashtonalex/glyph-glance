package com.example.glyph_glance.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE isActive = 1")
    suspend fun getAllRules(): List<Rule>

    @Query("SELECT * FROM rules")
    fun getAllRulesFlow(): Flow<List<Rule>>

    @Insert
    suspend fun insert(rule: Rule)
}
