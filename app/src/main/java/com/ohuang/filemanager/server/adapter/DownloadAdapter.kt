package com.ohuang.filemanager.server.adapter

import android.util.Log
import com.ohuang.filemanager.getServiceFilePath
import com.ohuang.filemanager.server.util.DownloadUtil
import com.yanzhenjie.andserver.framework.body.FileBody
import com.yanzhenjie.andserver.framework.handler.HandlerAdapter
import com.yanzhenjie.andserver.framework.handler.MappingHandler
import com.yanzhenjie.andserver.framework.handler.RequestHandler
import com.yanzhenjie.andserver.framework.mapping.Addition
import com.yanzhenjie.andserver.framework.mapping.Mapping
import com.yanzhenjie.andserver.framework.view.BodyView
import com.yanzhenjie.andserver.framework.view.ObjectView
import com.yanzhenjie.andserver.framework.view.View
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.util.IOUtils
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.nio.channels.Channels

class DownloadAdapter : HandlerAdapter {


    override fun intercept(request: HttpRequest): Boolean {
        val path = request.path
        Log.d("DownloadAdapter", "intercept $path")
        return path.startsWith("/main/files") || path.startsWith("/main/download")

    }

    override fun getHandler(request: HttpRequest): RequestHandler? {
        Log.d("DownloadAdapter", "getHandler ${request.path}")
        return DownloadHandler(basePath =  getServiceFilePath())
    }
}


class DownloadHandler(var basePath: String) : MappingHandler("", Mapping(), Addition(),null) {

    private fun safePath(basePath: String, path: String?): File {
        var p = path ?: ""
        if (p.startsWith("/") || p.startsWith("\\")) {
            p = p.substring(1)
        }
        val dir = File(basePath)
        val target = File(dir, p)
        return try {
            if (!target.canonicalPath.startsWith(dir.canonicalPath)) {
                dir
            } else {
                target
            }
        } catch (e: IOException) {
            dir
        }
    }

    fun getFilePath(urlPath: String): String {
        var filePath = urlPath
        if (urlPath.startsWith("/main/files")) {
            filePath = urlPath.replaceFirst("/main/files", "")
        }
        if (urlPath.startsWith("/main/download")) {
            filePath = urlPath.replaceFirst("/main/download", "")
        }
        filePath = URLDecoder.decode(filePath, "utf-8")
        return filePath
    }

    override fun handle(
        request: HttpRequest,
        response: HttpResponse
    ): View? {
        return super.handle(request, response)

    }

    override fun onHandle(
        request: HttpRequest,
        response: HttpResponse
    ): View? {
        Log.d("DownloadAdapter", "handle ${request.path}")
        val path = request.path
        val filePath = getFilePath(path)
        if (filePath != path) {
            val file = safePath(basePath, filePath)
            val download = DownloadUtil.download(request, response, file, filePath)
            if (download!=null) {
                return ObjectView(true,download)
            }
        }
        return ObjectView(false, null)
    }

    override fun getETag(request: HttpRequest): String? {
        Log.d("DownloadAdapter", "getETag ${request.path}")
        return null
    }

    override fun getLastModified(request: HttpRequest): Long {
        Log.d("DownloadAdapter", "request ${request.path}")
        val path = request.path
        val filePath = getFilePath(path)
        if (filePath != path) {
            val file = safePath(basePath, filePath)
            if (file.exists()){
                return file.lastModified()
            }
        }
        return System.currentTimeMillis()
    }
}

class RandomAccessFileBody(val body:File,val contentLength:Long,val startByte: Long) : FileBody(body) {

    override fun contentLength(): Long {
        return contentLength
    }

    override fun writeTo(output: OutputStream) {
        val randomAccessFile: RandomAccessFile = RandomAccessFile(body, "r")

        //以只读模式设置文件指针偏移量
        randomAccessFile.seek(startByte)

        val channel = randomAccessFile.channel


        // 2. 转化为 InputStream（只读）
        val inputStream = Channels.newInputStream(channel)
        IOUtils.write(inputStream, output)
        IOUtils.closeQuietly(inputStream)

    }
}