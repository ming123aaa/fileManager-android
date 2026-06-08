package com.ohuang.filemanager.config

import android.content.Context
import android.widget.Toast
import com.ohuang.filemanager.util.SPUtil

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://192.168.8.102:8080"

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
        Toast.makeText(context,"保存服务器配置成功", Toast.LENGTH_SHORT).show()
    }

    fun loadBaseUrl(context: Context) {
        baseUrl = SPUtil.get(context, "server_url", "") as String
    }

    fun getWebUrl(isManager: Boolean=true): String{
        return if (isManager){
            return baseUrl+"/file.html"
        }else{
            return baseUrl
        }
    }
}