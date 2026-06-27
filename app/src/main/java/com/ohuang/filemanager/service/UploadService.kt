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
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.ohuang.filemanager.R
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.statedata.StateData
import com.ohuang.filemanager.util.UriToFile
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.call.await
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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
            val successFiles = mutableListOf<String>()
            val failFiles = mutableListOf<String>()

            for ((index, fileUri) in fileUris.withIndex()) {
                if (!isUploading.value) {
                    break
                }
                if (isCannel) {
                    break
                }
                val fileName = getFileName(uri = fileUri)

                var file: FileInputStream?=null
                try {
                    file = UriToFile.uriToFileInputStream(fileUri, this@UploadService)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                if (file == null) {
                    failFiles.add("$fileName--file is null")
                    continue
                }
                val num = index + 1


                liveData.postValue("正在上传 ($num/${fileUris.size}): $fileName")
                showProgress("正在上传 ($num/${fileUris.size}): $fileName")
                var lastUpdateTime = 0L
                call = ApiService.uploadFile(file = file, fileName = fileName,path= path) { current, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime > 500 || current == total) {
                        lastUpdateTime= System.currentTimeMillis()
                        val s =
                            "${(current / (1024 * 1024 * 1.0f)).toFixed()}MB/${(total / (1024 * 1024 * 1.0f)).toFixed()}MB"
                        val progress =
                            "正在上传 ($num/${fileUris.size}): $fileName \n上传中:${(current * 100 / total)}%  $s"
                        liveData.postValue(progress)
                    }

                }

                try {
                    val result = call?.await()
                    call == null
                    successFiles.add("$fileName--$result")
                } catch (e: Throwable) {
                    failFiles.add((fileName + "--" + e.message))
                }



            }

            delay(10)
            clearFileCheche()
            isCannel = false
            isUploading.postValue(false)
            val message = StringBuilder().apply {
                append("上传完成,共${fileUris.size}个文件\n")

                if (failFiles.isNotEmpty()) {
                    append("失败${failFiles.size}个文件 :" + failFiles + "\n")
                }
                if (successFiles.isNotEmpty()) {
                    append("成功${successFiles.size}个文件 :" + successFiles)
                }


            }.toString()
            liveData.postValue(message)
            showProgress(message)
        }
    }


    private fun getFileName(uri: Uri): String {
        var name: String? = null
        try {

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
            if (name==null){
                val strs = uri.toString().split("/")
                name=strs.lastOrNull()
            }
        }catch (e: Throwable){

        }finally {

        }

        return name?:"未知文件_${System.currentTimeMillis()}"
    }

    fun clearFileCheche() {
        try {
            val cacheDir1 = UriToFile.copyChecheDir(applicationContext)
            cacheDir1.deleteRecursively()
        } catch (e: Throwable) {

        }
    }

}
