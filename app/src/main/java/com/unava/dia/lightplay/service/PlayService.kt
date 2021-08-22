package com.unava.dia.lightplay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.unava.dia.lightplay.R
import com.unava.dia.lightplay.other.AppConstants.ACTION_PAUSE_SERVICE
import com.unava.dia.lightplay.other.AppConstants.ACTION_RESUME_SERVICE
import com.unava.dia.lightplay.other.AppConstants.ACTION_START_SERVICE
import com.unava.dia.lightplay.other.AppConstants.ACTION_STOP_SERVICE
import com.unava.dia.lightplay.other.AppConstants.NOTIFICATION_CHANNEL_ID
import com.unava.dia.lightplay.other.AppConstants.NOTIFICATION_CHANNEL_NAME
import com.unava.dia.lightplay.other.AppConstants.NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PlayService : LifecycleService() {

    private lateinit var mPlayer: MediaPlayer


    private var length: Int = 0
    private var notificationManager: NotificationManager? = null

    private val time = MutableLiveData<Int>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    var serviceKilled = false

    companion object {
        val isPlaying = MutableLiveData<Boolean>()
    }

    private fun postInitialValues() {
        isPlaying.postValue(false)
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        isPlaying.observe(this, {
            updateNotification(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    val str: String = intent.getStringExtra("URI") ?: ""
                    if (str.isNotEmpty())
                        startForegroundService(str)
                }
                ACTION_RESUME_SERVICE -> {
                    resumeService()
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun resumeService() {
        mPlayer.seekTo(length)
        mPlayer.start()

        isPlaying.postValue(true)
    }
    private fun pauseService() {
        mPlayer.pause()
        length = mPlayer.currentPosition

        isPlaying.postValue(false)
    }

    private fun startForegroundService(str: String) {
        mPlayer = MediaPlayer.create(this, Uri.parse(str))
        isPlaying.postValue(true)
        mPlayer.start()
        startTimer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager!!)
        }

        startForeground(
            NOTIFICATION_ID,
            baseNotificationBuilder.setProgress(mPlayer.duration, mPlayer.currentPosition, false)
                .build()
        )

        time.observe(this, {
            if (!serviceKilled) {
                notificationManager!!.notify(NOTIFICATION_ID, baseNotificationBuilder
                    .setProgress(mPlayer.duration, mPlayer.currentPosition, false)
                    .build())
            }
        })
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notificationActionText = if (isPlaying) "Pause" else "Resume"

        val pendingIntent = if (isPlaying) {
            val pauseIntent = Intent(this, PlayService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, PlayService::class.java).apply {
                action = ACTION_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }


        baseNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(baseNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        if (!serviceKilled) {
            if(!isPlaying) {
                baseNotificationBuilder
                    .addAction(R.drawable.ic_play, notificationActionText, pendingIntent)
                    .setProgress(mPlayer.duration, mPlayer.duration, false)
            } else {
                baseNotificationBuilder
                    .addAction(R.drawable.ic_pause, notificationActionText, pendingIntent)
                    .setProgress(mPlayer.duration, mPlayer.duration, false)
            }
            notificationManager?.notify(NOTIFICATION_ID, baseNotificationBuilder.build())
        }
    }

    private fun startTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            while (mPlayer.currentPosition < mPlayer.duration) {
                time.postValue(mPlayer.currentPosition)
                delay(600)
            }
        }
    }

    private fun killService() {
        serviceKilled = true
        mPlayer.stop()
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}