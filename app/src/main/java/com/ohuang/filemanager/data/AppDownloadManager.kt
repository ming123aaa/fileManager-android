package com.ohuang.filemanager.data

import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.ohuang.filemanager.server.util.AppContext
import com.ohuang.filemanager.util.SPUtil
import com.ohuang.kthttp.call.awaitOrNull
import com.ohuang.kthttp.call.throwCancellationException
import com.ohuang.kthttp.httpCallRetryOnFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.LinkedList

object AppDownloadManager {

    private const val MAX_CONCURRENT_DOWNLOADS = 5

    private val tasksLock = Any()

    /** 任务表：key = task.id，O(1) 查找 */
    val tasks: SnapshotStateMap<Long, DownloadTask> = mutableStateMapOf()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadingFiles = HashSet<String>()
    private val activeJobs = mutableMapOf<Long, Job>()


    /** 是否断点续传（跳过已下载完成的文件） */
    private val _isContinueDownload =
        MutableStateFlow(SPUtil.get(AppContext.instance, "_isContinueDownload", true) as Boolean)
    val isContinueDownload: StateFlow<Boolean> = _isContinueDownload

    /** 下载间隔 */
    private val _downloadInterval =
        MutableStateFlow(SPUtil.get(AppContext.instance, "_downloadInterval", 50L) as Long)
    val downloadInterval: StateFlow<Long> = _downloadInterval

    fun setDownloadInterval(time: Long) {
        var fTime = 50L
        if (time >= 0 && time <= 1000) {
            fTime = time
        }
        SPUtil.put(AppContext.instance, "_downloadInterval", fTime)
        _downloadInterval.value = fTime
    }

    fun setContinueDownload(value: Boolean) {
        SPUtil.put(AppContext.instance, "_isContinueDownload", value)
        _isContinueDownload.value = value
    }

    /** 全局暂停标志：暂停所有后新任务不会自动开始下载 */
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    // 等待队列：只传递任务id，worker从列表取最新数据
    private val pendingChannel = Channel<Long>(Channel.UNLIMITED)

    init {
        // 启动固定数量的 worker，从等待队列取任务执行
        repeat(MAX_CONCURRENT_DOWNLOADS) {
            scope.launch {
                for (taskId in pendingChannel) {
                    val task = tasks[taskId]

                    if (task != null && task.status == DownloadTask.Status.WAITING) {
                        doDownload(taskId)
                    }
                }
            }
        }
    }

    private fun getDownloadDir(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "fileManager"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 创建下载任务并放入等待队列
     */
    fun downloadFile(
        serverPath: String,
        fileName: String,
        totalSize: Long = 0L
    ): Boolean {
        val task = synchronized(tasksLock) {
            val exists = tasks.values.any { it.fileName == fileName }
            if (exists) return false


            val localFile = File(getDownloadDir(), fileName)

            DownloadTask(

                fileName = fileName,
                serverPath = serverPath,
                localFile = localFile,
                totalSize = totalSize,
                status = DownloadTask.Status.WAITING
            ).also { addTask(it) }
        }
        // 全局暂停时只入队不触发下载，等 resumeAll 时再统一调度
        if (!_isPaused.value) {
            pendingChannel.trySend(task.id)
        }
        return true
    }

    /**
     * 下载文件夹：创建单个任务，扫描文件后统一下载
     */
    fun downloadFolder(
        folderServerPath: String,
        folderName: String
    ): Boolean {
        val exists = tasks.values.any { it.fileName == folderName }
        if (exists) return false

        val localDir = File(getDownloadDir(), folderName)
        val task = DownloadTask(
            fileName = folderName,
            serverPath = folderServerPath,
            localFile = localDir,
            totalSize = 0L,
            status = DownloadTask.Status.WAITING,
            isFolder = true
        ).also { addTask(it) }

        if (!_isPaused.value) {
            pendingChannel.trySend(task.id)
        }
        return true
    }


    /**
     * 递归扫描文件夹，返回所有文件信息（serverPath, localFile, size）
     */
    private suspend fun scanFolder(
        serverPath: String,
        localDir: File,
        findFolderError: (serverPath: String) -> Boolean,
        fileCall: suspend (serverPath: String, file: File, size: Long) -> Unit
    ): Boolean {
        var isError = false
        coroutineScope {

            val files = httpCallRetryOnFailure(time = 100) { ApiService.getAllFiles(serverPath) }
            if (files == null) {
                isError = findFolderError(serverPath)
                return@coroutineScope

            }
            for (item in files) {

                if (!isActive) {
                    return@coroutineScope
                }
                val itemServerPath = if (serverPath.isEmpty()) item.name
                else "$serverPath/${item.name}"

                if (item.isFolder) {
                    val subDir = File(localDir, item.name)
                    if (!subDir.exists()) subDir.mkdirs()
                    delay(_downloadInterval.value)
                    if (!isActive) {
                        return@coroutineScope
                    }

                    isError = scanFolder(itemServerPath, subDir, findFolderError, fileCall)
                    if (isError) {
                        return@coroutineScope
                    }
                } else {
                    val file = File(localDir, item.name)

                    fileCall(itemServerPath, file, item.length)
                }
            }

        }
        return isError
    }

    /**
     * worker 实际执行下载
     */
    private suspend fun doDownload(taskId: Long) {
        // 原子操作：只有当前状态为 WAITING 时才设置为 DOWNLOADING，防止与 pauseAll 竞态
        updateTaskIf(taskId, { it.status == DownloadTask.Status.WAITING }) {
            it.copy(status = DownloadTask.Status.DOWNLOADING)
        }
        val currentTask = tasks[taskId]
        if (currentTask?.status != DownloadTask.Status.DOWNLOADING) return


        val job = scope.launch {

            try {


                val task = tasks[taskId] ?: return@launch

                if (task.isFolder) {
                    doDownloadFolder(taskId, task)
                } else {
                    doDownloadFile(taskId, task)
                }
            } catch (e: CancellationException) {
                updateTaskIf(taskId, { true }) { it.copy(status = DownloadTask.Status.PAUSED) }
            } catch (e: Exception) {
                if (isActive) {
                    updateTaskIf(taskId, { true }) {
                        it.copy(
                            status = DownloadTask.Status.FAILED,
                            errorMessage = e.message ?: "未知错误"
                        )
                    }
                } else {
                    updateTaskIf(taskId, { true }) { it.copy(status = DownloadTask.Status.PAUSED) }
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
        job.join()
    }

    private suspend fun download(
        url: String,
        file: File,
        isContinueDownload: Boolean, //是否断点下载
        error: (msg: String) -> Unit = {},
        onProcess: (current: Long, total: Long) -> Unit = { _, _ -> }
    ): File? {

        val isDownloaded: Boolean = synchronized(lock = downloadingFiles) {
            if (downloadingFiles.contains(file.absolutePath)) {  //如果当前文件已在下载队列
                error("文件已在下载队列了")
                true
            } else {
                downloadingFiles.add(file.absolutePath)
                false
            }
        }

        if (isDownloaded) return null
        try {
            delay(_downloadInterval.value)
            return ApiService.download(
                url = url,
                file = file,
                isContinueDownload = isContinueDownload,
                onProcess = onProcess
            ).awaitOrNull()
        } catch (e: Throwable) {
            throwCancellationException(e)
        } finally {
            synchronized(lock = downloadingFiles) {
                downloadingFiles.remove(file.absolutePath)
            }
        }

        return null
    }

    /**
     * 下载单个文件
     */
    private suspend fun doDownloadFile(taskId: Long, task: DownloadTask) {
        coroutineScope {
            var totalSize = task.totalSize
            val url = if (task.serverPath.startsWith("http://")
                || task.serverPath.startsWith("https://")
            ) {
                task.serverPath
            } else {
                ApiService.getDownloadPath(task.serverPath, false)
            }

            if (totalSize <= 0) {

                val fileInfo = ApiService.checkDownloadPath(url).awaitOrNull()
                totalSize = fileInfo?.contentLength ?: 0L
            }


            var lastUpdateTime = 0L

            var msg: String = "下载失败"
            val result = download(
                url = url,
                file = task.localFile,
                isContinueDownload = _isContinueDownload.value,
                error = { msg = it }
            ) { current, total ->
                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 500 || current == total || current == 0L) {
                    lastUpdateTime = now
                    updateTask(taskId) {
                        it.copy(
                            downloadedSize = current,
                            totalSize = if (total > 0) total else totalSize
                        )
                    }
                }
            }

            if (isActive) {
                if (result != null) {
                    updateTaskIf(taskId, { true }) {
                        it.copy(
                            status = DownloadTask.Status.COMPLETED,
                            downloadedSize = totalSize,
                            totalSize = totalSize
                        )
                    }
                } else {
                    updateTaskIf(taskId, { true }) {
                        it.copy(status = DownloadTask.Status.FAILED, errorMessage = msg)
                    }
                }
            } else {
                updateTaskIf(taskId, { true }) { it.copy(status = DownloadTask.Status.PAUSED) }
            }
        }
    }

    /**
     * 下载文件夹：扫描后逐个文件下载，聚合进度到单个任务
     */
    private suspend fun doDownloadFolder(taskId: Long, task: DownloadTask) {
        coroutineScope {

            val fileInfos = LinkedList<Triple<String, File, Long>>()
            var totalSize: Long = 0
            var lastUpdateTime: Long = 0


            // 扫描文件夹获取所有文件
            val isError=scanFolder(
                task.serverPath,
                task.localFile, { errorFolder->

                    updateTask(taskId) {
                        it.copy(
                            status = DownloadTask.Status.FAILED,
                            totalFiles = fileInfos.size,
                            totalSize = totalSize,
                            errorMessage = "获取文件夹失败  dirPath = $errorFolder",
                            downloadedSize = 0,
                            completedFiles = 0,
                        )
                    }
                    return@scanFolder true


                }
            ) { serverPath: String, file: File, size: Long ->
                fileInfos.add(Triple<String, File, Long>(serverPath, file, size))
                totalSize += size
                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 500) {
                    lastUpdateTime = System.currentTimeMillis()
                    updateTask(taskId) {
                        it.copy(
                            totalFiles = fileInfos.size,
                            totalSize = totalSize,

                            downloadedSize = 0,
                            completedFiles = 0,
                        )
                    }
                }
            }
            if (isError){
                return@coroutineScope
            }
            if (!isActive) {
                updateTaskIf(taskId, { true }) {
                    it.copy(
                        status = DownloadTask.Status.PAUSED,
                        totalFiles = fileInfos.size,
                        totalSize = totalSize,

                        completedFiles = 0, downloadedSize = 0,

                        )
                }
            }
            if (fileInfos.isEmpty()) {
                updateTaskIf(taskId, { true }) {
                    it.copy(
                        status = DownloadTask.Status.COMPLETED,
                        totalFiles = fileInfos.size,
                        totalSize = totalSize,

                        completedFiles = 0, downloadedSize = 0,

                        )
                }
                return@coroutineScope
            }



            updateTask(taskId) {
                it.copy(totalFiles = fileInfos.size, totalSize = totalSize)
            }

            var downloadedBytes = task.downloadedSize
            var completedFiles = task.completedFiles

            for (index in completedFiles until fileInfos.size) {
                val (serverPath, localFile, fileSize) = fileInfos[index]
                if (!isActive) {
                    updateTaskIf(taskId, { true }) {
                        it.copy(
                            status = DownloadTask.Status.PAUSED,
                            downloadedSize = downloadedBytes,
                            completedFiles = completedFiles
                        )
                    }
                    return@coroutineScope
                }

                // 确保父目录存在
                localFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

                val url = ApiService.getDownloadPath(serverPath, false)

                var msg: String = "下载失败"
                val result = download(
                    url = url,
                    file = localFile,
                    isContinueDownload = _isContinueDownload.value,
                    error = { msg = it }
                ) { current, _ ->
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 500) {
                        lastUpdateTime = now
                        updateTask(taskId) {
                            it.copy(downloadedSize = downloadedBytes + current,completedFiles = completedFiles)
                        }
                    }
                }

                if (!isActive) {
                    updateTaskIf(taskId, { true }) {
                        it.copy(
                            status = DownloadTask.Status.PAUSED,
                            downloadedSize = downloadedBytes,
                            completedFiles = completedFiles
                        )
                    }
                    return@coroutineScope
                }

                if (result == null) {
                    updateTaskIf(taskId, { true }) {
                        it.copy(
                            status = DownloadTask.Status.FAILED,
                            errorMessage = "$msg url=$url",
                            downloadedSize = downloadedBytes,
                            completedFiles = completedFiles
                        )
                    }
                    return@coroutineScope
                }

                downloadedBytes += fileSize
                completedFiles++
                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 500) {
                    lastUpdateTime = now
                    updateTask(taskId) {
                        it.copy(downloadedSize = downloadedBytes, completedFiles = completedFiles)
                    }
                }
            }


            updateTaskIf(taskId, { true }) {
                it.copy(
                    status = DownloadTask.Status.COMPLETED,
                    downloadedSize = totalSize,
                    completedFiles = fileInfos.size,
                    totalFiles = fileInfos.size
                )
            }
        }
    }

    private fun addTask(task: DownloadTask) {
        synchronized(tasksLock) {
            val existing = tasks[task.id]
            if (existing != null && existing.status in listOf(
                    DownloadTask.Status.COMPLETED,
                    DownloadTask.Status.FAILED,
                    DownloadTask.Status.PAUSED
                )
            ) {
                tasks.remove(task.id)
            }
            tasks[task.id] = task
        }
    }

    private fun updateTask(id: Long, update: (DownloadTask) -> DownloadTask) {
        synchronized(tasksLock) {
            val current = tasks[id] ?: return
            tasks[id] = update(current)
        }
    }

    private inline fun updateTaskIf(
        id: Long,
        predicate: (DownloadTask) -> Boolean,
        crossinline update: (DownloadTask) -> DownloadTask
    ) {
        synchronized(tasksLock) {
            val current = tasks[id] ?: return
            if (predicate(current)) {
                tasks[id] = update(current)
            }
        }
    }

    fun cancelDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status != DownloadTask.Status.COMPLETED }) {
            it.copy(status = DownloadTask.Status.PAUSED)
        }
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
    }

    fun pauseDownload(taskId: Long) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTaskIf(taskId, { it.status != DownloadTask.Status.COMPLETED }) {
            it.copy(status = DownloadTask.Status.PAUSED)
        }
    }

    fun resumeDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status == DownloadTask.Status.PAUSED }) {
            it.copy(status = DownloadTask.Status.WAITING)
        }
        val task = tasks[taskId]
        if (task?.status == DownloadTask.Status.WAITING) {
            pendingChannel.trySend(taskId)
        }
    }

    fun pauseAll() {
        _isPaused.value = true
        drainPendingChannel()
        val toPause = mutableListOf<Long>()
        synchronized(tasksLock) {
            for ((id, task) in tasks) {
                if (task.status == DownloadTask.Status.DOWNLOADING || task.status == DownloadTask.Status.WAITING) {
                    toPause.add(id)
                }
            }
            toPause.forEach { id ->
                val current = tasks[id]
                if (current != null) {
                    tasks[id] = current.copy(status = DownloadTask.Status.PAUSED)
                }
            }
        }
        toPause.forEach { id ->
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
        }
    }

    fun resumeAll() {
        _isPaused.value = false
        val toResume = mutableListOf<Long>()
        synchronized(tasksLock) {
            for ((id, task) in tasks) {
                if (task.status == DownloadTask.Status.PAUSED) {
                    toResume.add(id)
                }
            }
            toResume.forEach { id ->
                val current = tasks[id]
                if (current != null) {
                    tasks[id] = current.copy(status = DownloadTask.Status.WAITING)
                }
            }
        }
        toResume.forEach { id ->
            val updated = tasks[id]
            if (updated?.status == DownloadTask.Status.WAITING) {
                pendingChannel.trySend(id)
            }
        }
    }

    fun retryDownload(taskId: Long) {
        updateTaskIf(
            taskId,
            { it.status == DownloadTask.Status.FAILED || it.status == DownloadTask.Status.PAUSED }
        ) {
            it.copy(
                status = DownloadTask.Status.WAITING,
                downloadedSize = 0L,
                errorMessage = null,
                completedFiles = 0
            )
        }
        val task = tasks[taskId]
        if (task?.status == DownloadTask.Status.WAITING) {
            pendingChannel.trySend(taskId)
        }
    }

    fun removeTask(taskId: Long) {
        cancelDownload(taskId)
        synchronized(tasksLock) {
            tasks.remove(taskId)
        }
    }

    fun clearCompleted() {
        synchronized(tasksLock) {
            tasks.entries.removeAll { it.value.status == DownloadTask.Status.COMPLETED }
        }
    }

    fun clearTasks(taskIds: List<Long>) {
        val idSet = taskIds.toSet()
        for (id in taskIds) {
            cancelDownload(id)
        }
        synchronized(tasksLock) {
            idSet.forEach { tasks.remove(it) }
        }
    }

    fun isDownloading(taskId: Long): Boolean {
        return activeJobs[taskId]?.isActive == true
    }

    /**
     * 排空等待队列中所有残留的 task ID
     */
    private fun drainPendingChannel() {
        while (pendingChannel.tryReceive().isSuccess) { /* drain */
        }
    }
}
