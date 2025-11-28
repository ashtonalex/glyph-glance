package com.example.glyph_glance.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ContactProfile::class, Rule::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun ruleDao(): RuleDao
}
