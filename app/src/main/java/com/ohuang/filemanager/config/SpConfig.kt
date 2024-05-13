package com.ohuang.filemanager.config

import android.content.Context
import com.ohuang.filemanager.util.SPUtil

object SpConfig {

    fun setUrl(context: Context, url: String) {
        SPUtil.put(context, "url", url)
    }

    fun getUrl(context: Context): String {
        return SPUtil.get(context, "url", "") as String
    }

    fun setUseOldDownloadApi(context: Context, boolean: Boolean){
        SPUtil.put(context, "useOldDownloadApi", boolean)
    }

    fun getUseOldDownloadApi(context: Context): Boolean{
        return SPUtil.get(context, "useOldDownloadApi", false) as Boolean
    }
}