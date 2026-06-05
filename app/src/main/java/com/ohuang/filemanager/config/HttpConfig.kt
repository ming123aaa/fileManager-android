package com.ohuang.filemanager.config

import android.content.Context
import com.ohuang.filemanager.util.SPUtil

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://192.168.8.5:8080"

    private var baseUrl: String = ""

    fun getBaseUrl(): String {
        return if (baseUrl.isNotEmpty()) baseUrl else DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun saveBaseUrl(context: Context, url: String) {
        SPUtil.put(context, "server_url", url)
        baseUrl = url
    }

    fun loadBaseUrl(context: Context) {
        baseUrl = SPUtil.get(context, "server_url", "") as String
    }
}