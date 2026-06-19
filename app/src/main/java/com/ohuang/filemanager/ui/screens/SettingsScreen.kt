package com.ohuang.filemanager.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.ohuang.filemanager.AndServerManager
import com.ohuang.filemanager.DataMigrationActivity
import com.ohuang.filemanager.ServiceLauncherActivity
import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.getDefaultServiceFilePath
import com.ohuang.filemanager.getPrivateServiceFilePath
import com.ohuang.filemanager.getServicePort
import com.ohuang.filemanager.util.ClipboardUtils
import com.ohuang.filemanager.util.BatteryOptimizationHelper
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current


    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {

                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {


            ServiceUrlSetting(context)

            Spacer(modifier = Modifier.height(24.dp))

            LocalService(context)

            Spacer(modifier = Modifier.height(24.dp))

            AboutContent(context)

        }
    }
}

@Composable
private fun ServiceUrlSetting(context: Context) {
    Column {

        val serverUrl = remember { mutableStateOf(HttpConfig.getBaseUrl()) }
        val isSaving = remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "服务器地址",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
            )

            IconButton(
                onClick = {
                    if (serverUrl.value.isNotEmpty()) {
                        isSaving.value = true
                        HttpConfig.saveBaseUrl(context, serverUrl.value)
                        isSaving.value = false

                    }
                },
                enabled = !isSaving.value && serverUrl.value.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save"
                )
            }
        }
        OutlinedTextField(
            value = serverUrl.value,
            onValueChange = { serverUrl.value = it },
            label = { Text("服务器地址") },
            placeholder = { Text("http://localhost:8080") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "提示: 请确保服务器地址正确，包括协议(http/https)和端口号",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        var testMsg by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        Row(
            modifier = Modifier.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                coroutineScope.launch {
                    testMsg = "正在测试中..."
                    var data = ApiService.testConnect(serverUrl.value).awaitOrNull {
                        testMsg = it.message ?: "请求失败"
                    }
                    if (data != null) {
                        testMsg = data
                    }
                }
            }) {
                Text("测试地址")
            }

            Text("测试结果:${testMsg}", modifier = Modifier.padding(horizontal = 5.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))


        // Web端 入口按钮
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, HttpConfig.getWebUrl(true).toUri())
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("网页端")
        }

    }
}

@Composable
private fun LocalService(context: Context) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本地服务器",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
            )

        }



        Button({
            if (AndServerManager.isRunning) {
                AndServerManager.stop()
            } else {
                AndServerManager.run(port = getServicePort())
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (AndServerManager.isRunning) "停止本地服务器" else "启动本地服务器")
        }
        Spacer(modifier = Modifier.height(15.dp))

        Text(
            if (AndServerManager.isRunning) "本地服务器已启动,通过以下地址访问:\n${AndServerManager.url}" else "本地服务器未启动 ${if (AndServerManager.startServiceFair) "-服务启动失败" else ""}",
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = AndServerManager.isRunning, onClick = {
                    ClipboardUtils.copyText(
                        AndServerManager.url,
                        context
                    )
                    Toast.makeText(
                        context, "已复制",
                        Toast.LENGTH_SHORT
                    ).show()
                })
        )

        Spacer(modifier = Modifier.height(15.dp))
        Button(
            onClick = {
                BatteryOptimizationHelper.checkAndRequest(context){
                    if (!it) {
                        BatteryOptimizationHelper.openAppSettings(context)
                    }
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.SettingsPower,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("忽略电池优化")
        }

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = {
                val intent = Intent(context, ServiceLauncherActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("本地服务端配置")
        }


    }
}


@Composable
private fun AboutContent(context: Context) {
    Column {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/ming123aaa/fileManager-android".toUri()
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "文件管理器",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "版本 ${getAppVersion(context)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 清理缓存功能
        CacheCleanerCard(context)

        Spacer(modifier = Modifier.height(16.dp))

        var showClearDialog by remember { mutableStateOf(false) }
        Button(
            onClick = {
                showClearDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("清空内部文件")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                context.startActivity(Intent(context, DataMigrationActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("本地文件数据")
        }

        val rememberCoroutineScope = rememberCoroutineScope()
        if (showClearDialog) {

            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("确认清除") },
                text = { Text("确定要清空内部文件吗？此操作不可恢复！") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            rememberCoroutineScope.launch(Dispatchers.IO) {
                                val job = launch(Dispatchers.Main) {
                                    delay(500)
                                    Toast.makeText(context, "开始清除", Toast.LENGTH_SHORT).show()
                                }
                                try {
                                    File(getDefaultServiceFilePath()).deleteRecursively()
                                    File(getPrivateServiceFilePath()).deleteRecursively()
                                } catch (e: Throwable) {
                                }

                                withContext(Dispatchers.Main) {
                                    job.cancel()
                                    Toast.makeText(context, "清除完成", Toast.LENGTH_SHORT).show()
                                }
                            }


                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 动态获取应用版本号
 */
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}

/**
 * 清理缓存卡片组件
 */
@Composable
private fun CacheCleanerCard(context: Context) {
    var cacheSize by remember { mutableStateOf("计算中...") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 计算缓存大小
    LaunchedEffect(Unit) {
        cacheSize = calculateCacheSize(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "清理缓存",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "当前缓存: $cacheSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = cacheSize != "计算中..." && cacheSize != "0 B"
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清理缓存")
            }
        }
    }

    // 确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("清理缓存") },
            text = { Text("确定要清理应用缓存吗？这将删除缩略图和临时文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        clearCache(context) {
                            cacheSize = calculateCacheSize(context)
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 计算应用缓存大小
 */
private fun calculateCacheSize(context: Context): String {
    return try {
        var totalSize = 0L

        // 应用内部缓存
        context.cacheDir?.let { cacheDir ->
            totalSize += getFolderSize(cacheDir)
        }

        // 应用外部缓存
        context.externalCacheDir?.let { externalCacheDir ->
            totalSize += getFolderSize(externalCacheDir)
        }

        // Coil 图片缓存
        val imageCachePath = "${context.cacheDir}/image_cache"
        totalSize += getFolderSize(java.io.File(imageCachePath))

        formatFileSize(totalSize)
    } catch (e: Exception) {
        "未知"
    }
}

/**
 * 获取文件夹大小
 */
private fun getFolderSize(file: java.io.File): Long {
    var size: Long = 0
    try {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                size += getFolderSize(child)
            }
        } else {
            size += file.length()
        }
    } catch (e: Exception) {
        // 忽略异常
    }
    return size
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * 清理应用缓存
 */
private fun clearCache(context: Context, onComplete: () -> Unit) {
    kotlinx.coroutines.MainScope().launch {
        Toast.makeText(context, "正在清理缓存...", Toast.LENGTH_SHORT).show()

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 清理内部缓存
                context.cacheDir?.deleteRecursively()

                // 清理外部缓存
                context.externalCacheDir?.deleteRecursively()

                // 清理 Coil 图片缓存
                val imageCachePath = "${context.cacheDir}/image_cache"
                java.io.File(imageCachePath).deleteRecursively()

                // 清理缩略图缓存
                val thumbnailPath = "${context.cacheDir}/thumbnails"
                java.io.File(thumbnailPath).deleteRecursively()

                // 重建目录
                context.cacheDir?.mkdirs()
                context.externalCacheDir?.mkdirs()
            } catch (e: Exception) {
                // 忽略异常
            }
        }

        Toast.makeText(context, "缓存清理完成", Toast.LENGTH_SHORT).show()
        onComplete()
    }
}