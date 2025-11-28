package com.example.glyph_glance.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_profiles")
data class ContactProfile(
    @PrimaryKey val senderId: String, // Hash or raw phone number
    val isSplitTexter: Boolean = false,
    val avgInterMessageTimeMillis: Long = 0L, // Running average
    val baselineSentiment: Float = 0.5f, // 0.0 (Neg) -> 1.0 (Pos)
    val lastMessageTimestamp: Long = 0L
)
