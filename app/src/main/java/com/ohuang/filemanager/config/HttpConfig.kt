package com.ohuang.filemanager.config

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.text.substring
import com.ohuang.filemanager.util.SPUtil

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    private var baseUrl: String = ""

    fun getBaseUrl(): String {
        return baseUrl.ifEmpty { DEFAULT_BASE_URL }
    }


    fun saveBaseUrl(context: Context, mUrl: String) {
        var url=mUrl
        if(mUrl.endsWith("/")){
            url=mUrl.substring(0,mUrl.length-1)
        }
        SPUtil.put(context, "server_url", url)
        baseUrl = url
        Toast.makeText(context, "保存服务器配置成功", Toast.LENGTH_SHORT).show()
    }

    fun loadBaseUrl(context: Context) {
        baseUrl = SPUtil.get(context, "server_url", "") as String
    }

    fun getWebUrl(isManager: Boolean = true): String {
        return if (isManager) {
            "$baseUrl/file.html"
        } else {
            baseUrl
        }
    }
}