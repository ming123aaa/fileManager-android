package com.ohuang.filemanager.data

import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.kthttp.HttpClient
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.download.FileInfo
import com.ohuang.kthttp.downloadFileInfo
import com.ohuang.kthttp.jsonCall
import com.ohuang.kthttp.post
import com.ohuang.kthttp.upload.addFile
import com.ohuang.kthttp.upload.postUploadFile
import com.ohuang.kthttp.url
import java.io.File

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

    fun deleteFile(path: String): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/delete")
            post {
                addParam("path", path)
            }
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

    fun checkDownloadPath(downloadPath: String): HttpCall<FileInfo>{
        return client.downloadFileInfo {
            url(downloadPath)
        }
    }

    fun getDownloadPath(fullPath: String,isFolder: Boolean=false): String{
        val baseUrl =HttpConfig.getBaseUrl()
        val encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8").replace("%2f","/").replace("+", "%20")
        return "${baseUrl}/main/files/$encodedPath"

    }

    fun testConnect(baseUrl: String=HttpConfig.getBaseUrl()): HttpCall<String> {
        return client.stringCall {
            url("$baseUrl/test/connect")
        }
    }

    fun uploadFile(
        file: File,
        path: String,
        onProgress: (current: Long, totalSize: Long) -> Unit,
    ): HttpCall<String> {
        return client.stringCall {
            url(HttpConfig.getBaseUrl() + BASE_PATH + "/fileUpload")

                postUploadFile {
                    addFile(key = "fileName", file = file, callBack = onProgress)
                    addFormDataPart("path",path)
                }


        }
    }
}
