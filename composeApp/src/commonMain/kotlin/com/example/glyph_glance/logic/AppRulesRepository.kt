package com.example.glyph_glance.logic

import com.example.glyph_glance.ai.CactusManager
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.database.RuleDao
import kotlinx.coroutines.flow.Flow

class AppRulesRepository(
    private val cactusManager: CactusManager, 
    private val ruleDao: RuleDao
) : RulesRepository {
    override suspend fun addNaturalLanguageRule(text: String) {
        // 1. Ask AI to convert text -> JSON
        val json = cactusManager.translateRule(text)
        // 2. Save to DB
        ruleDao.insert(Rule(rawInstruction = text, jsonLogic = json))
    }
    
    override fun getRulesFlow(): Flow<List<Rule>> = ruleDao.getAllRulesFlow()
}
