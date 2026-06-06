package com.ohuang.filemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.data.FileItem
import com.ohuang.filemanager.ui.components.*
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.utils.rememberGridColumns
import com.ohuang.filemanager.ui.viewmodel.FileViewModel
import com.ohuang.filemanager.util.SPUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(navController: NavController, onBack: () -> Unit = {},goSetting:()-> Unit) {
    val viewModel: FileViewModel = viewModel()
    val context = LocalContext.current
    val deviceType = rememberDeviceType()
    val gridColumns = rememberGridColumns(deviceType)

    LaunchedEffect(Unit) {
        HttpConfig.loadBaseUrl(context)
        viewModel.loadSortState(context)
    }

    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showToast by viewModel.showToast.collectAsState()

    // 下拉刷新状态：加载完成时自动结束刷新动画
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }
    BackHandler {
        if (viewModel.currentPath.value.isNotEmpty()){
            viewModel.goUp()
        }   else{
            onBack()
        }

    }

    val showMkdirDialog by viewModel.showMkdirDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()

    val renameFile by viewModel.renameFile.collectAsState()
    val deleteFile by viewModel.deleteFile.collectAsState()
    val moveFile by viewModel.moveFile.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    OutlinedTextField(
                        value = viewModel.searchQuery.value,
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
                            if (viewModel.searchQuery.value.isNotEmpty()) {
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                val sortBy = viewModel.sortBy.collectAsState()
                val sortDirection = viewModel.sortDirection.collectAsState()
                Toolbar(
                    filterMode = viewModel.filterMode.value,
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
                        context.startActivity(intent)
                    },
                    onCreateFolderClick = { viewModel.showMkdirDialog() },
                    onGoUpClick = { viewModel.goUp() },
                    canGoUp = currentPath.isNotEmpty()
                )

                Divider()

                Breadcrumb(
                    currentPath = currentPath,
                    onNavigate = { viewModel.loadFiles(it) }
                )

                Divider()

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (errorMessage != null) {
                        ErrorState(errorMessage = errorMessage!!) {
                            viewModel.loadFiles(currentPath)
                        }
                    } else {
                        FileList(
                            files = files,
                            selectedFile = selectedFile,
                            gridColumns = gridColumns,
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.loadFiles(currentPath)
                            },
                            onFileClick = { file ->
                                if (file.isFolder) {
                                    viewModel.navigateToFolder(file)
                                } else {
                                    viewModel.readFileContent(file)
                                }
                            },
                            onPreview = { file ->
                                viewModel.readFileContent(file)
                            },
                            onDownload = { file ->
                                downloadFile(context, viewModel, file)
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
                            }
                        )
                    }
                }
            }

            showToast?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = {}) {
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
        onDismiss = { viewModel.hideMoveDialog() },
        onMove = { targetPath ->
            moveFile?.let { viewModel.moveFile(it, targetPath) }
        }
    )
}

/**
 * 统一构建文件/文件夹的完整访问 URL
 */
private fun getFileUrl(viewModel: FileViewModel, file: FileItem): String {
    val fullPath = viewModel.getFullPath(file)
    val baseUrl = com.ohuang.filemanager.config.HttpConfig.getBaseUrl()
    val encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8").replace("+", "%20")
    return ApiService.getDownloadPath(fullPath, file.isFolder)
}

/**
 * 下载文件：构建下载 URL 并通过浏览器打开
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
            .fillMaxWidth()
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
