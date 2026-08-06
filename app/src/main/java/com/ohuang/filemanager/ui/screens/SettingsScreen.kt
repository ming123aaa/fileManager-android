package com.ohuang.filemanager.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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
import com.ohuang.filemanager.getServiceFilePath
import com.ohuang.filemanager.getServicePort
import com.ohuang.filemanager.service.UploadService
import com.ohuang.filemanager.util.BatteryOptimizationHelper
import com.ohuang.filemanager.util.ClipboardUtils
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
                            contentDescription = "返回"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("远程服务器")
            ServerUrlCard(context)

            SectionTitle("本地服务器")
            LocalServiceCard(context)

            SectionTitle("关于")
            CacheCleanerCard(context)
            DataAndClearCard(context)
            AboutCard(context)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ==================== 远程服务器 ====================

@Composable
private fun ServerUrlCard(context: Context) {
    val serverUrl = remember { mutableStateOf(HttpConfig.getBaseUrl()) }
    var isTipSave by remember { mutableStateOf(false) }
    var testMsg by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 地址输入行
            OutlinedTextField(
                value = serverUrl.value,
                onValueChange = {
                    serverUrl.value = it
                    isTipSave = (serverUrl.value != HttpConfig.getBaseUrl())
                },
                label = { Text("服务器地址") },
                placeholder = { Text("http://localhost:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (serverUrl.value.isNotEmpty()) {
                        IconButton(onClick = {
                            serverUrl.value = ""
                            isTipSave = false
                            testMsg = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 保存 + 测试连接按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 保存按钮（地址修改后显示）
                if (isTipSave && serverUrl.value.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            HttpConfig.saveBaseUrl(context, serverUrl.value)
                            serverUrl.value = HttpConfig.getBaseUrl()
                            isTipSave = false
                            testMsg = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存地址")
                    }
                }

                // 测试连接按钮
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            testMsg = "正在测试中..."
                            if (serverUrl.value.startsWith("http://") || serverUrl.value.startsWith("https://")) {
                                val data = ApiService.testConnect(serverUrl.value).awaitOrNull {
                                    testMsg = "连接失败: ${it.message}"
                                }
                                if (data != null) {
                                    testMsg = data
                                }
                            } else {
                                testMsg = "请输入正确的地址"
                            }
                        }
                    },
                    modifier = if (isTipSave && serverUrl.value.isNotEmpty()) Modifier.weight(1f) else Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("测试连接")
                }
            }

            // 测试结果
            if (testMsg.isNotEmpty()) {

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (testMsg.contains("失败") || testMsg.contains("错误"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testMsg.contains("失败") || testMsg.contains("错误"))
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }



            // 网页端入口
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, HttpConfig.getWebUrl(!HttpConfig.readOnly.value).toUri())
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
                Text("在浏览器中打开网页端")
            }
        }
    }
}

// ==================== 本地服务器 ====================

@Composable
private fun LocalServiceCard(context: Context) {
    val isRunning = AndServerManager.isRunning
    val url = AndServerManager.url
    val startFailed = AndServerManager.startServiceFair

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 服务器状态卡片
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRunning)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isRunning) Modifier.clickable {
                                ClipboardUtils.copyText(url, context)
                                Toast.makeText(context, "地址已复制", Toast.LENGTH_SHORT).show()
                            }
                            else Modifier
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunning) "服务器运行中" else "服务器未启动",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (isRunning) {
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (startFailed) {
                            Text(
                                text = "服务启动失败",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (isRunning) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 启动/停止按钮
            Button(
                onClick = {
                    if (isRunning) AndServerManager.stop() else AndServerManager.run(port = getServicePort())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRunning)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.WifiOff else Icons.Default.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "停止本地服务器" else "启动本地服务器")
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 设置项列表
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "本地服务端设置",
                subtitle = "配置端口、目录和自启选项",
                onClick = { context.startActivity(Intent(context, ServiceLauncherActivity::class.java)) }
            )

            SettingsItem(
                icon = Icons.Default.FileOpen,
                title = "本地服务器文件",
                subtitle = "浏览和管理本地服务目录",
                onClick = {
                    try {
                        val path = getServiceFilePath(context)
                        if (!File(path).exists()) File(path).mkdirs()
                    } catch (_: Throwable) {}
                    LocalFileManagerActivity.start(context, getServiceFilePath(context))
                }
            )

            SettingsItem(
                icon = Icons.Default.SettingsPower,
                title = "忽略电池优化",
                subtitle = "防止系统关闭后台服务",
                onClick = {
                    BatteryOptimizationHelper.checkAndRequest(context) {
                        if (!it) BatteryOptimizationHelper.openAppSettings(context)
                    }
                }
            )

            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "黑屏省电",
                subtitle = "降低功耗，适合长时间运行",
                onClick = { context.startActivity(Intent(context, PowerSavingActivity::class.java)) }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }
    }
}

// ==================== 关于 ====================

@Composable
private fun AboutCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Row(
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
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "文件管理器",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "版本 ${getAppVersion(context)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击访问 GitHub 项目主页",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

        }
    }
}

@Composable
private fun DataAndClearCard(context: Context) {
    var showClearDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 本地文件数据
            Surface(
                onClick = {
                    context.startActivity(Intent(context, DataMigrationActivity::class.java))
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本地文件数据",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "查看和管理数据迁移",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 清空内部文件
            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清空内部文件")
            }
        }
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认清除") },
            text = {
                Text(
                    "确定要清空内部文件吗？此操作不可恢复！",
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            val job = launch(Dispatchers.Main) {
                                delay(500)
                                Toast.makeText(context, "开始清除", Toast.LENGTH_SHORT).show()
                            }
                            try {
                                File(getDefaultServiceFilePath()).deleteRecursively()
                                File(getPrivateServiceFilePath()).deleteRecursively()
                            } catch (_: Throwable) {}
                            withContext(Dispatchers.Main) {
                                job.cancel()
                                Toast.makeText(context, "清除完成", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 缓存清理 ====================

@Composable
private fun CacheCleanerCard(context: Context) {
    var cacheSize by remember { mutableStateOf("计算中...") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cacheSize = calculateCacheSize(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "清理缓存",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "当前缓存: $cacheSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (UploadService.isUploading.value) {
                        Toast.makeText(context, "文件上传中，请稍后再清理", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    showConfirmDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cacheSize != "计算中..." && cacheSize != "0 B",
                shape = RoundedCornerShape(10.dp)
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

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null
                )
            },
            title = { Text("清理缓存") },
            text = { Text("确定要清理应用缓存吗？这将删除缩略图和临时文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (UploadService.isUploading.value) {
                            Toast.makeText(context, "文件上传中，请稍后再清理", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
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

// ==================== 工具函数 ====================

private fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}

private fun calculateCacheSize(context: Context): String {
    return try {
        var totalSize = 0L
        context.cacheDir?.let { totalSize += getFolderSize(it) }
        context.externalCacheDir?.let { totalSize += getFolderSize(it) }
        val imageCachePath = "${context.cacheDir}/image_cache"
        totalSize += getFolderSize(java.io.File(imageCachePath))
        formatFileSize(totalSize)
    } catch (e: Exception) {
        "未知"
    }
}

private fun getFolderSize(file: java.io.File): Long {
    var size: Long = 0
    try {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> size += getFolderSize(child) }
        } else {
            size += file.length()
        }
    } catch (_: Exception) {}
    return size
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun clearCache(context: Context, onComplete: () -> Unit) {
    kotlinx.coroutines.MainScope().launch {
        Toast.makeText(context, "正在清理缓存...", Toast.LENGTH_SHORT).show()
        withContext(Dispatchers.IO) {
            try {
                context.cacheDir?.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
                java.io.File("${context.cacheDir}/image_cache").deleteRecursively()
                java.io.File("${context.cacheDir}/thumbnails").deleteRecursively()
                context.cacheDir?.mkdirs()
                context.externalCacheDir?.mkdirs()
            } catch (_: Exception) {}
        }
        Toast.makeText(context, "缓存清理完成", Toast.LENGTH_SHORT).show()
        onComplete()
    }
}