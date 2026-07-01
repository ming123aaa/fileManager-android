package com.ohuang.filemanager.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ohuang.filemanager.data.AppDownloadManager
import com.ohuang.filemanager.data.DownloadTask
import com.ohuang.filemanager.ui.components.FileType
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

private enum class DownloadFilter(val label: String) {
    ALL("全部"),
    DOWNLOADING("下载中"),
    PREPARING("准备"),
    COMPLETED("完成"),
    FAILED("失败")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(navController: NavController, onBack: () -> Unit) {
    val tasks = AppDownloadManager.tasks.map { it.value }
    val isContinueDownload by AppDownloadManager.isContinueDownload.collectAsState()
    val isPaused by AppDownloadManager.isPaused.collectAsState()
    val downloadInterval by AppDownloadManager.downloadInterval.collectAsState()

    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    val selectedTaskIds = remember { mutableStateListOf<Long>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSingleConfirmDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<DownloadTask?>(null) }
    var showNewDownloadDialog by remember { mutableStateOf(false) }
    var showDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showDownloadSettingsDialog by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingFileName by remember { mutableStateOf("") }
    var pendingFileSize by remember { mutableStateOf(0L) }
    var isLoadingFileInfo by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 存储权限申请
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                storagePermissionLauncher.launch(intent)
            }
        } else {
            val readGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val writeGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!readGranted || !writeGranted) {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    // APK 安装权限
    var pendingApkFile by remember { mutableStateOf<File?>(null) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()) {
            pendingApkFile?.let { openFileInExternalApp(it, context) }
        }
        pendingApkFile = null
    }
    if (pendingApkFile != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val granted = context.packageManager.canRequestPackageInstalls()
        if (!granted) {
            LaunchedEffect(pendingApkFile) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                installPermissionLauncher.launch(intent)
            }
        } else {
            pendingApkFile?.let { openFileInExternalApp(it, context) }
            pendingApkFile = null
        }
    }

    // 筛选：仅 sortedTasks 或 selectedFilter 变化时重算
    val filteredTasks = tasks.filter {
        when (selectedFilter) {
            DownloadFilter.ALL -> true
            DownloadFilter.DOWNLOADING ->
                it.status == DownloadTask.Status.DOWNLOADING

            DownloadFilter.PREPARING ->
                it.status == DownloadTask.Status.WAITING || it.status == DownloadTask.Status.PAUSED

            DownloadFilter.COMPLETED ->
                it.status == DownloadTask.Status.COMPLETED

            DownloadFilter.FAILED ->
                it.status == DownloadTask.Status.FAILED

        }
    }.sortedWith(
        compareBy<DownloadTask> {
            when (it.status) {
                DownloadTask.Status.DOWNLOADING -> 0
                DownloadTask.Status.WAITING -> 1
                DownloadTask.Status.PAUSED -> 2
                DownloadTask.Status.FAILED -> 3
                DownloadTask.Status.COMPLETED -> 4
            }
        }.thenByDescending { it.id }
    )

    // 各分类计数：仅 sortedTasks 变化时重算（progress 更新不触发）
    val filterCounts =
        mapOf(
            DownloadFilter.ALL to tasks.size,
            DownloadFilter.DOWNLOADING to tasks.count { it.status == DownloadTask.Status.DOWNLOADING },
            DownloadFilter.PREPARING to tasks.count {
                it.status == DownloadTask.Status.WAITING || it.status == DownloadTask.Status.PAUSED
            },
            DownloadFilter.COMPLETED to tasks.count { it.status == DownloadTask.Status.COMPLETED },
            DownloadFilter.FAILED to tasks.count { it.status == DownloadTask.Status.FAILED }
        )


    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("已选 ${selectedTaskIds.size} 项")
                    } else {
                        Text("下载")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // 多选模式：退出多选
                        IconButton(onClick = {
                            selectedTaskIds.clear()
                            isSelectionMode = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "退出多选"
                            )
                        }
                        // 全选 / 取消全选
                        val allSelected = selectedTaskIds.containsAll(filteredTasks.map { it.id })
                        TextButton(onClick = {
                            val ids = filteredTasks.map { it.id }

                            if (allSelected) {
                                selectedTaskIds.clear()
                            } else {
                                selectedTaskIds.clear()
                                selectedTaskIds.addAll(ids)
                            }
                        }) {
                            Text(text = if (allSelected) "取消全选" else "全选")
                        }
                        IconButton(onClick = {
                            if (selectedTaskIds.isNotEmpty()) {
                                showDeleteConfirmDialog = true
                            } else {
                                isSelectionMode = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除所选"
                            )
                        }
                    } else {
                        // 普通模式
                        // 新建下载按钮
                        IconButton(onClick = {
                            showNewDownloadDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新建下载"
                            )
                        }


                        if (isPaused) {
                            if (tasks.any { it.status == DownloadTask.Status.PAUSED }) {
                                IconButton(onClick = { AppDownloadManager.resumeAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "全部继续"
                                    )
                                }
                            }
                        } else {
                            if (tasks.any { it.status == DownloadTask.Status.DOWNLOADING || it.status == DownloadTask.Status.WAITING }) {
                                IconButton(onClick = { AppDownloadManager.pauseAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.PauseCircle,
                                        contentDescription = "全部暂停"
                                    )
                                }
                            } else if (tasks.any { it.status == DownloadTask.Status.PAUSED }) {
                                IconButton(onClick = { AppDownloadManager.resumeAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "全部继续"
                                    )
                                }
                            }
                        }
                        if (tasks.isNotEmpty()) {
                            IconButton(onClick = {
                                selectedTaskIds.clear()
                                isSelectionMode = true

                            }) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "选择清除"
                                )
                            }
                        }

                        // 下载设置按钮
                        IconButton(onClick = { showDownloadSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "下载设置"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            
            val downloadDirPath = remember {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "fileManager"
                ).absolutePath
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                // 下载目录（点击进入目录）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { LocalFileManagerActivity.start(context, downloadDirPath) }
                        .padding(top=8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "下载目录: Download/fileManager",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 筛选标签
            if (tasks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DownloadFilter.entries.forEach { filter ->
                        val count = filterCounts[filter] ?: 0
                        val icon = when (filter) {
                            DownloadFilter.ALL -> Icons.Default.Apps
                            DownloadFilter.DOWNLOADING -> Icons.Default.Download
                            DownloadFilter.PREPARING -> Icons.Default.HourglassTop
                            DownloadFilter.COMPLETED -> Icons.Default.CheckCircle
                            DownloadFilter.FAILED -> Icons.Default.ErrorOutline
                        }
                        val chipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (filter) {
                                DownloadFilter.ALL -> MaterialTheme.colorScheme.primaryContainer
                                DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer
                                DownloadFilter.PREPARING -> MaterialTheme.colorScheme.tertiaryContainer
                                DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                                DownloadFilter.FAILED -> MaterialTheme.colorScheme.errorContainer
                            },
                            selectedLabelColor = when (filter) {
                                DownloadFilter.ALL -> MaterialTheme.colorScheme.onPrimaryContainer
                                DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
                                DownloadFilter.PREPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                                DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                                DownloadFilter.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            },
                            selectedLeadingIconColor = when (filter) {
                                DownloadFilter.ALL -> MaterialTheme.colorScheme.onPrimaryContainer
                                DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
                                DownloadFilter.PREPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                                DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                                DownloadFilter.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = chipColors
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无下载任务",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "该分类下没有任务",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),

                ) {


                    items(filteredTasks) { sortedTask ->
                        // 从 map 读取最新 task 数据（含 progress），O(1) 查找
                        val task = sortedTask
                        DownloadTaskItem(
                            task = task,
                            isSelectionMode = isSelectionMode,
                            isSelected = task.id in selectedTaskIds,
                            onToggleSelection = {
                                if (task.id in selectedTaskIds) selectedTaskIds.remove(task.id)
                                else selectedTaskIds.add(task.id)
                            },
                            onClick = {
                                if (task.status == DownloadTask.Status.COMPLETED) {
                                    val localFile = task.localFile
                                    if (task.isFolder) {
                                        LocalFileManagerActivity.start(context, localFile.absolutePath)
                                    } else if (localFile.exists()) {
                                        if (localFile.name.endsWith(".apk", ignoreCase = true) &&
                                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()) {
                                            pendingApkFile = localFile
                                        } else {
                                            when{
                                                FileType.isMediaType(localFile.name)->{
                                                    openMediaPreview(files=listOf(localFile), currentFile = localFile,context=context, isLoop =false)
                                                }
                                                else->{
                                                    openFileInExternalApp(localFile, context)
                                                }
                                            }

                                        }
                                    }
                                }
                            },
                            onShare = {
                                if (task.status == DownloadTask.Status.COMPLETED) {
                                    val localFile = task.localFile
                                    if (task.isFolder) {
                                        LocalFileManagerActivity.start(context, localFile.absolutePath)
                                    } else if (localFile.exists()) {
                                        if (localFile.name.endsWith(".apk", ignoreCase = true) &&
                                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()) {
                                            pendingApkFile = localFile
                                        } else {
                                            openFileInExternalApp(localFile, context)
                                        }
                                    }
                                }
                            },
                            onPause = { AppDownloadManager.pauseDownload(task.id) },
                            onResume = { AppDownloadManager.resumeDownload(task.id) },
                            onCancel = { AppDownloadManager.cancelDownload(task.id) },
                            onRetry = { AppDownloadManager.retryDownload(task.id) },
                            onRemove = {
                                taskToDelete = task
                                showDeleteSingleConfirmDialog = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedTaskIds.size} 个任务吗？仅删除下载任务,文件不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    AppDownloadManager.clearTasks(selectedTaskIds.toList())
                    selectedTaskIds.clear()
                    isSelectionMode = false
                    showDeleteConfirmDialog = false
                }) {
                    Text("删除任务")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 单个删除确认弹窗
    if (showDeleteSingleConfirmDialog && taskToDelete != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("确认删除") },
            text = { Text("确定要删除「${taskToDelete!!.fileName}」下载任务吗？仅删除下载任务,文件不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    AppDownloadManager.removeTask(taskToDelete!!.id)
                    showDeleteSingleConfirmDialog = false
                    taskToDelete = null
                }) {
                    Text("删除任务")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteSingleConfirmDialog = false
                    taskToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 新建下载 - URL输入弹窗
    if (showNewDownloadDialog) {
        var pathInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("新建下载") },
            text = {
                Column {
                    Text(
                        text = "文件下载地址:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        label = { Text("文件下载地址") },
                        placeholder = { Text("http://") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingFileInfo
                    )
                    if (isLoadingFileInfo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在获取文件信息...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val path = pathInput.trim()
                        if (path.isNotEmpty() && (path.startsWith("http://") || path.startsWith("https://"))) {
                            pendingUrl = path
                            isLoadingFileInfo = true
                            scope.launch {
                                val downloadUrl = path
                                val fileInfo =
                                    com.ohuang.filemanager.data.ApiService.checkDownloadPath(
                                        downloadUrl
                                    ).awaitOrNull()
                                isLoadingFileInfo = false
                                if (fileInfo != null) {
                                    val rawFileName = fileInfo.fileName?.ifEmpty {
                                        path.substringAfterLast('/').ifEmpty { "未知文件" }
                                    } ?: path.substringAfterLast('/').ifEmpty { "未知文件" }
                                    pendingFileName = sanitizeFileName(rawFileName)
                                    pendingFileSize = fileInfo.contentLength
                                    showNewDownloadDialog = false
                                    showDownloadConfirmDialog = true
                                }
                            }
                        } else {
                            Toast.makeText(context, "下载地址错误", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = pathInput.isNotBlank() && !isLoadingFileInfo
                ) {
                    Text("下一步")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewDownloadDialog = false
                    pathInput = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 新建下载 - 确认弹窗（文件名可编辑）
    if (showDownloadConfirmDialog) {
        var editableFileName by remember { mutableStateOf(pendingFileName) }
        var fileNameError by remember { mutableStateOf<String?>(null) }

        fun validateFileName(name: String) {
            fileNameError = isValidFileName(name)
        }

        AlertDialog(
            onDismissRequest = {},
            title = { Text("确认下载") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "文件名：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editableFileName,
                        onValueChange = {
                            editableFileName = it
                            validateFileName(it)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = fileNameError != null,
                        supportingText = {
                            if (fileNameError != null) {
                                Text(fileNameError!!)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "文件大小：${formatFileSize(pendingFileSize)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val isAdd = AppDownloadManager.downloadFile(
                            serverPath = pendingUrl,
                            fileName = editableFileName.trim(),
                            totalSize = pendingFileSize
                        )
                        if (!isAdd) {
                            Toast.makeText(context, "文件已在下载列表中", Toast.LENGTH_SHORT).show()
                        }
                        showDownloadConfirmDialog = false
                        editableFileName = ""
                        pendingUrl = ""
                        pendingFileName = ""
                        pendingFileSize = 0L
                    },
                    enabled = fileNameError == null
                ) {
                    Text("开始下载")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadConfirmDialog = false
                    editableFileName = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 下载设置弹窗
    if (showDownloadSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadSettingsDialog = false },
            title = { Text("下载设置") },
            text = {
                Column {
                    // 断点续传开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "断点续传",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isContinueDownload,
                            onCheckedChange = { AppDownloadManager.setContinueDownload(it) },
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    HorizontalDivider()

                    // 下载间隔设置
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "下载间隔",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${downloadInterval}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = downloadInterval.toFloat(),
                        onValueChange = { AppDownloadManager.setDownloadInterval(it.toLong()) },
                        valueRange = 0f..1000f,
                        steps = 99
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadSettingsDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit = {},
    onShare:()-> Unit={},
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .let { mod ->
                if (isSelectionMode) {
                    mod.then(Modifier.clickable { onToggleSelection() })
                } else if (task.status == DownloadTask.Status.COMPLETED) {
                    mod.then(Modifier.clickable { onClick() })
                } else {
                    mod
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 文件名和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = when {
                            task.isFolder -> Icons.Default.Folder
                            task.status == DownloadTask.Status.COMPLETED -> Icons.Default.CheckCircle
                            task.status == DownloadTask.Status.FAILED -> Icons.Default.Error
                            task.status == DownloadTask.Status.PAUSED -> Icons.Default.PauseCircle
                            task.status == DownloadTask.Status.DOWNLOADING -> Icons.Default.Download
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (task.status) {
                            DownloadTask.Status.COMPLETED -> Color(0xFF4CAF50)
                            DownloadTask.Status.FAILED -> MaterialTheme.colorScheme.error
                            DownloadTask.Status.PAUSED -> Color(0xFFFF9800)
                            DownloadTask.Status.DOWNLOADING -> MaterialTheme.colorScheme.primary
                            DownloadTask.Status.WAITING -> Color(0xFFFFC107)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (task.status) {
                            DownloadTask.Status.WAITING -> "等待中"
                            DownloadTask.Status.DOWNLOADING -> {
                                if (task.isFolder) {
                                    val stringBuilder = StringBuilder()
                                    if (task.downloadedSize == 0L) {
                                        val str="扫描文件中,已扫描${task.totalFiles}个文件 "
                                        stringBuilder.append(str)
                                    } else {
                                        val str="${task.completedFiles}/${task.totalFiles} 文件  ${task.formatDownloadedSize()} / ${task.formatTotalSize()} "
                                        stringBuilder.append(str)
                                    }


                                    stringBuilder.toString()
                                } else {
                                    val stringBuilder = StringBuilder("${task.formatDownloadedSize()} / ${task.formatTotalSize()}")


                                    stringBuilder.toString()
                                }
                            }

                            DownloadTask.Status.PAUSED -> {

                                if (task.isFolder) "已暂停  ${task.completedFiles}/${task.totalFiles} 文件  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                                else "已暂停  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                            }

                            DownloadTask.Status.COMPLETED -> {
                                if (task.isFolder) "下载完成  ${task.totalFiles} 个文件  ${task.formatTotalSize()}"
                                else "下载完成  ${task.formatTotalSize()}"
                            }

                            DownloadTask.Status.FAILED -> task.errorMessage ?: "下载失败"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 操作按钮（多选模式下隐藏）
                if (!isSelectionMode) {
                    when (task.status) {
                        DownloadTask.Status.WAITING, DownloadTask.Status.DOWNLOADING -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "暂停",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "取消",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DownloadTask.Status.PAUSED -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "继续",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "取消",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DownloadTask.Status.FAILED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "重试",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DownloadTask.Status.COMPLETED -> {
                            IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "打开文件",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 进度条
            AnimatedVisibility(visible = task.status == DownloadTask.Status.DOWNLOADING || task.status == DownloadTask.Status.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }
    }
}



private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "未知大小"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "$bytes ${units[unitIndex]}"
    } else {
        String.format("%.2f %s", size, units[unitIndex])
    }
}

private fun isValidFileName(fileName: String): String? {
    val trimmed = fileName.trim()

    if (trimmed.isEmpty()) {
        return "文件名不能为空"
    }

    if (trimmed == "." || trimmed == "..") {
        return "文件名不能为 . 或 .."
    }

    val invalidChars = """[\\/:*?"<>|\x00-\x1F]""".toRegex()
    if (invalidChars.containsMatchIn(trimmed)) {
        return "文件名不能包含特殊字符: \\/:*?\"<>|"
    }

    val reservedNames = setOf(
        "CON",
        "PRN",
        "AUX",
        "NUL",
        "COM1",
        "COM2",
        "COM3",
        "COM4",
        "COM5",
        "COM6",
        "COM7",
        "COM8",
        "COM9",
        "LPT1",
        "LPT2",
        "LPT3",
        "LPT4",
        "LPT5",
        "LPT6",
        "LPT7",
        "LPT8",
        "LPT9"
    )
    if (reservedNames.contains(trimmed.uppercase())) {
        return "文件名不能为系统保留名称"
    }

    return null
}

private fun sanitizeFileName(fileName: String): String {
    // URL解码
    val decoded = try {
        java.net.URLDecoder.decode(fileName, "UTF-8")
    } catch (e: Exception) {
        fileName
    }

    // 将特殊字符替换为下划线
    return decoded.replace(Regex("""[\\/:*?"<>|\x00-\x1F]"""), "_")
}
