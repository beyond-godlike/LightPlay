package com.unava.dia.lightplay.service.callbacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.unava.dia.lightplay.other.AppConstants.NOTIFICATION_ID
import com.unava.dia.lightplay.service.PlayService

class SongsNotificationListener(private val playService: PlayService) : PlayerNotificationManager.NotificationListener {
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        playService.apply {
            stopForeground(true)
            isForeground = false
            stopSelf()
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        playService.apply {
            if(ongoing && !isForeground) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)
                )
                startForeground(NOTIFICATION_ID, notification)
                isForeground = true
            }
        }
    }
}