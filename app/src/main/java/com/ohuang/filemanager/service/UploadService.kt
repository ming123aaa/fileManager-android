package com.ohuang.filemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.ohuang.filemanager.R
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.data.DownloadTask
import com.ohuang.filemanager.statedata.StateData
import com.ohuang.filemanager.util.UriToFile
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.call.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

data class UploadFileInfo(val uri: Uri, val relativePath: String)

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

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = this@UploadService.packageName
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifaction() {
        createNotificationChannel()
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.mipmap.icon_app)
            .setContentText("当前没有要上传的任务").setContentTitle("上传进度")
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setOngoing(true)

        val notification = repliedNotification.build()
        startForeground(notifId_1, notification)
    }

    private fun showProgress(text: String) {
        val repliedNotification = NotificationCompat.Builder(this, CHANNEL_ID2)
        repliedNotification.setSmallIcon(R.mipmap.icon_app)
            .setContentText(text).setContentTitle("上传进度")
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setOngoing(true)
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

            // 先将 tree URI 展开为文件信息列表（包含相对路径）
            liveData.postValue("正在扫描文件夹...")
            val allFileInfos = mutableListOf<UploadFileInfo>()
            for (uri in fileUris) {
                if (DocumentsContract.isTreeUri(uri)) {
                    collectFilesFromTree(uri, allFileInfos)
                } else {
                    allFileInfos.add(UploadFileInfo(uri, ""))
                }
            }

            var lastUpdateTime = 0L

            for ((index, fileInfo) in allFileInfos.withIndex()) {
                if (!isUploading.value) {
                    break
                }
                if (isCannel) {
                    break
                }
                val fileName = getFileName(uri = fileInfo.uri)

                var file: FileInputStream?=null
                try {
                    file = UriToFile.uriToFileInputStream(fileInfo.uri, this@UploadService)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                val num = index + 1

                if (file == null) {
                    failFiles.add(fileInfo.relativePath+"/"+fileName+"--file is null")
                    continue
                }

                val uploadPath = if (fileInfo.relativePath.isEmpty()) path else "$path/${fileInfo.relativePath}"

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime > 500 ) {
                    lastUpdateTime= System.currentTimeMillis()
                    liveData.postValue("正在上传 ($num/${allFileInfos.size}): $fileName \n准备上传")
                    showProgress("正在上传 ($num/${allFileInfos.size}): $fileName")
                }

                call = ApiService.uploadFile(file = file, fileName = fileName, path = uploadPath) { current, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime > 500 ) {
                        lastUpdateTime= System.currentTimeMillis()
                        val s =
                            "${DownloadTask.formatBytes(current)}/${DownloadTask.formatBytes(total )}"
                        val progress =
                            "正在上传 ($num/${allFileInfos.size}): $fileName \n上传中:${(current * 100 / total)}%  $s"
                        liveData.postValue(progress)
                    }

                }

                try {
                    val result = call?.await()
                    call = null
                    successFiles.add("$fileName--$result")
                } catch (e: Throwable) {
                    failFiles.add((fileInfo.relativePath+"/"+fileName + "--" + e.message))
                }finally {
                    clearFileCheche()
                }


            }

            delay(10)

            isCannel = false
            isUploading.postValue(false)
            val message = StringBuilder().apply {
                append("上传完成,共${allFileInfos.size}个文件\n")

                if (failFiles.isNotEmpty()) {
                    append("失败${failFiles.size}个文件 :" + failFiles + "\n")
                }
                if (successFiles.isNotEmpty()) {
                    append("成功${successFiles.size}个文件" )
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
            if (name == null) {
                val strs = uri.toString().split("/")
                name = strs.lastOrNull()?.let { Uri.decode(it) }
            }
        } catch (e: Throwable) {
        }

        return name ?: "未知文件_${System.currentTimeMillis()}"
    }

    fun clearFileCheche() {
        try {
            val cacheDir1 = UriToFile.copyChecheDir(applicationContext)
            cacheDir1.deleteRecursively()
        } catch (e: Throwable) {

        }
    }

    /** 使用文件遍历方式收集目录树中的所有文件信息（包含相对路径） */
    private fun collectFilesFromTree(treeUri: Uri, result: MutableList<UploadFileInfo>) {
        val rootPath = uriToPath(treeUri)
        if (rootPath != null) {
            val rootDir = File(rootPath)
            if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val fullRelativePath = file.absolutePath.removePrefix(rootDir.absolutePath).removePrefix("/")
                        val dirPath = fullRelativePath.substringBeforeLast("/", "")
                        val relativePath = if (dirPath.isEmpty()) rootDir.name else "${rootDir.name}/$dirPath"
                        result.add(UploadFileInfo(Uri.fromFile(file), relativePath))
                    }
                }
                return
            }
        }
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDisplayName = queryDocumentDisplayName(treeUri, rootDocId)
        collectFilesRecursiveFallback(treeUri, rootDocId, rootDisplayName, result)
    }

    private fun uriToPath(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory().absolutePath}/${Uri.decode(split[1])}"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDocumentDisplayName(treeUri: Uri, docId: String): String {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0) ?: extractNameFromDocId(docId)
            } else {
                extractNameFromDocId(docId)
            }
        } catch (e: Exception) {
            extractNameFromDocId(docId)
        } finally {
            cursor?.close()
        }
    }

    private fun extractNameFromDocId(docId: String): String {
        return docId.split(":").lastOrNull()?.substringAfterLast("/")?.let { Uri.decode(it) } ?: "unknown"
    }

    private fun collectFilesRecursiveFallback(treeUri: Uri, docId: String, relativePath: String, result: MutableList<UploadFileInfo>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            if (cursor == null) return

            while (cursor.moveToNext()) {
                val childDocId = cursor.getString(0)
                val mimeType = cursor.getString(1)
                val displayName = cursor.getString(2) ?: "unknown"
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)

                if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                    val newRelativePath = if (relativePath.isEmpty()) displayName else "$relativePath/$displayName"
                    collectFilesRecursiveFallback(treeUri, childDocId, newRelativePath, result)
                } else {
                    result.add(UploadFileInfo(childUri, relativePath))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }

}
