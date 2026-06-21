package com.ohuang.filemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.FileUtils
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.ohuang.filemanager.R
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.statedata.StateData
import com.ohuang.filemanager.util.UriToFile
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.jvm.Throws

class UploadService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifaction()
            liveData.observeForever {
                if (it.isNotEmpty()) {
                    showProgress(it)
                }
            }
        }
    }

    private val CHANNEL_ID2 = "Channel2"
    var notifId_1 = 0x1
    var binder = DownUpBinder()

    companion object {
        val liveData = MutableLiveData<String>("")
        val isUploading: StateData<Boolean> = StateData(false)
    }


    var call: HttpCall<String>? = null
    var isCannel: Boolean = false

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
        fun startUpLoad(uri: Uri, path: String) {
            isUploading.value?.let {
                if (!it) {
                    upload(listOf(uri), path)
                }
            }
        }

        fun startMultiUpload(uris: List<Uri>, path: String) {
            isUploading.value?.let {
                if (!it) {
                    upload(uris, path)
                }
            }
        }

        fun stopUpLoad() {
            isCannel = true
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

    fun Number.toFixed(): String {
        return "%.2f".format(this@toFixed)
    }

    fun upload(fileUris: List<Uri>, path: String) {
        if (isUploading.value) {
            return
        }
        isUploading.value = true
        showProgress("正在准备上传")
        liveData.postValue("正在准备上传")

        GlobalScope.launch(Dispatchers.IO) {
            var successCount = 0
            var failCount = 0

            for ((index, fileUri) in fileUris.withIndex()) {
                if (!isUploading.value) {
                    break
                }
                if (isCannel) {
                    break
                }

                var file: File? = null
                try {
                    file = UriToFile.uriToFile(fileUri, this@UploadService)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                if (file == null) {
                    failCount++
                    continue
                }
                val num = index + 1

                val fileName = file.name
                liveData.postValue("正在上传 ($num/${fileUris.size}): $fileName")
                showProgress("正在上传 ($num/${fileUris.size}): $fileName")

                call = ApiService.uploadFile(file, path) { current, total ->
                    val s =
                        "${(current / (1024 * 1024 * 1.0f)).toFixed()}MB/${(total / (1024 * 1024 * 1.0f)).toFixed()}MB"
                    val progress =
                        "正在上传 ($num/${fileUris.size}): $fileName \n上传中:${(current * 100 / total)}%  $s"
                    liveData.postValue(progress)
                }

                val result = call?.awaitOrNull()


                if (result != null && result.isNotBlank()) {
                    successCount++
                } else {
                    failCount++
                }


            }
            withContext(Dispatchers.IO) {
                clearFileCheche()
            }

            isCannel = false
            isUploading.postValue(false)
            val message = when {
                failCount == 0 -> "全部上传完成，成功 $successCount 个"
                successCount == 0 -> "上传失败，共失败 $failCount 个"
                else -> "上传完成，成功 $successCount 个，失败 $failCount 个"
            }
            liveData.postValue(message)
            showProgress(message)
        }
    }

    fun clearFileCheche() {
        try {
            val cacheDir1 = UriToFile.copyChecheDir(applicationContext)
            cacheDir1.deleteRecursively()
        } catch (e: Throwable) {

        }
    }

}
