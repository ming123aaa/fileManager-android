package com.ohuang.filemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    val selectedTaskIds = remember { mutableStateListOf<Long>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                        Text("下载列表")
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
                        // 多选模式：全选 / 取消全选
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

                        if (isPaused) {
                            if (tasks.any { it.status == DownloadTask.Status.PAUSED }) {
                                IconButton(onClick = { AppDownloadManager.resumeAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "全部继续"
                                    )
                                }
                            }
                        }else {
                            if (tasks.any { it.status == DownloadTask.Status.DOWNLOADING || it.status == DownloadTask.Status.WAITING }) {
                                IconButton(onClick = { AppDownloadManager.pauseAll() }) {
                                    Icon(
                                        imageVector = Icons.Default.PauseCircle,
                                        contentDescription = "全部暂停"
                                    )
                                }
                            }else if (tasks.any { it.status == DownloadTask.Status.PAUSED }) {
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
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // 断点续传开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "跳过已下载的内容",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isContinueDownload,
                    onCheckedChange = { AppDownloadManager.setContinueDownload(it) }
                )
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
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { sortedTask ->
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
                            onPause = { AppDownloadManager.pauseDownload(task.id) },
                            onResume = { AppDownloadManager.resumeDownload(task.id) },
                            onCancel = { AppDownloadManager.cancelDownload(task.id) },
                            onRetry = { AppDownloadManager.retryDownload(task.id) },
                            onRemove = { AppDownloadManager.removeTask(task.id) },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedTaskIds.size} 个任务吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    AppDownloadManager.clearTasks(selectedTaskIds.toList())
                    selectedTaskIds.clear()
                    isSelectionMode = false
                    showDeleteConfirmDialog = false
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
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
                                    if (task.downloadedSize == 0L) {
                                        "扫描文件中,已扫描${task.totalFiles}个文件"
                                    } else {
                                        "${task.completedFiles}/${task.totalFiles} 文件  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                                    }
                                } else {
                                    "${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
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

private fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
    }
}
