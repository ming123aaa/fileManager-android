package com.ohuang.filemanager.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 将本地文件复制到系统剪贴板。
 * - 优先通过 FileProvider 生成 content:// URI，粘贴到文件管理器可直接使用
 * - 若文件不在 FileProvider 覆盖范围内，回退为复制文件路径纯文本
 *
 * @return true 成功，false 失败
 */
fun copyFileToClipboard(context: Context, filePath: String): Boolean {
    return try {
        val file = File(filePath)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // 优先通过 FileProvider 生成 content:// URI
        val uri: Uri? = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }

        if (uri != null) {
            clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "file", uri))
        } else {
            // 回退：复制文件路径纯文本
            clipboard.setPrimaryClip(ClipData.newPlainText("file_path", filePath))
        }
        true
    } catch (_: Exception) {
        false
    }
}