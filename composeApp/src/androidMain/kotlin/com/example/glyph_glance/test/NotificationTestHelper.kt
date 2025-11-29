package com.example.glyph_glance.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.glyph_glance.service.LiveLogger

/**
 * Helper class to send test notifications for debugging.
 * This allows you to test notification interception without needing external apps.
 */
object NotificationTestHelper {
    
    private const val CHANNEL_ID = "glyph_test_channel"
    private const val CHANNEL_NAME = "Glyph Test Notifications"
    
    /**
     * Create a notification channel (required for Android 8.0+)
     */
    fun createTestChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Send a test notification that will be intercepted by GlyphNotificationListener.
     * 
     * @param context The application context
     * @param title The notification title
     * @param text The notification text
     * @param notificationId Unique ID for this notification
     */
    fun sendTestNotification(
        context: Context,
        title: String = "Test Notification",
        text: String = "This is a test notification",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        createTestChannel(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
        
        LiveLogger.addLog("TEST: Sent test notification #$notificationId: $title - $text")
    }
    
    /**
     * Send multiple test notifications rapidly to test buffering.
     * 
     * @param context The application context
     * @param count Number of notifications to send
     * @param delayMs Delay between notifications in milliseconds
     */
    fun sendRapidTestNotifications(
        context: Context,
        count: Int = 3,
        delayMs: Long = 1000
    ) {
        createTestChannel(context)
        
        Thread {
            repeat(count) { index ->
                sendTestNotification(
                    context = context,
                    title = "Test Message ${index + 1}",
                    text = "This is test message number ${index + 1} of $count",
                    notificationId = 1000 + index
                )
                
                if (index < count - 1) {
                    Thread.sleep(delayMs)
                }
            }
        }.start()
        
        LiveLogger.addLog("TEST: Sent $count rapid test notifications")
    }
}

