package com.ohuang.filemanager.config

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.text.substring
import com.google.gson.Gson
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.server.scan.DeviceBindingManager
import com.ohuang.filemanager.server.scan.DeviceInfo
import com.ohuang.filemanager.server.scan.DeviceScanner
import com.ohuang.filemanager.server.util.AppContext
import com.ohuang.filemanager.util.SPUtil
import com.ohuang.kthttp.call.awaitOrNull
import com.ohuang.kthttp.transform.gsonTransForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    var baseUrl: String = ""
        private set
        get() {
            if (DeviceBindingManager.isUseDevice()) {
                val deviceUrl = DeviceBindingManager.getUrl()
                if (!deviceUrl.isNullOrBlank()) {
                    return deviceUrl
                }
            }
            return field.ifEmpty { DEFAULT_BASE_URL }
        }

    private var isScanned: Boolean = false


    private var _readOnlyFlow = MutableStateFlow(false)
    var readOnly: StateFlow<Boolean> = _readOnlyFlow


   suspend fun connect() {
       withContext(Dispatchers.IO){
           if (!isScanned&&DeviceBindingManager.isUseDevice()) {
               isScanned=true
               DeviceBindingManager.scanOrNull()
           }
           ApiService.testConnect().request {
               _readOnlyFlow.value = it.lowercase().contains("read")
           }
       }


    }


    fun saveBaseUrl(context: Context, mUrl: String) {
        var url = mUrl
        if (mUrl.endsWith("/")) {
            url = mUrl.substring(0, mUrl.length - 1)
        }
        if (!(mUrl.startsWith("http://") || mUrl.startsWith("https://"))) {
            url = "http://$mUrl"
        }
        SPUtil.put(context, "server_url", url)
        baseUrl = url
        GlobalScope.launch {
            connect()
        }

        Toast.makeText(context, "保存服务器配置成功", Toast.LENGTH_SHORT).show()
    }


    fun loadBaseUrl(context: Context) {
        isScanned = false
        baseUrl = SPUtil.get(context, "server_url", "") as String
        DeviceBindingManager.init()
        GlobalScope.launch {
            connect()
        }
    }

    fun getWebUrl(isManager: Boolean = true): String {
        return if (isManager) {
            "$baseUrl/file.html"
        } else {
            baseUrl
        }
    }
}