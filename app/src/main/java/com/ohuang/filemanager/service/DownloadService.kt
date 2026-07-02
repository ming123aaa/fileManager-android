package com.ohuang.filemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ohuang.filemanager.R
import com.ohuang.filemanager.data.AppDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val CHANNEL_ID = "DownloadChannel"
    private val NOTIF_ID = 0x2
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastNotifTime = 0L
    private val NOTIF_MIN_INTERVAL = 500L
    private var hasReceivedFirstUpdate = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showProgress("正在准备下载")

        // 立即用当前状态更新通知，不等协程启动
        val currentMsg = AppDownloadManager.progressMessage.value
        if (currentMsg.isNotEmpty()) {
            showProgress(currentMsg)
            hasReceivedFirstUpdate = true
        }

        scope.launch {
            AppDownloadManager.progressMessage.collect { message ->
                if (message.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    if (now - lastNotifTime >= NOTIF_MIN_INTERVAL || message.contains("完成") || !hasReceivedFirstUpdate) {
                        lastNotifTime = now
                        hasReceivedFirstUpdate = true
                        showProgress(message)
                    }
                }
            }
        }


    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = this@DownloadService.packageName
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "显示下载进度", NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(false)
            channel.setSound(null, null)
            channel.enableVibration(false)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgress(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon_app)
            .setContentText(text)
            .setContentTitle("下载进度")
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        AppDownloadManager.notifyDownloadServiceStopped()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
}
