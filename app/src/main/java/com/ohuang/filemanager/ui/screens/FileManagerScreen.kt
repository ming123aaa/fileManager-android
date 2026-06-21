package com.ohuang.filemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.filemanager.data.FileItem
import com.ohuang.filemanager.MediaFileInfo
import com.ohuang.filemanager.MediaPreviewActivity
import com.ohuang.filemanager.ui.components.*
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.utils.rememberGridColumns
import com.ohuang.filemanager.ui.utils.rememberPreViewGridColumns
import com.ohuang.filemanager.ui.viewmodel.FileViewModel
import com.ohuang.filemanager.ui.viewmodel.ViewMode
import com.ohuang.filemanager.util.SPUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    navController: NavController, onBack: () -> Unit = {}, goSetting: () -> Unit
) {
    val viewModel: FileViewModel = viewModel()
    val context = LocalContext.current
    val deviceType = rememberDeviceType()



    LaunchedEffect(Unit) {
        HttpConfig.loadBaseUrl(context)
        viewModel.loadSortState(context)
        viewModel.loadViewModeState(context)
    }

    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showToast by viewModel.showToast.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    // 上传结果处理：上传成功后刷新文件列表
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.loadFiles(currentPath)
        }
    }

    // 下载权限请求（Android 10.0 之前需要）
    var pendingDownloadFile by remember { mutableStateOf<FileItem?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            pendingDownloadFile?.let { file ->
                startSystemDownload(context, file, viewModel)
                pendingDownloadFile = null
            }
        } else {
            Toast.makeText(context, "需要存储权限才能下载文件", Toast.LENGTH_SHORT).show()
            pendingDownloadFile = null
        }
    }

    // 下拉刷新状态：加载完成时自动结束刷新动画
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }
    BackHandler {

        if (viewModel.isMultiSelectMode.value){
            viewModel.exitMultiSelectMode()
        }else if (viewModel.currentPath.value.isNotEmpty()) {
            viewModel.goUp()
        } else {
            onBack()
        }

    }

    val showMkdirDialog by viewModel.showMkdirDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()

    val renameFile by viewModel.renameFile.collectAsState()
    val deleteFile by viewModel.deleteFile.collectAsState()
    val moveFile by viewModel.moveFile.collectAsState()
    val downloadFile by viewModel.downloadFile.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()
    val editFileContent by viewModel.editFileContent.collectAsState()
    val moveTargetPath by viewModel.moveTargetPath.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // 多选模式相关状态
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()
    val showBatchMoveDialog by viewModel.showBatchMoveDialog.collectAsState()
    val showBatchDownloadDialog by viewModel.showBatchDownloadDialog.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("搜索文件或文件夹...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = goSetting) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        // 多选模式下显示底部操作栏
        bottomBar = {
            if (isMultiSelectMode) {
                MultiSelectBottomBar(
                    selectedCount = selectedFiles.size,
                    totalCount = files.size,
                    onSelectAll = { viewModel.selectAllFiles() },
                    onDeselectAll = { viewModel.deselectAllFiles() },
                    onDelete = { viewModel.showBatchDeleteDialog() },
                    onMove = { viewModel.showBatchMoveDialog() },
                    onDownload = { viewModel.showBatchDownloadDialog() },
                    onCancel = { viewModel.exitMultiSelectMode() }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                val sortBy = viewModel.sortBy.collectAsState()
                val filterMode = viewModel.filterMode.collectAsState()
                val sortDirection = viewModel.sortDirection.collectAsState()
                Toolbar(
                    filterMode = filterMode.value,
                    onFilterModeChanged = { viewModel.setFilterMode(it) },
                    sortBy = sortBy.value,
                    sortDirection = sortDirection.value,
                    onSortChanged = {
                        viewModel.setSortBy(it)
                        SPUtil.put(context, "fm_sortBy", it.name)
                    },
                    onSortDirectionChanged = {
                        viewModel.toggleSortDirection()
                        SPUtil.put(context, "fm_sortDir", viewModel.sortDirection.value.name)
                    },
                    onUploadClick = {
                        val intent = Intent(context, UploadActivity::class.java)
                        intent.putExtra("path", currentPath)
                        uploadLauncher.launch(intent)
                    },
                    onCreateFolderClick = { viewModel.showMkdirDialog() },
                    onGoUpClick = { viewModel.goUp() },
                    canGoUp = currentPath.isNotEmpty(),
                    viewMode = viewMode,
                    onViewModeChanged = { newMode ->
                        viewModel.setViewMode(newMode)
                        SPUtil.put(context, "fm_viewMode", newMode.name)
                    },
                    // 多选模式参数
                    isMultiSelectMode = isMultiSelectMode,
                    onToggleMultiSelectMode = { viewModel.toggleMultiSelectMode() }
                )

                Divider()

                Breadcrumb(
                    currentPath = currentPath,
                    onNavigate = { viewModel.loadFiles(it) }
                )

                Divider()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {

                    var lazyGridState by remember {
                        mutableStateOf(LazyGridState())
                    }

                    val rememberCoroutineScope = rememberCoroutineScope()

                    FileList(
                        files = files,
                        selectedFile = selectedFile,

                        isRefreshing = isRefreshing,
                        lazyGridState = lazyGridState,
                        viewMode = viewMode,
                        getFileUrl = { file -> getFileUrl(viewModel, file) },
                        onRefresh = {
                            rememberCoroutineScope.launch {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    delay(300)
                                    viewModel.refreshFiles()
                                }
                            }

                        },
                        onFileClick = { file ->
                            if (file.isFolder) {
                                viewModel.navigateToFolder(file)
                                viewModel.setSelectedFile(null)
                            } else {
                                viewModel.setSelectedFile(file)
                                val fileType = FileType.getFileType(file.name)
                                val canEdit = FileType.isEditStringType(file.name)

                                if (FileType.isMediaType(file.name)) {

                                    // 图片或视频 -> 打开媒体预览
                                    val mediaFiles = files.filter {
                                        FileType.isMediaType(it.name)
                                    }.map {
                                        MediaFileInfo(
                                            getFileUrl(viewModel, it),
                                            it.getFileName()
                                        )
                                    }
                                    val currentIndex =
                                        mediaFiles.indexOfFirst { it.name == file.getFileName() }
                                    if (currentIndex >= 0) {
                                        MediaPreviewActivity.start(
                                            context,
                                            mediaFiles,
                                            currentIndex
                                        )
                                    }
                                } else if (fileType == FileType.AUDIO || fileType == FileType.HTML) {
                                    // 音频或HTML -> 打开 WebView
                                    val url = getFileUrl(viewModel, file)
                                    com.ohuang.filemanager.WebActivity.start(context, url)
                                } else if (canEdit && !file.isWithinTextEditorLimit()) {
                                    // 可编辑文本 -> 打开编辑弹窗
                                    viewModel.readFileContent(file)
                                } else {
                                    // 其他 -> 显示下载确认弹窗
                                    viewModel.showDownloadDialog(file)
                                }
                            }
                        },
                        onPreview = { file ->
                            if (FileType.isMediaType(file.name)) {
                                // 图片或视频 -> 打开媒体预览
                                val mediaFiles = files.filter {
                                    FileType.isMediaType(it.name)
                                }.map {
                                    MediaFileInfo(
                                        getFileUrl(viewModel, it),
                                        it.getFileName()
                                    )
                                }
                                val currentIndex =
                                    mediaFiles.indexOfFirst { it.name == file.getFileName() }
                                if (currentIndex >= 0) {
                                    MediaPreviewActivity.start(
                                        context,
                                        mediaFiles,
                                        currentIndex
                                    )
                                }
                            } else {
                                // 打开 WebView Activity 预览
                                val url = getFileUrl(viewModel, file)
                                com.ohuang.filemanager.WebActivity.start(context, url)
                            }
                        },
                        onEditString = { file ->
                            // 打开文本编辑弹窗
                            viewModel.readFileContent(file)
                        },
                        onDownload = { file ->
                            viewModel.showDownloadDialog(file)
                        },
                        onRename = { file ->
                            viewModel.showRenameDialog(file)
                        },
                        onDelete = { file ->
                            viewModel.showDeleteDialog(file)
                        },
                        onMove = { file ->
                            viewModel.showMoveDialog(file)
                        },
                        onCopyLink = { file ->
                            copyFileLinkToClipboard(context, viewModel, file)
                        },
                        onOpenInNew = { file ->
                            openFileInNewTab(context, viewModel, file)
                        },
                        // 多选模式相关参数
                        isMultiSelectMode = isMultiSelectMode,
                        selectedFiles = selectedFiles,
                        onToggleFileSelection = { file -> viewModel.toggleFileSelection(file) }
                    )

                    var isShowLoading by remember {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            lazyGridState = LazyGridState(
                                firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex,
                                firstVisibleItemScrollOffset = lazyGridState.firstVisibleItemScrollOffset
                            )
                            delay(100)
                            isShowLoading = isLoading
                        } else {
                            lazyGridState = viewModel.getLazyGridState()
                            isShowLoading = isLoading
                        }
                    }

                    if (isShowLoading) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )

                        }

                    }

                    if (!isLoading && errorMessage != null) {
                        ErrorState(errorMessage = errorMessage!!) {
                            viewModel.loadFiles(currentPath)
                        }
                    }

                }
            }

            showToast?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = {
                            viewModel.hideToastMessage()
                        }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(it)
                }
            }
        }
    }

    CreateFolderDialog(
        show = showMkdirDialog,
        onDismiss = { viewModel.hideMkdirDialog() },
        onCreate = { viewModel.createFolder(it) }
    )

    RenameDialog(
        show = showRenameDialog,
        file = renameFile,
        onDismiss = { viewModel.hideRenameDialog() },
        onRename = { newName ->
            renameFile?.let { viewModel.renameFile(it, newName) }
        }
    )

    DeleteDialog(
        show = showDeleteDialog,
        file = deleteFile,
        onDismiss = { viewModel.hideDeleteDialog() },
        onDelete = {
            deleteFile?.let { viewModel.deleteFile(it) }
        }
    )

    MoveDialog(
        show = showMoveDialog,
        file = moveFile,
        folderTree = folderTree,
        selectedPath = moveTargetPath,
        onDismiss = { viewModel.hideMoveDialog() },
        onMove = { targetPath ->
            moveFile?.let { viewModel.moveFile(it, targetPath) }
        },
        onToggleFolder = { node ->
            viewModel.toggleFolder(node)
        },
        onSelectPath = { path ->
            viewModel.setMoveTargetPath(path)
        }
    )

    DownloadDialog(
        show = showDownloadDialog,
        file = downloadFile,
        onDismiss = { viewModel.hideDownloadDialog() },
        onDownload = {
            downloadFile?.let { file ->
                viewModel.hideDownloadDialog()
                // Android 10.0 (API 29) 及以上版本不需要请求存储权限
                // Android 10.0 之前需要检查并请求读写权限
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    checkStoragePermissionAndDownload(
                        context,
                        file,
                        viewModel,
                        permissionLauncher
                    ) {
                        pendingDownloadFile = file
                    }
                } else {
                    startSystemDownload(context, file, viewModel)
                }
            }
        }
    )

    EditDialog(
        show = showEditDialog,
        file = previewFile,
        content = editFileContent,
        onDismiss = { viewModel.hideEditDialog() },
        onSave = { content ->
            previewFile?.let { file -> viewModel.saveFileContent(file, content) }
        }
    )

    LoadingDialog(show = showLoadingDialog)

    // 批量删除对话框
    BatchDeleteDialog(
        show = showBatchDeleteDialog,
        selectedFiles = selectedFiles,
        onDismiss = { viewModel.hideBatchDeleteDialog() },
        onDelete = { viewModel.deleteSelectedFiles() }
    )

    // 批量移动对话框
    BatchMoveDialog(
        show = showBatchMoveDialog,
        selectedFiles = selectedFiles,
        folderTree = folderTree,
        selectedPath = moveTargetPath,
        onDismiss = { viewModel.hideBatchMoveDialog() },
        onMove = { targetPath -> viewModel.moveSelectedFiles(targetPath) },
        onToggleFolder = { node -> viewModel.toggleFolder(node) },
        onSelectPath = { path -> viewModel.setMoveTargetPath(path) }
    )

    // 批量下载对话框
    BatchDownloadDialog(
        show = showBatchDownloadDialog,
        selectedFiles = selectedFiles,
        onDismiss = { viewModel.hideBatchDownloadDialog() },
        onDownload = {
            viewModel.downloadSelectedFiles(context)
            viewModel.hideBatchDownloadDialog()
        }
    )
}

/**
 * 统一构建文件/文件夹的完整访问 URL
 */
private fun getFileUrl(viewModel: FileViewModel, file: FileItem): String {

    return viewModel.getFileUrl(file)
}

/**
 * 检查存储权限并下载（Android 10.0 之前版本）
 */
private fun checkStoragePermissionAndDownload(
    context: Context,
    file: FileItem,
    viewModel: FileViewModel,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onPermissionNeeded: () -> Unit
) {
    val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    val hasReadPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(readPermission)
    val hasWritePermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(writePermission)

    if (hasReadPermission && hasWritePermission) {
        // 已有权限，直接下载
        startSystemDownload(context, file, viewModel)
    } else {
        // 需要请求权限
        onPermissionNeeded()
        permissionLauncher.launch(arrayOf(readPermission, writePermission))
    }
}

/**
 * 使用系统下载器下载文件
 */
private fun startSystemDownload(context: Context, file: FileItem, viewModel: FileViewModel) {
    try {
        val url = getFileUrl(viewModel, file)
        val fileName = file.getFileName()

        val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("下载 $fileName")
            setDescription("正在下载 $fileName")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "已开始下载: $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 下载文件：构建下载 URL 并通过浏览器打开（保留作为后备方案）
 */
private fun downloadFile(context: Context, viewModel: FileViewModel, file: FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getFileUrl(viewModel, file)))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 复制文件/文件夹链接到剪贴板
 */
private fun copyFileLinkToClipboard(context: Context, viewModel: FileViewModel, file: FileItem) {
    try {
        val fileUrl = getFileUrl(viewModel, file)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("file_link", fileUrl)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "链接已复制到剪贴板", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 在新标签页（外部浏览器）中打开文件/文件夹
 */
private fun openFileInNewTab(context: Context, viewModel: FileViewModel, file: FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getFileUrl(viewModel, file)))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

/**
 * 多选模式底部操作栏
 */
@Composable
fun MultiSelectBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    val deviceType = rememberDeviceType()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 选中数量提示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已选中 $selectedCount / $totalCount 项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = onSelectAll) {
                        Text("全选")
                    }
                    TextButton(onClick = onDeselectAll) {
                        Text("取消全选")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (deviceType == DeviceType.TABLET) {
                // 平板：四个按钮显示在一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 删除按钮
                    Button(
                        onClick = onDelete,
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }

                    // 移动按钮
                    Button(
                        onClick = onMove,
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("移动")
                    }

                    // 下载按钮
                    Button(
                        onClick = onDownload,
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载")
                    }

                    // 取消按钮
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                }
            } else {
                // 手机：两行显示
                // 操作按钮 - 第一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 删除按钮
                    Button(
                        onClick = onDelete,
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }

                    // 移动按钮
                    Button(
                        onClick = onMove,
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("移动")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 操作按钮 - 第二行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 下载按钮
                    Button(
                        onClick = onDownload,
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载")
                    }

                    // 取消按钮
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                }
            }

            // 底部留空
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
