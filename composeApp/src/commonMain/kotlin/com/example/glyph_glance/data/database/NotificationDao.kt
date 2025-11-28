package com.example.glyph_glance.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.glyph_glance.data.models.Notification
import com.example.glyph_glance.data.models.NotificationPriority
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE priority = :priority ORDER BY timestamp DESC")
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int): Flow<List<Notification>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE priority = :priority AND isRead = 0")
    fun getUnreadCountByPriority(priority: NotificationPriority): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    
    @Update
    suspend fun updateNotification(notification: Notification)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}
