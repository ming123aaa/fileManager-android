package com.ohuang.filemanager

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewSetting {


    fun setting(webView: WebView,context: Context){
        webView.settings.apply {
            //开启js
            javaScriptEnabled = true
            //弹出框的设置
            //1.NARROW_COLUMNS：可能的话使所有列的宽度不超过屏幕宽度
            //2.NORMAL：正常显示不做任何渲染
            //3.SINGLE_COLUMN：把所有内容放大webview等宽的一列中
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            //它会使用网页元标记中定义的属性加载WebView。因此它按照html中的定义缩放网页。
            useWideViewPort = true

            /************ 缓存模式 **********/
            //保存密码
            savePassword = true
            //保存表单数据
            saveFormData = true
            //开启数据库
            databaseEnabled = true
            //设置DOM Storage缓存
            domStorageEnabled = true
            //关闭webView中缓存
            cacheMode = WebSettings.LOAD_NO_CACHE
            //设置缓存路径
            setAppCachePath(context.cacheDir.absolutePath)

            /************ 页面自动适配 **********/
            //步骤1.隐藏webview缩放按钮
            displayZoomControls = true

            //步骤2 设置页面布局
            //1)方式一，控制页面布局，有一定缺陷可能导致页面显示温度，不推荐
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

            //2）方式二,自动根据手机分辨率缩放，推荐
            useWideViewPort = true
            loadWithOverviewMode = true

            /************ 页面缩放支持 **********/
            //仅支持双击缩放，不支持触摸缩放（android4.0）
            setSupportZoom(true)
            //设置支持缩放,设置了此属性，setSupportZoom(true);也默认设置为true
            builtInZoomControls = true

            /************ 图片加载 **********/
            //默认为false,true表示阻塞图片请求
            blockNetworkImage = true
            //支持自动加载图片
            loadsImagesAutomatically = true

            /************ 字体相关 **********/
            //设置WebView标准字体库字体，默认字体“sans-serif”。
            standardFontFamily = ""
            //设置WebView字体最小值，默认值8，取值1到72
            minimumFontSize = 8
            //设定编码格式
            defaultTextEncodingName = "UTF-8"
            //设置在WebView内部是否允许访问文件
            allowFileAccess = true
            //设置WebView中加载页面字体变焦百分比，默认100，整型数。
            textZoom = 100

            /************ 插件相关 **********/
            //支持插件
            pluginState = WebSettings.PluginState.ON
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            //多窗口
            supportMultipleWindows()
            //当webview调用requestFocus时为webview设置节点 webview
            setNeedInitialFocus(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //设置Web调试
                WebView.setWebContentsDebuggingEnabled(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //解决加载Https和Http混合模式网页加载问题
                    mixedContentMode = WebSettings.LOAD_NORMAL
                }

            }
            //追加自定义标识符，一定要+=
            userAgentString += "/native_${context.packageName}"
        }

    }
}