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
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.statedata.StateData
import com.ohuang.filemanager.util.UriToFile
import com.ohuang.kthttp.call.HttpCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.jvm.Throws

class UploadService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifaction()
            liveData.observeForever {
                if (it.isNotEmpty()){
                    showProgress(it)
                }
            }
        }
    }

    private val CHANNEL_ID2 = "Channel2"
    var notifId_1 = 0x1
    var binder = DownUpBinder()
    var liveData = MutableLiveData<String>("")
    var isUploading: StateData<Boolean> = StateData(false)

    var call: HttpCall<String>?= null

    private fun notifaction() {
        createNotificationChannel()
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("当前没有要上传的任务").setContentTitle("上传进度")
        val notification = repliedNotification.build()
        startForeground(notifId_1, notification)
    }

    private fun showProgress(text: String) {
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(text).setContentTitle("上传进度")
        val notification = repliedNotification.build()
        startForeground(notifId_1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID2, "显示上传进度", importance)
            channel.importance = NotificationManager.IMPORTANCE_HIGH
            channel.enableLights(false)
            channel.setSound(null, null)
            channel.enableVibration(false)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    inner class DownUpBinder : Binder() {
        fun startUpLoad(uri: Uri,path: String) {
            isUploading.value?.let {
                if (!it) {
                    upload(uri,path)
                }
            }
        }

        fun stopUpLoad(){
            call?.cancel()
        }

        fun isUpload() = isUploading



        fun getLivedata() = liveData
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    fun Number.toFixed(): String{
        return "%.2f".format(this@toFixed)
    }

    fun upload(fileUri: Uri,path: String) {
        isUploading.value = true
        showProgress("正在准备上传")
        liveData.postValue("正在准备上传")

        GlobalScope.launch(Dispatchers.IO) {

            var file: File? =null
            try {
                file = UriToFile.uriToFile(fileUri, this@UploadService)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            if (file == null) {
                isUploading.postValue(false)
                showProgress("获取文件失败,请检查权限和文件")
                liveData.postValue("获取文件失败,请检查权限和文件")
                return@launch
            }

            call= ApiService.uploadFile(file,path) { current, total ->
                val s = "${(current / (1024 * 1024 * 1.0f)).toFixed()}MB/${(total / (1024 * 1024 * 1.0f)).toFixed()}MB"
                val progress = "上传中:${(current * 100 / total)}%  $s"
                liveData.postValue(progress)


            }
            call?.request({ error ->
                call=null
                GlobalScope.launch(Dispatchers.Main) {
                    isUploading.postValue(false)
                    liveData.postValue("上传失败: ${error.message}")
                }
            }) { _ ->
                call=null
                GlobalScope.launch(Dispatchers.Main) {
                    liveData.postValue("上传任务完成")
                    isUploading.value = false
                }
            }
        }



    }
}
