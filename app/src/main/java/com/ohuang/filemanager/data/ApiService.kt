package com.ohuang.filemanager.data

import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.kthttp.HttpClient
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.call.map
import com.ohuang.kthttp.download
import com.ohuang.kthttp.download.FileInfo
import com.ohuang.kthttp.downloadFileInfo
import com.ohuang.kthttp.jsonCall
import com.ohuang.kthttp.post
import com.ohuang.kthttp.stringHttpResponseCall
import com.ohuang.kthttp.upload.addFile
import com.ohuang.kthttp.upload.addFileInputSteam
import com.ohuang.kthttp.upload.postUploadFile
import com.ohuang.kthttp.url
import java.io.File
import java.io.FileInputStream

object ApiService {
    private const val BASE_PATH = "/main"

    private val client = HttpClient()

    fun getAllFiles(path: String): HttpCall<List<FileItem>> {
        return client.jsonCall<List<FileItem>> {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/getAllFile") {
                if (path.isNotEmpty()) {
                    addParam("path", path)
                }
            }
        }
    }

    fun getFileInfo(path: String): HttpCall<FileItem> {
        return client.jsonCall<FileItem> {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/fileInfo") {
                addParam("path", path)
            }
        }
    }

    fun createFolder(name: String, path: String = ""): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/mkdir")
            post {
                addParam("name", name)
                addParam("path", path)
            }
        }
    }

    fun createFile(name: String, path: String = ""): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/createFile")
            post {
                addParam("name", name)
                addParam("path", path)
            }
        }
    }

    fun deleteFile(path: String): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/delete")
            post {
                addParam("path", path)
            }
        }
    }

    /**
     * 下载文件
     */
    fun download(
        url: String,
        file: File,
        isContinueDownload: Boolean, //是否断点下载
        onProcess: (current: Long, total: Long) -> Unit = { _, _ -> }
    ): HttpCall<File> {
        return client.download(file = file, isContinueDownload = isContinueDownload, onProcess = onProcess) {
            url(url)
        }
    }


    /**
     * 获取要下载文件的大小
     */
    fun checkDownloadPath(downloadPath: String): HttpCall<FileInfo> {
        return client.downloadFileInfo {
            url(downloadPath)
        }
    }

    fun renameFile(path: String, newName: String): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/rename")
            post {
                addParam("path", path)
                addParam("newName", newName)
            }
        }
    }

    fun moveFile(path: String, targetDir: String): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/move")
            post {
                addParam("path", path)
                addParam("targetDir", targetDir)
            }
        }
    }

    fun readText(path: String, encoding: String = ""): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/readText") {
                addParam("path", path)
                if (encoding.isNotEmpty()) {
                    addParam("encoding", encoding)
                }
            }
        }
    }

    fun writeText(path: String, text: String): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/writeText")
            post {
                addParam("path", path)
                addParam("txt", text)
            }
        }
    }


    fun getDownloadPath(fullPath: String, isFolder: Boolean = false): String {
        if (fullPath.startsWith("http://")||fullPath.startsWith("https://")){
            return fullPath
        }
        val baseUrl = HttpConfig.getBaseUrl()
        val path =
            java.net.URLEncoder.encode(fullPath, "UTF-8").replace("%2F", "/").replace("+", "%20")
        val encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8").replace("+", "%20")
        return if (isFolder) {
            "${baseUrl}/file.html?path=${encodedPath}"
        } else {
            "${baseUrl}/main/files/$path"
        }


    }

    fun testConnect(baseUrl: String = HttpConfig.getBaseUrl()): HttpCall<String> {
        return client.stringCall {
            url("$baseUrl/test/connect")
        }
    }

    fun uploadFile(
        file: FileInputStream,
        fileName: String,
        path: String,
        onProgress: (current: Long, totalSize: Long) -> Unit,
    ): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/fileUpload")

            postUploadFile {
                addFileInputSteam(key = "fileName", file = file, fileName = fileName, callBack = onProgress)
                addFormDataPart("path", path)
            }


        }
    }
}
