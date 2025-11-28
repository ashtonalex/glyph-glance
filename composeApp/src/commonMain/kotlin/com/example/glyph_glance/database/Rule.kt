package com.example.glyph_glance.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawInstruction: String, // "Don't disturb unless it's Mom"
    val jsonLogic: String, // Serialized JSON: {"whitelist": ["Mom"], "min_urgency": 4}
    val isActive: Boolean = true
)
