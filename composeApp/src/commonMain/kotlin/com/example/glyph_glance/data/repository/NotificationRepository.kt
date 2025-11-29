package com.example.glyph_glance.data.repository

import com.example.glyph_glance.database.NotificationDao
import com.example.glyph_glance.data.models.Notification
import com.example.glyph_glance.data.models.NotificationPriority
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<Notification>>
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<Notification>>
    fun getRecentNotifications(limit: Int): Flow<List<Notification>>
    fun getUnreadCountByPriority(priority: NotificationPriority): Flow<Int>
    suspend fun insertNotification(notification: Notification)
    suspend fun markAsRead(id: Long)
    suspend fun deleteNotification(id: Long)
}

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {
    override fun getAllNotifications() = dao.getAllNotifications()
    
    override fun getNotificationsByPriority(priority: NotificationPriority) =
        dao.getNotificationsByPriority(priority)
    
    override fun getRecentNotifications(limit: Int) =
        dao.getRecentNotifications(limit)
    
    override fun getUnreadCountByPriority(priority: NotificationPriority) =
        dao.getUnreadCountByPriority(priority)
    
    override suspend fun insertNotification(notification: Notification) =
        dao.insertNotification(notification)
    
    override suspend fun markAsRead(id: Long) =
        dao.markAsRead(id)
    
    override suspend fun deleteNotification(id: Long) =
        dao.deleteNotification(id)
}
