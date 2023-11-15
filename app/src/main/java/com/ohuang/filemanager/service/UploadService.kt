package com.ohuang.filemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData


import com.ohuang.filemanager.R
import com.ohuang.filemanager.config.Http
import com.ohuang.filemanager.util.UriToFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toObservable

import java.io.File
import java.lang.Exception

class UploadService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifaction() //启动前台通知
        }
    }


    //通知
    private val CHANNEL_ID2 = "Channel2"
    var notifId_1 = 0x1
    var binder = DownUpBinder()
    var liveData = MutableLiveData<String>()

    //通知
    private fun notifaction() {
        createNotificationChannel() //创建通道
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("当前没有要上传的任务").setContentTitle("上传进度")
        val notification = repliedNotification.build()
        startForeground(notifId_1, notification)
    }

    private fun showProgress(i: String) {
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(i).setContentTitle("上传进度")
        val notification = repliedNotification.build()
        startForeground(notifId_1, notification)
    }

    //通知通道注册
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID2, "显示上传进度", importance)
            channel.importance = NotificationManager.IMPORTANCE_HIGH //重要性
            channel.enableLights(false)
            channel.setSound(null, null)
            channel.enableVibration(false)
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    inner class DownUpBinder : Binder() {
        fun startUpLoad(uri: Uri) {
            if (!isUploading) {
                upload2(uri)
                isUploading = true

            }
        }

        fun isUpload() = isUploading

        fun getLivedata() = liveData


    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    var isUploading = false

    fun upload2(fileUri: Uri) {
        rxhttpUpload(fileUri)
    }

    private fun rxhttpUpload(fileUri: Uri) {
        GlobalScope.launch {


            showProgress("正在准备上传")
            liveData.postValue("正在准备上传")
            RxHttp.postForm(Http.Main.FileUpload())
                .addPart(this@UploadService, "fileName", fileUri)
                .toObservable<String>()
                .onMainProgress {//this为当前协程CoroutineScope对象，用于控制回调线程
                    //上传进度回调,0-100，仅在进度有更新时才会回调
                    val currentProgress = it.progress //当前进度 0-100
                    val currentSize = it.currentSize  //当前已上传的字节大小
                    val totalSize = it.totalSize
                    val s =
                        "" + (currentSize / (1024 * 1024 * 1.0f)) + "MB/" + (totalSize / (1024 * 1024 * 1.0f)) + "MB"
                    val progress = "当前进度为$currentProgress%  $s"
                    showProgress(progress)
                    liveData.postValue(progress)
                }.subscribe({
                    showProgress("上传任务完成")
                    liveData.postValue("上传任务完成")
                    isUploading = false
                }, {
                    isUploading = false
                    showProgress("上传失败" + it)
                    liveData.postValue("上传失败" + it)
                })


        }

    }


}