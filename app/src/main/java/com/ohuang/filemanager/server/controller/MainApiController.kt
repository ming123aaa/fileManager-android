package com.ohuang.filemanager.server.controller

import android.util.Log
import com.google.gson.Gson
import com.ohuang.filemanager.getServiceFilePath
import com.ohuang.filemanager.server.bean.FileBean
import com.ohuang.filemanager.server.util.AppContext
import com.ohuang.filemanager.util.SPUtil
import com.yanzhenjie.andserver.annotation.*
import com.yanzhenjie.andserver.framework.body.FileBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.multipart.MultipartFile
import com.yanzhenjie.andserver.http.multipart.MultipartRequest
import java.io.*
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/main")
class MainApiController {

    private val gson = Gson()

    private fun getBasePath(): String {
        return getServiceFilePath(AppContext.instance)
    }

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



    @GetMapping("/fileUpload")
    fun fileUploadGet(
        @RequestParam("fileName") file: MultipartFile,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String
    ): String {
        return fileUploadInternal(getBasePath(), file, path)
    }

    @PostMapping("/fileUpload")
    fun fileUploadPost(
        @RequestParam("fileName") file: MultipartFile,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String
    ): String {
        return fileUploadInternal(getBasePath(), file, path)
    }

    private fun fileUploadInternal(basePath: String, file: MultipartFile, path: String): String {
        if (file.isEmpty) {
            return "文件为空"
        }
        val fileName = file.filename ?: return "文件名无效"
        val safeFileName = File(fileName).name
        val dir = safePath(basePath, path)
        val dest = File(dir, safeFileName)
        return try {
            file.transferTo(dest)
            "上传成功"
        } catch (e: Exception) {
            "上传失败: ${e.message}"
        }
    }

    @GetMapping("/multifileUpload")
    fun multifileUploadGet(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String
    ): String {
        return multifileUploadInternal(getBasePath(), request, path)
    }

    @PostMapping("/multifileUpload")
    fun multifileUploadPost(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String
    ): String {
        return multifileUploadInternal(getBasePath(), request, path)
    }

    private fun multifileUploadInternal(
        basePath: String,
        request: HttpRequest,
        path: String
    ): String {
        if (request !is MultipartRequest) {
            return "请求格式错误"
        }
        val files = request.getFiles("fileName")
        if (files.isNullOrEmpty()) {
            return "没有选择文件"
        }
        val dir = safePath(basePath, path)
        var successCount = 0
        for (file in files) {
            if (file.isEmpty) continue
            val fileName = file.filename ?: continue
            val safeFileName = File(fileName).name
            try {
                file.transferTo(File(dir, safeFileName))
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "成功上传 $successCount 个文件"
    }







    @GetMapping("/getAllFile")
    fun getAllFileGet(
        request: HttpRequest,
        @RequestParam(value = "path", required = false) path: String
    ): String {
        return getAllFileInternal(getBasePath(), path)
    }

    @PostMapping("/getAllFile")
    fun getAllFilePost(
        request: HttpRequest,
        @RequestParam(value = "path", required = false) path: String
    ): String {
        return getAllFileInternal(getBasePath(), path)
    }

    private fun getAllFileInternal(basePath: String, path: String): String {
        val files = mutableListOf<FileBean>()
        val file = safePath(basePath, path)
        val tempList = file.listFiles()
        if (tempList != null) {
            for (f in tempList) {
                files.add(FileBean(f.name, f.length(), f.isDirectory, f.lastModified()))
            }
        }
        return gson.toJson(files)
    }

    @GetMapping("/mkdir")
    fun mkdirGet(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String,
        @RequestParam("name") name: String
    ): String {
        return mkdirInternal(getBasePath(), path, name)
    }

    @PostMapping("/mkdir")
    fun mkdirPost(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String,
        @RequestParam("name") name: String
    ): String {
        return mkdirInternal(getBasePath(), path, name)
    }

    private fun mkdirInternal(basePath: String, path: String, name: String): String {
        if (name.isBlank()) {
            return "文件夹名不能为空"
        }
        val safeName = File(name).name
        val dir = safePath(basePath, path)
        val newDir = File(dir, safeName)
        if (newDir.exists()) {
            return "文件夹已存在"
        }
        return if (newDir.mkdirs()) "创建成功" else "创建失败"
    }

    @GetMapping("/delete")
    fun deleteGet(request: HttpRequest, @RequestParam("path") path: String): String {
        return deleteInternal(getBasePath(), path)
    }

    @PostMapping("/delete")
    fun deletePost(request: HttpRequest, @RequestParam("path") path: String): String {
        return deleteInternal(getBasePath(), path)
    }

    private fun deleteInternal(basePath: String, path: String): String {
        val file = safePath(basePath, path)
        if (!file.exists()) {
            return "文件不存在"
        }
        return if (deleteRecursive(file)) "删除成功" else "删除失败"
    }

    private fun deleteRecursive(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursive(child)
                }
            }
        }
        return file.delete()
    }

    @GetMapping("/rename")
    fun renameGet(
        request: HttpRequest,
        @RequestParam("path") path: String,
        @RequestParam("newName") newName: String
    ): String {
        return renameInternal(getBasePath(), path, newName)
    }

    @PostMapping("/rename")
    fun renamePost(
        request: HttpRequest,
        @RequestParam("path") path: String,
        @RequestParam("newName") newName: String
    ): String {
        return renameInternal(getBasePath(), path, newName)
    }

    private fun renameInternal(basePath: String, path: String, newName: String): String {
        if (newName.isBlank()) {
            return "新名称不能为空"
        }
        val safeNewName = File(newName).name
        val file = safePath(basePath, path)
        if (!file.exists()) {
            return "文件不存在"
        }
        val newFile = File(file.parentFile, safeNewName)
        if (newFile.exists()) {
            return "同名文件已存在"
        }
        return if (file.renameTo(newFile)) "重命名成功" else "重命名失败"
    }

    @GetMapping("/move")
    fun moveGet(
        request: HttpRequest,
        @RequestParam("path") path: String,
        @RequestParam("targetDir") targetDir: String
    ): String {
        return moveInternal(getBasePath(), path, targetDir)
    }

    @PostMapping("/move")
    fun movePost(
        request: HttpRequest,
        @RequestParam("path") path: String,
        @RequestParam("targetDir") targetDir: String
    ): String {
        return moveInternal(getBasePath(), path, targetDir)
    }

    private fun moveInternal(basePath: String, path: String, targetDir: String): String {
        val file = safePath(basePath, path)
        if (!file.exists()) {
            return "文件不存在"
        }
        val dest = safePath(basePath, targetDir)
        if (!dest.exists() || !dest.isDirectory) {
            return "目标目录不存在"
        }
        val newFile = File(dest, file.name)
        if (newFile.exists()) {
            return "目标位置已存在同名文件"
        }
        return if (file.renameTo(newFile)) "移动成功" else "移动失败"
    }

    @GetMapping("/writeText")
    fun writeTextGet(
        request: HttpRequest,
        @RequestParam("txt",required = false, defaultValue = "") txt: String,
        @RequestParam(value = "path", required = false, defaultValue = "test.txt") path: String
    ): String {
        return writeTextInternal(getBasePath(), txt, path)
    }

    @PostMapping("/writeText")
    fun writeTextPost(
        request: HttpRequest,
        @RequestParam("txt",required = false, defaultValue = "") txt: String,
        @RequestParam(value = "path", required = false, defaultValue = "test.txt") path: String
    ): String {
        return writeTextInternal(getBasePath(), txt, path)
    }

    private fun writeTextInternal(basePath: String, txt: String, path: String): String {
        val file = safePath(basePath, path)
        return try {
            FileOutputStream(file).use { fos ->
                if(txt.isEmpty()){
                    fos.write(ByteArray(0))
                }else {
                    fos.write(txt.toByteArray(StandardCharsets.UTF_8))
                }
            }
            "写入成功"
        } catch (e: Throwable) {
            "写入失败: ${e.message}"
        }
    }

    @GetMapping("/readText")
    fun readTextGet(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "test.txt") path: String,
        @RequestParam(value = "encoding", required = false) encoding: String?
    ): String {
        return readTextInternal(getBasePath(), path, encoding)
    }

    @PostMapping("/readText")
    fun readTextPost(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "test.txt") path: String,
        @RequestParam(value = "encoding", required = false) encoding: String?
    ): String {
        return readTextInternal(getBasePath(), path, encoding)
    }

    private fun readTextInternal(basePath: String, path: String, encoding: String?): String {
        val file = safePath(basePath, path)
        if (!file.isFile || !file.exists()) {
            return ""
        }
        var charset = StandardCharsets.UTF_8
        if (encoding != null && encoding.isNotEmpty()) {
            charset = try {
                charset(encoding)
            } catch (e: Exception) {
                detectEncoding(file)
            }
        } else {
            charset = detectEncoding(file)
        }
        return try {
            BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { br ->
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(line)
                }
                sb.toString()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun detectEncoding(file: File): java.nio.charset.Charset {
        try {
            FileInputStream(file).use { isStream ->
                val head = ByteArray(3)
                if (isStream.read(head) >= 3) {
                    if (head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()) {
                        return StandardCharsets.UTF_8
                    }
                    if (head[0] == 0xFF.toByte() && head[1] == 0xFE.toByte()) {
                        return charset("UTF-16LE")
                    }
                    if (head[0] == 0xFE.toByte() && head[1] == 0xFF.toByte()) {
                        return charset("UTF-16BE")
                    }
                }
            }
        } catch (e: Exception) {
        }
        return StandardCharsets.UTF_8
    }

    @GetMapping("/createFile")
    fun createFileGet(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String,
        @RequestParam("name") name: String
    ): String {
        return createFileInternal(getBasePath(), path, name)
    }

    @PostMapping("/createFile")
    fun createFilePost(
        request: HttpRequest,
        @RequestParam(value = "path", required = false, defaultValue = "") path: String,
        @RequestParam("name") name: String
    ): String {
        return createFileInternal(getBasePath(), path, name)
    }

    private fun createFileInternal(basePath: String, path: String, name: String): String {
        if (name.isBlank()) {
            return "文件名不能为空"
        }
        val safeName = File(name).name
        val dir = safePath(basePath, path)
        val newFile = File(dir, safeName)
        if (newFile.exists()) {
            return "文件已存在"
        }
        return try {
            if (newFile.createNewFile()) "创建成功" else "创建失败"
        } catch (e: IOException) {
            "创建失败: ${e.message}"
        }
    }

    @GetMapping("/fileInfo")
    fun fileInfoGet(request: HttpRequest, @RequestParam("path") path: String): String {
        return fileInfoInternal(getBasePath(), path)
    }

    @PostMapping("/fileInfo")
    fun fileInfoPost(request: HttpRequest, @RequestParam("path") path: String): String {
        return fileInfoInternal(getBasePath(), path)
    }

    private fun fileInfoInternal(basePath: String, path: String): String {
        val file = safePath(basePath, path)
        if (!file.exists()) {
            return "{}"
        }
        return gson.toJson(
            FileBean(
                file.name,
                file.length(),
                file.isDirectory,
                file.lastModified()
            )
        )
    }
}