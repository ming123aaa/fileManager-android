package com.ohuang.filemanager.data

import java.io.File

data class DownloadTask(
    val incrementID: Long= createIncrementId(),
    val fileName: String,
    val serverPath: String,
    val localFile: File,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: Status = Status.WAITING,
    val errorMessage: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    val isFolder: Boolean = false,
    val completedFiles: Int = 0,
    val totalFiles: Int = 0
) {
    val id: Long
        get() = incrementID

    enum class Status {
        WAITING,    // 等待下载
        DOWNLOADING, // 下载中
        PAUSED,     // 已暂停
        COMPLETED,  // 下载完成
        FAILED,     // 下载失败

    }

    val progress: Float
        get() = if (totalSize > 0) (downloadedSize.toFloat() / totalSize).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    fun formatDownloadedSize(): String = formatBytes(downloadedSize)
    fun formatTotalSize(): String = formatBytes(totalSize)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes == 0L) return "0 B"
            val units = listOf("B", "KB", "MB", "GB", "TB")
            var size = bytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
            else "%.1f %s".format(size, units[unitIndex])
        }

        private var count=0L //自增id

        /**
         * 创建自增id
         */
        fun createIncrementId(): Long{
            return count++
        }


    }
}
