@file:OptIn(ExperimentalMaterial3Api::class)

package com.ohuang.filemanager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ohuang.filemanager.server.MutableWebServer
import com.ohuang.filemanager.server.adapter.DownloadAdapter
import com.ohuang.filemanager.server.util.AppContext
import com.ohuang.filemanager.util.ClipboardUtils
import com.ohuang.filemanager.util.NetWorkUtil
import com.ohuang.filemanager.util.SPUtil
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.server.WebServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.coroutines.resume
import kotlin.math.max

class ServiceLauncherActivity : ComponentActivity() {

    private val REQUEST_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var port: Int by remember {
                        mutableIntStateOf(getServicePort())
                    }

                    var fileDirPath: String by remember {
                        mutableStateOf(getServiceFilePath())
                    }

                    var saveEnable: Boolean by remember {
                        mutableStateOf(true)
                    }

                    val rememberCoroutineScope = rememberCoroutineScope()

                    Scaffold(
                        topBar = {
                            SmallTopAppBar(
                                title = { Text("本地服务器设置") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        finish()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {

                                        if (getServicePort() != port || getServiceFilePath() != fileDirPath) {
                                            saveEnable = false
                                            setServicePort(port = port)
                                            setServiceFilePath(path = fileDirPath)
                                            rememberCoroutineScope.launch {

                                                AndServerManager.waitRestart()

                                                saveEnable = true
                                                Toast.makeText(
                                                    this@ServiceLauncherActivity, "已保存",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            saveEnable = true
                                            Toast.makeText(
                                                this@ServiceLauncherActivity, "已保存",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }, enabled = saveEnable) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "保存"
                                        )
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            Content(
                                port = port,
                                onPortChange = { port = it },
                                fileDirPath = fileDirPath,
                                onFilePathChange = { fileDirPath = it })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Content(
        port: Int,
        onPortChange: (Int) -> Unit,
        fileDirPath: String,
        onFilePathChange: (String) -> Unit
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp)) {
            Text(
                "当前文件目录:$fileDirPath", maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            val directoryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri: Uri? ->
                    uri?.let {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val path = uriToPath(it)
                        path?.let { filePath ->
                            onFilePathChange(filePath)
                            AndServerManager.msgList.add(ServiceMsg(msg = "已选择目录: $filePath"))
                        }
                    }
                }
            )

            Button(onClick = {
                if (checkStoragePermission()) {
                    directoryLauncher.launch(null)
                } else {
                    requestStoragePermission()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择文件目录")
            }

            Row(modifier = Modifier.fillMaxWidth()) {

                Button(onClick = {
                    onFilePathChange(getDefaultServiceFilePath())
                }, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("默认目录")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = {
                    requestStoragePermission()
                }, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("权限申请")
                }
            }

            Row(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth()

            ) {
                Text("端口:", modifier = Modifier.align(alignment = Alignment.CenterVertically))
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    modifier = Modifier.weight(1f),
                    value = "$port", onValueChange = {
                        onPortChange(it.toIntOrNull() ?: port)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动启动本地服务器:", modifier = Modifier.weight(1f))
                var autoStart by remember {
                    mutableStateOf(getServiceAuto())
                }
                Switch(
                    checked = autoStart,
                    onCheckedChange = {
                        autoStart = it
                        setServiceAuto(isAuto = it)
                        AndServerManager.msgList.add(ServiceMsg(msg = if (it) "已开启自启" else "已关闭自启"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {

                items(AndServerManager.msgList) {
                    val textColor = when (it.type) {
                        ServiceType.THROW -> MaterialTheme.colorScheme.error
                        ServiceType.ERROR -> MaterialTheme.colorScheme.error
                        ServiceType.STOP -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Text(
                        it.msg, color = textColor, modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ClipboardUtils.copyText(
                                    it.msg,
                                    this@ServiceLauncherActivity
                                )
                                Toast.makeText(
                                    this@ServiceLauncherActivity, "已复制",
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                }
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        val readGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                AndServerManager.msgList.add(ServiceMsg(msg = "权限申请成功"))
            } else {
                AndServerManager.msgList.add(
                    ServiceMsg(
                        type = ServiceType.ERROR,
                        msg = "权限申请失败，请手动开启权限"
                    )
                )
                Toast.makeText(this, "权限申请失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uriToPath(uri: Uri): String? {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}"
            } else {
                split[1]
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun getServiceAuto(context: Context = AppContext.instance): Boolean {
    return SPUtil.get(context, "fileManager_auto", true) as Boolean
}

fun setServiceAuto(context: Context = AppContext.instance, isAuto: Boolean) {
    SPUtil.put(context, "fileManager_auto", isAuto)
}

fun getServicePort(context: Context = AppContext.instance): Int {
    return SPUtil.get(context, "fileManager_port", 8080) as Int
}

fun setServicePort(context: Context = AppContext.instance, port: Int = 8080) {
    SPUtil.put(context, "fileManager_port", port)
}

fun getServiceFilePath(context: Context = AppContext.instance): String {

    return SPUtil.get(
        context,
        "fileManager_dir",
        getDefaultServiceFilePath()
    ) as String
}

fun getDefaultServiceFilePath(context: Context = AppContext.instance): String {
    return context.filesDir.absolutePath + "/fileManager"
}

fun setServiceFilePath(
    context: Context = AppContext.instance,
    path: String = getDefaultServiceFilePath()
) {
    SPUtil.put(context, "fileManager_dir", path)
}

enum class ServiceType {
    MSG, ERROR, THROW, STOP
}

data class ServiceMsg(
    val type: ServiceType = ServiceType.MSG,
    val msg: String,
    val time: Long = System.currentTimeMillis()
)

object AndServerManager {

    fun autoStart(context: Context = AppContext.instance) {
        if (!isRunning && getServiceAuto(context)) {
            run(context, getServicePort(context))
        }
    }


    val msgList = SnapshotStateList<ServiceMsg>()


    var isRunning by mutableStateOf(false)
        private set

    var url by mutableStateOf("")
        private set
    var startServiceFair by mutableStateOf(false)
        private set

    private var server: Server? = null


    private var restartCall: () -> Unit = {}

    private var isRestart: Boolean = false


    fun stop() {
        server?.shutdown()
        server = null

    }

    suspend fun waitRestart(context: Context = AppContext.instance, port: Int = getServicePort()) {
        if (isRestart) {
            return
        }
        isRestart = true
        if (isRunning) {
            return suspendCancellableCoroutine {
                restartCall = {
                    isRestart = false
                    restartCall = {}
                    run(context = context, port = port)
                    if (it.isActive) {
                        it.resume(Unit)
                    }
                }
                stop()
            }
        } else {
            run(context = context, port = port)
            isRestart = false
        }

    }


    fun run(context: Context = AppContext.instance, port: Int = getServicePort()) {
        if (isRunning) {
            return
        }
        startServiceFair=false
        msgList.clear()

        server = MutableWebServer.builder(context)
            .addAdapter(DownloadAdapter())
            .port(port)
            .listener(object : Server.ServerListener {
                override fun onStarted() {
                    isRunning = true
                    val ip = getLocalIpAddress(context)
                    msgList.add(ServiceMsg(msg = "服务已启动"))
                    msgList.add(ServiceMsg(msg = "访问地址:http://$ip:$port"))
                    msgList.add(ServiceMsg(msg = "http://$ip:$port"))
                    url = "http://$ip:$port"
                }

                override fun onStopped() {
                    isRunning = false

                    msgList.removeIf { it.type == ServiceType.MSG }
                    msgList.add(ServiceMsg(type = ServiceType.STOP, msg = "服务已停止"))
                    restartCall()
                }

                override fun onException(e: Exception?) {
                    isRunning = false

                    msgList.removeIf { it.type == ServiceType.MSG }
                    Toast.makeText(context, "运行失败,请检查端口和网络", Toast.LENGTH_SHORT).show()
                    startServiceFair=true
                    msgList.add(
                        ServiceMsg(
                            type = ServiceType.THROW,
                            msg = "运行失败,请检查端口和网络: ${e?.message}\n${e?.stackTraceToString()}"
                        )
                    )
                    restartCall()
                }
            })
            .build()

        server?.startup()
    }

    private fun getLocalIpAddress(context: Context): String {
        try {
            return NetWorkUtil.getWifiIP(context) ?: "127.0.0.1"
        } catch (ex: Exception) {
        }
        return "127.0.0.1"
    }
}