package com.ohuang.filemanager.server.config

import android.content.Context
import com.ohuang.filemanager.getServiceFilePath
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.Multipart
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite
import com.yanzhenjie.andserver.framework.website.FileBrowser
import java.io.File


@Config
class AppConfig : WebConfig {
    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        delegate.addWebsite(AssetsWebsite(context, "/html/","index.html"))
//        delegate.addWebsite(AssetsWebsite(context, "/html/","file.html"))



    }
}