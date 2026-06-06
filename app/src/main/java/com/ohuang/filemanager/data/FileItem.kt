package com.ohuang.filemanager.data

import com.google.gson.annotations.SerializedName

data class FileItem(
    @SerializedName("name") val name: String,
    @SerializedName("length") val length: Long,
    @SerializedName("isFolder") val isFolder: Boolean,
    @SerializedName("lastModified") val lastModified: Long
) {
    fun getFileName(): String {
        val parts = name.split("/")
        return parts.last()
    }

    /**
     * 是否超出文本编辑的大小
     */
    fun isWithinTextEditorLimit(): Boolean{
        return length>=1024*1024
    }

    fun formatSize(): String {
        if (length == 0L) return if (isFolder) "—" else "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var size = length.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}" 
               else "%.1f %s".format(size, units[unitIndex])
    }

    fun formatDate(): String {
        if (lastModified == 0L) return "—"
        val now = System.currentTimeMillis()
        val diff = now - lastModified
        
        if (diff < 60000) return "刚刚"
        if (diff < 3600000) return "${diff / 60000}分钟前"
        if (diff < 86400000) return "${diff / 3600000}小时前"
        if (diff < 604800000) return "${diff / 86400000}天前"
        
        val date = java.util.Date(lastModified)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}