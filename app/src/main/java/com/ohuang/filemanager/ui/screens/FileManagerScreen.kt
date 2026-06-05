package com.ohuang.filemanager.ui.screens

import android.content.Intent
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
import com.ohuang.filemanager.ui.components.*
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.utils.rememberGridColumns
import com.ohuang.filemanager.ui.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(navController: NavController, onRootDirectoryChanged: (Boolean) -> Unit = {}) {
    val viewModel: FileViewModel = viewModel()
    val context = LocalContext.current
    val deviceType = rememberDeviceType()
    val gridColumns = rememberGridColumns(deviceType)

    LaunchedEffect(Unit) {
        HttpConfig.loadBaseUrl(context)
        viewModel.loadFiles()
    }

    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showToast by viewModel.showToast.collectAsState()

    LaunchedEffect(currentPath) {
        onRootDirectoryChanged(currentPath.isEmpty())
    }

    val showMkdirDialog by viewModel.showMkdirDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    val renameFile by viewModel.renameFile.collectAsState()
    val deleteFile by viewModel.deleteFile.collectAsState()

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
                    IconButton(onClick = {
                        navController.navigate("settings")
                    }) {
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
                Toolbar(
                    filterMode = viewModel.filterMode.value,
                    onFilterModeChanged = { viewModel.setFilterMode(it) },
                    sortBy = viewModel.sortBy.value,
                    sortDirection = viewModel.sortDirection.value,
                    onSortChanged = { viewModel.setSortBy(it) },
                    onSortDirectionChanged = { viewModel.toggleSortDirection() },
                    onUploadClick = {
                        val intent=Intent(context, UploadActivity::class.java)
                        intent.putExtra("path",currentPath)
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
                                // TODO: Implement download
                            },
                            onRename = { file ->
                                viewModel.showRenameDialog(file)
                            },
                            onDelete = { file ->
                                viewModel.showDeleteDialog(file)
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
