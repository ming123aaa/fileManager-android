package com.ohuang.filemanager.config

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.text.substring
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.util.SPUtil
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    private var baseUrl: String = ""



    private var _readOnlyFlow=MutableStateFlow(false)
    var readOnly: StateFlow<Boolean> = _readOnlyFlow

    fun getBaseUrl(): String {
        return baseUrl.ifEmpty { DEFAULT_BASE_URL }
    }

    fun connect(){
        ApiService.testConnect().request {
            _readOnlyFlow.value = it.lowercase().contains("read")
        }
    }


    fun saveBaseUrl(context: Context, mUrl: String) {
        var url=mUrl
        if(mUrl.endsWith("/")){
            url=mUrl.substring(0,mUrl.length-1)
        }
        if (!(mUrl.startsWith("http://")||mUrl.startsWith("https://"))){
            url= "http://$mUrl"
        }
        SPUtil.put(context, "server_url", url)
        baseUrl = url
        connect()
        Toast.makeText(context, "保存服务器配置成功", Toast.LENGTH_SHORT).show()
    }

    fun loadBaseUrl(context: Context) {
        baseUrl = SPUtil.get(context, "server_url", "") as String
        connect()
    }

    fun getWebUrl(isManager: Boolean = true): String {
        return if (isManager) {
            "$baseUrl/file.html"
        } else {
            baseUrl
        }
    }
}