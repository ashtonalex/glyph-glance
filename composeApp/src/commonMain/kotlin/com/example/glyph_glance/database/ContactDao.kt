package com.example.glyph_glance.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact_profiles WHERE senderId = :senderId")
    suspend fun getProfile(senderId: String): ContactProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ContactProfile)

    @Update
    suspend fun update(profile: ContactProfile)
}
