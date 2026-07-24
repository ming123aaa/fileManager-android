package com.ohuang.filemanager.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohuang.filemanager.MediaFileInfo
import com.ohuang.filemanager.MediaPreviewActivity
import com.ohuang.filemanager.data.AppDownloadManager.getDownloadDir
import com.ohuang.filemanager.data.FileItem
import com.ohuang.filemanager.ui.components.*
import com.ohuang.filemanager.ui.theme.FileManagerTheme
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.viewmodel.FilterMode
import com.ohuang.filemanager.ui.viewmodel.FolderTreeNode
import com.ohuang.filemanager.ui.viewmodel.SortBy
import com.ohuang.filemanager.ui.viewmodel.SortDirection
import com.ohuang.filemanager.ui.viewmodel.ViewMode
import com.ohuang.filemanager.util.SPUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ============================================================
// ViewModel
// ============================================================

class LocalFileViewModel(private val rootDir: String, initialSubDir: String?) : ViewModel() {

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files
    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath
    private val _selectedFile = MutableStateFlow<FileItem?>(null)
    val selectedFile: StateFlow<FileItem?> = _selectedFile
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode
    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy
    private val _sortDirection = MutableStateFlow(SortDirection.ASC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast

    // 对话框状态
    private val _showMkdirDialog = MutableStateFlow(false)
    val showMkdirDialog: StateFlow<Boolean> = _showMkdirDialog
    private val _showCreateFileDialog = MutableStateFlow(false)
    val showCreateFileDialog: StateFlow<Boolean> = _showCreateFileDialog
    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog
    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog
    private val _showTextEditor = MutableStateFlow(false)
    val showTextEditor: StateFlow<Boolean> = _showTextEditor
    private val _defaultEditMode = MutableStateFlow(false)
    val defaultEditMode: StateFlow<Boolean> = _defaultEditMode
    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog

    // 多选状态
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode
    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles
    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog
    private val _showBatchMoveDialog = MutableStateFlow(false)
    val showBatchMoveDialog: StateFlow<Boolean> = _showBatchMoveDialog


    // 导出相关状态
    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog
    private val _exportFile = MutableStateFlow<FileItem?>(null)
    val exportFile: StateFlow<FileItem?> = _exportFile
    private val _showBatchExportDialog = MutableStateFlow(false)
    val showBatchExportDialog: StateFlow<Boolean> = _showBatchExportDialog

    // 当前操作的文件
    private val _previewFile = MutableStateFlow<FileItem?>(null)
    val previewFile: StateFlow<FileItem?> = _previewFile
    private val _renameFile = MutableStateFlow<FileItem?>(null)
    val renameFile: StateFlow<FileItem?> = _renameFile
    private val _deleteFile = MutableStateFlow<FileItem?>(null)
    val deleteFile: StateFlow<FileItem?> = _deleteFile
    private val _moveFile = MutableStateFlow<FileItem?>(null)
    val moveFile: StateFlow<FileItem?> = _moveFile
    private val _editFileContent = MutableStateFlow("")
    val editFileContent: StateFlow<String> = _editFileContent
    private val _moveTargetPath = MutableStateFlow("")
    val moveTargetPath: StateFlow<String> = _moveTargetPath
    private val _folderTree = MutableStateFlow<List<FolderTreeNode>>(emptyList())
    val folderTree: StateFlow<List<FolderTreeNode>> = _folderTree

    private var allFiles: List<FileItem> = emptyList()
    private val lazyGridStateMap = mutableMapOf<String, LazyGridState>()
    private var currentRelativePath: String = initialSubDir?.trim('/').orEmpty()

    val rootName: String
        get() = rootDir.substringAfterLast("/").ifEmpty { rootDir }
    private val absoluteDir: String
        get() = if (currentRelativePath.isEmpty()) rootDir else "$rootDir/$currentRelativePath"

    fun getFullPath(file: FileItem): String = "$absoluteDir/${file.name}"
    fun canGoUp(): Boolean = currentRelativePath.isNotEmpty()

    fun getLazyGridState(): LazyGridState =
        lazyGridStateMap.getOrPut(_currentPath.value) { LazyGridState() }

    init {
        loadFiles()
    }

    // ==================== 文件加载 ====================

    fun loadFiles() {
        viewModelScope.launch { loadFilesInternal() }
    }

    private suspend fun loadFilesInternal(isRefresh: Boolean = false) {
        if (isRefresh) {
            delay(500)
        }
        _isLoading.value = true
        _errorMessage.value = null
        var hasError = false
        try {

            withContext(Dispatchers.IO) {
                val dir = File(absoluteDir)
                when {
                    !dir.exists() -> {
                        _errorMessage.value = "目录不存在: $absoluteDir"; hasError = true
                    }

                    !dir.isDirectory -> {
                        _errorMessage.value = "路径不是目录: $absoluteDir"; hasError = true
                    }

                    else -> {
                        allFiles = (dir.listFiles() ?: emptyArray()).map { f ->
                            FileItem(
                                name = f.name,
                                length = if (f.isDirectory) 0L else f.length(),
                                isFolder = f.isDirectory,
                                lastModified = f.lastModified()
                            )
                        }
                        _currentPath.value = currentRelativePath
                    }
                }
            }

            if (!hasError) applyFilters()


        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "加载失败"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refreshFiles() {
        loadFilesInternal(isRefresh = true)
        if (_errorMessage.value == null) showToastMessage("刷新完成")
    }

    // ==================== 过滤与排序 ====================

    fun applyFilters() {
        var filtered = allFiles

        when (_filterMode.value) {
            FilterMode.FILES -> filtered = filtered.filter { !it.isFolder }
            FilterMode.FOLDERS -> filtered = filtered.filter { it.isFolder }
            FilterMode.ALL -> {}
        }

        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter { it.name.lowercase().contains(query) }
        }

        val comparator: Comparator<FileItem> = when (_sortBy.value) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareBy { it.length }
            SortBy.DATE -> compareBy { it.lastModified }
        }
        val ordered =
            if (_sortDirection.value == SortDirection.DESC) comparator.reversed() else comparator
        _files.value =
            filtered.sortedWith(compareByDescending<FileItem> { it.isFolder }.then(ordered))
    }

    // ==================== 导航 ====================

    fun navigateToFolder(file: FileItem) {
        if (!file.isFolder) return
        currentRelativePath =
            if (currentRelativePath.isEmpty()) file.name else "$currentRelativePath/${file.name}"
        _selectedFile.value = null
        viewModelScope.launch { loadFilesInternal() }
    }

    fun goUp() {
        if (currentRelativePath.isEmpty()) return
        currentRelativePath =
            currentRelativePath.split("/").filter { it.isNotEmpty() }.dropLast(1).joinToString("/")
        _selectedFile.value = null
        viewModelScope.launch { loadFilesInternal() }
    }

    fun goToRelativePath(relPath: String) {
        currentRelativePath = relPath
        viewModelScope.launch { loadFilesInternal() }
    }

    // ==================== 状态设置 ====================

    fun setSelectedFile(file: FileItem?) {
        _selectedFile.value = file
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query; applyFilters()
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode; applyFilters()
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy; applyFilters()
    }

    fun setSortDirection(direction: SortDirection) {
        _sortDirection.value = direction; applyFilters()
    }

    fun toggleSortDirection() {
        _sortDirection.value =
            if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        applyFilters()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun loadSortState(context: Context) {
        runCatching {
            _sortBy.value = SortBy.valueOf(SPUtil.get(context, "lfm_sortBy", "NAME") as String)
            _sortDirection.value =
                SortDirection.valueOf(SPUtil.get(context, "lfm_sortDir", "ASC") as String)
        }
        applyFilters()
    }

    fun loadViewModeState(context: Context) {
        runCatching {
            _viewMode.value =
                ViewMode.valueOf(SPUtil.get(context, "lfm_viewMode", "GRID") as String)
        }
    }

    // ==================== 文件操作 ====================

    fun createFolder(name: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dir = File(absoluteDir, name)
                    if (dir.exists()) return@withContext showToastMessage("文件夹已存在")
                    dir.mkdirs()
                }
                _showMkdirDialog.value = false
                showToastMessage("文件夹创建成功")
                loadFilesInternal()
            } catch (e: Exception) {
                showToastMessage(e.message ?: "创建失败")
            }
        }
    }

    fun createFile(name: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(absoluteDir, name)
                    if (file.exists()) return@withContext showToastMessage("文件已存在")
                    file.createNewFile()
                }
                _showCreateFileDialog.value = false
                showToastMessage("文件创建成功")
                loadFilesInternal()
            } catch (e: Exception) {
                showToastMessage(e.message ?: "创建失败")
            }
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newFile = File(absoluteDir, newName)
                    if (newFile.exists()) return@withContext showToastMessage("目标名称已存在")
                    if (!File(getFullPath(file)).renameTo(newFile)) return@withContext showToastMessage(
                        "重命名失败"
                    )
                }
                _showRenameDialog.value = false
                showToastMessage("重命名成功")
                loadFilesInternal()
            } catch (e: Exception) {
                showToastMessage(e.message ?: "重命名失败")
            }
        }
    }

    fun deleteFile(file: FileItem) {
        if (_isLoading.value) {
            showToastMessage("正在删除中,请稍后!")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val target = File(getFullPath(file))
                    if (!target.exists()) return@withContext showToastMessage("文件不存在")
                    target.deleteRecursively()
                }
                _showDeleteDialog.value = false
                showToastMessage(if (file.isFolder) "文件夹删除成功" else "文件删除成功")
                loadFilesInternal()
            } catch (e: Exception) {
                showToastMessage(e.message ?: "删除失败")
            }
        }
    }

    fun moveFile(file: FileItem, targetRelativePath: String) {
        if (_isLoading.value) {
            showToastMessage("正在移动中,请稍后!")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val targetDir =
                        if (targetRelativePath.isEmpty()) File(rootDir) else File("$rootDir/$targetRelativePath")
                    val dest = File(targetDir, file.name)
                    if (dest.exists()) return@withContext showToastMessage("目标位置已存在同名文件")
                    if (!File(getFullPath(file)).renameTo(dest)) return@withContext showToastMessage(
                        "移动失败（可能跨存储设备）"
                    )
                }
                _showMoveDialog.value = false
                _showBatchMoveDialog.value = false
                showToastMessage("移动成功")
                loadFilesInternal()
            } catch (e: Exception) {
                showToastMessage(e.message ?: "移动失败")
            }
        }
    }

    // ==================== 导入功能 ====================

    /** 将选中的 URI 文件导入到当前目录 */
    fun importFiles(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) {

            return
        }
        viewModelScope.launch {
            _showLoadingDialog.value = true
            var successCount = 0
            var failCount = 0
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    try {
                        val fileName = getFileNameFromUri(context, uri) ?: "unknown_$successCount"
                        val dest = File(absoluteDir, fileName)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream == null) {
                            failCount++; continue
                        }
                        inputStream.use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        successCount++
                    } catch (_: Exception) {
                        failCount++
                    }
                }
            }
            _showLoadingDialog.value = false
            showToastMessage(formatBatchResult("导入", successCount, failCount))
            loadFilesInternal()
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        }
        if (name == null) name = uri.lastPathSegment
        return name
    }

    // ==================== 文本编辑 ====================

    fun readFileContent(file: FileItem, defaultEditMode: Boolean = false) {
        viewModelScope.launch {
            try {
                _showLoadingDialog.value = true
                val content = withContext(Dispatchers.IO) {
                    val f = File(getFullPath(file))
                    when {
                        !f.exists() -> {
                            showToastMessage("文件不存在"); null
                        }

                        f.length() >= 100 * 1024 -> {
                            showToastMessage("文件过大，请下载后编辑"); null
                        }

                        else -> f.readText(Charsets.UTF_8)
                    }
                }
                _showLoadingDialog.value = false
                if (content != null) {
                    _previewFile.value = file
                    _editFileContent.value = content
                    _defaultEditMode.value = defaultEditMode
                    _showTextEditor.value = true
                }
            } catch (e: Exception) {
                _showLoadingDialog.value = false
                showToastMessage(e.message ?: "读取失败")
            }
        }
    }

    fun saveFileContent(file: FileItem, content: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    File(getFullPath(file)).writeText(
                        content,
                        Charsets.UTF_8
                    )
                }
                _isLoading.value = false
                _showEditDialog.value = false
                showToastMessage("保存成功")
            } catch (e: Exception) {
                _isLoading.value = false
                showToastMessage(e.message ?: "保存失败")
            }
        }
    }

    // ==================== 文件夹树 ====================

    /** 列出指定目录下的子文件夹 */
    private suspend fun listSubFolders(dirPath: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            val dir = if (dirPath.isEmpty()) File(rootDir) else File("$rootDir/$dirPath")
            if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
            (dir.listFiles() ?: emptyArray())
                .filter { it.isDirectory }
                .map { FileItem(it.name, 0L, true, it.lastModified()) }
        }

    fun loadFolderTree(dirRelativePath: String) {
        viewModelScope.launch {
            val folders = listSubFolders(dirRelativePath)
            if (dirRelativePath.isEmpty()) {
                val rootNode = FolderTreeNode(
                    path = "", name = rootName, isExpanded = true,
                    children = folders.map { FolderTreeNode(path = it.name, name = it.name) }
                )
                _folderTree.value = listOf(rootNode)
            } else {
                updateFolderTreeWithChildren(dirRelativePath, folders)
            }
        }
    }

    fun toggleFolder(node: FolderTreeNode) {
        viewModelScope.launch {
            val currentTree = _folderTree.value.toMutableList()
            updateNodeInTree(currentTree, node.path) {
                if (it.isExpanded) it.copy(isExpanded = false)
                else it.copy(isExpanded = true, isLoading = true)
            }
            _folderTree.value = currentTree

            if (!node.isExpanded) {
                val folders = listSubFolders(node.path)
                val updatedTree = _folderTree.value.toMutableList()
                updateNodeInTree(updatedTree, node.path) {
                    it.copy(
                        isExpanded = true, isLoading = false,
                        hasSubfolders = folders.isNotEmpty(),
                        children = folders.map { f ->
                            val path = if (node.path.isEmpty()) f.name else "${node.path}/${f.name}"
                            FolderTreeNode(path = path, name = f.name)
                        }
                    )
                }
                _folderTree.value = updatedTree
            }
        }
    }

    private fun updateNodeInTree(
        tree: MutableList<FolderTreeNode>, targetPath: String,
        updater: (FolderTreeNode) -> FolderTreeNode
    ): Boolean {
        for (i in tree.indices) {
            if (tree[i].path == targetPath) {
                tree[i] = updater(tree[i])
                return true
            }
            if (tree[i].children.isNotEmpty()) {
                val children = tree[i].children.toMutableList()
                if (updateNodeInTree(children, targetPath, updater)) {
                    tree[i] = tree[i].copy(children = children)
                    return true
                }
            }
        }
        return false
    }

    private fun updateFolderTreeWithChildren(dirRelativePath: String, folders: List<FileItem>) {
        val tree = _folderTree.value.toMutableList()
        updateNodeInTree(tree, dirRelativePath) {
            it.copy(
                hasSubfolders = folders.isNotEmpty(),
                children = folders.map { f ->
                    val path =
                        if (dirRelativePath.isEmpty()) f.name else "$dirRelativePath/${f.name}"
                    FolderTreeNode(path = path, name = f.name)
                }
            )
        }
        _folderTree.value = tree
    }

    // ==================== 多选功能 ====================

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) _selectedFiles.value = emptySet()
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedFiles.value = emptySet()
    }

    fun toggleFileSelection(file: FileItem) {
        _selectedFiles.value = _selectedFiles.value.toMutableSet().apply {
            if (contains(file)) remove(file) else add(file)
        }
    }

    fun selectAllFiles() {
        _selectedFiles.value = _files.value.toSet()
    }

    fun deselectAllFiles() {
        _selectedFiles.value = emptySet()
    }

    fun showBatchDeleteDialog() {
        if (_selectedFiles.value.isNotEmpty()) _showBatchDeleteDialog.value = true
    }

    fun hideBatchDeleteDialog() {
        _showBatchDeleteDialog.value = false
    }

    fun showBatchMoveDialog() {
        if (_selectedFiles.value.isNotEmpty()) {
            _moveTargetPath.value = ""
            _showBatchMoveDialog.value = true
            loadFolderTree("")
        }
    }

    fun hideBatchMoveDialog() {
        _showBatchMoveDialog.value = false
        _moveTargetPath.value = ""
        _folderTree.value = emptyList()
    }

    // ==================== 导出功能 ====================

    fun showExportDialog(file: FileItem) {
        _exportFile.value = file
        _showExportDialog.value = true
    }

    fun hideExportDialog() {
        _showExportDialog.value = false
        _exportFile.value = null
    }

    fun showBatchExportDialog() {
        if (_selectedFiles.value.isNotEmpty()) _showBatchExportDialog.value = true
    }

    fun hideBatchExportDialog() {
        _showBatchExportDialog.value = false
    }

    /** 导出单个文件/文件夹到 Download/fileManager 目录 */
    fun exportFile(context: Context, file: FileItem) {
        if (_isLoading.value) {
            showToastMessage("正在导出中,请稍后!")
            return
        }
        viewModelScope.launch {
            _showExportDialog.value = false
            _showLoadingDialog.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val src = File(getFullPath(file))
                    if (!src.exists()) return@withContext "源文件不存在"
                    val downloadDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "fileManager"
                    )
                    if (!downloadDir.exists()) downloadDir.mkdirs()
                    val dest = File(downloadDir, file.name)
                    if (file.isFolder) {
                        src.copyRecursively(dest, overwrite = false)
                    } else {
                        src.copyTo(dest, overwrite = false)
                    }
                    "已导出到: ${dest.absolutePath}"
                }
                _showLoadingDialog.value = false
                showToastMessage(result)
            } catch (e: Exception) {
                _showLoadingDialog.value = false
                showToastMessage("导出失败: ${e.message}")
            }
        }
    }

    /** 批量导出文件到 Download/fileManager 目录 */
    fun exportSelectedFiles(context: Context) {
        if (_isLoading.value) {
            showToastMessage("正在导出中,请稍后!")
            return
        }
        if (_selectedFiles.value.isEmpty()) {
            showToastMessage("请先选择要导出的文件!")
            return
        }
        viewModelScope.launch {
            _showBatchExportDialog.value = false
            _showLoadingDialog.value = true
            var successCount = 0
            var failCount = 0
            withContext(Dispatchers.IO) {
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "fileManager"
                )
                if (!downloadDir.exists()) downloadDir.mkdirs()
                for (file in _selectedFiles.value) {
                    try {
                        val src = File(getFullPath(file))
                        val dest = File(downloadDir, file.name)
                        if (file.isFolder) {
                            src.copyRecursively(dest, overwrite = false)
                        } else {
                            src.copyTo(dest, overwrite = false)
                        }
                        successCount++
                    } catch (_: Exception) {
                        failCount++
                    }
                }
            }
            _showLoadingDialog.value = false
            showToastMessage(formatBatchResult("导出", successCount, failCount))
            _selectedFiles.value = emptySet()
            _isMultiSelectMode.value = false
        }
    }

    fun deleteSelectedFiles() {
        if (_isLoading.value) {
            showToastMessage("正在删除中,请稍后!")
            return
        }
        if (_selectedFiles.value.isEmpty()) {
            showToastMessage("请先选择要删除的文件!")
            return
        }
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            withContext(Dispatchers.IO) {
                for (file in _selectedFiles.value) {
                    try {
                        val target = File(getFullPath(file))
                        if (target.exists() && target.deleteRecursively()) successCount++ else failCount++
                    } catch (_: Exception) {
                        failCount++
                    }
                }
            }
            _showBatchDeleteDialog.value = false
            showToastMessage(formatBatchResult("删除", successCount, failCount))
            _selectedFiles.value = emptySet()
            _isMultiSelectMode.value = false
            loadFilesInternal()
        }
    }

    fun moveSelectedFiles(targetRelativePath: String) {
        if (_isLoading.value) {
            showToastMessage("正在移动中,请稍后!")
            return
        }
        if (_selectedFiles.value.isEmpty()) {
            showToastMessage("请先选择要移动的文件!")
            return
        }
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            withContext(Dispatchers.IO) {
                val targetDir =
                    if (targetRelativePath.isEmpty()) File(rootDir) else File("$rootDir/$targetRelativePath")
                for (file in _selectedFiles.value) {
                    try {
                        val src = File(getFullPath(file))
                        val dest = File(targetDir, file.name)
                        if (dest.exists() || !src.renameTo(dest)) failCount++ else successCount++
                    } catch (_: Exception) {
                        failCount++
                    }
                }
            }
            _showBatchMoveDialog.value = false
            showToastMessage(formatBatchResult("移动", successCount, failCount))
            _selectedFiles.value = emptySet()
            _isMultiSelectMode.value = false
            _folderTree.value = emptyList()
            loadFilesInternal()
        }
    }

    private fun formatBatchResult(action: String, success: Int, fail: Int): String = when {
        fail == 0 -> "成功${action} $success 个项目"
        success == 0 -> "${action}失败"
        else -> "成功${action} $success 个，失败 $fail 个"
    }

    // ==================== 对话框控制 ====================

    fun showMkdirDialog() {
        _showMkdirDialog.value = true
    }

    fun hideMkdirDialog() {
        _showMkdirDialog.value = false
    }

    fun showCreateFileDialog() {
        _showCreateFileDialog.value = true
    }

    fun hideCreateFileDialog() {
        _showCreateFileDialog.value = false
    }

    fun showRenameDialog(file: FileItem) {
        _renameFile.value = file; _showRenameDialog.value = true
    }

    fun hideRenameDialog() {
        _showRenameDialog.value = false; _renameFile.value = null
    }

    fun showDeleteDialog(file: FileItem) {
        _deleteFile.value = file; _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false; _deleteFile.value = null
    }

    fun showMoveDialog(file: FileItem) {
        _moveFile.value = file
        _moveTargetPath.value = ""
        _showMoveDialog.value = true
        loadFolderTree("")
    }

    fun hideMoveDialog() {
        _showMoveDialog.value = false
        _moveFile.value = null
        _moveTargetPath.value = ""
        _folderTree.value = emptyList()
    }

    fun setMoveTargetPath(path: String) {
        _moveTargetPath.value = path
    }

    fun showLoadingDialog() {
        _showLoadingDialog.value = true
    }

    fun hideLoadingDialog() {
        _showLoadingDialog.value = false
    }

    fun hideEditDialog() {
        _showEditDialog.value = false
        _previewFile.value = null
        _editFileContent.value = ""
    }

    fun hideTextEditor() {
        _showTextEditor.value = false
        _previewFile.value = null
        _editFileContent.value = ""
        _defaultEditMode.value = false
    }

    fun showToastMessage(message: String) {
        _showToast.value = message
        viewModelScope.launch { delay(2000); _showToast.value = null }
    }

    fun hideToastMessage() {
        _showToast.value = null
    }
}

// ============================================================
// Compose Screen
// ============================================================

/** 构建当前目录下所有媒体文件的预览列表 */
private fun buildMediaFileList(
    files: List<FileItem>,
    getFilePath: (FileItem) -> String
): List<MediaFileInfo> = files
    .filter { !it.isFolder && FileType.isMediaType(it.name) }
    .map { MediaFileInfo(getFilePath(it), it.getFileName()) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFileManagerScreen(
    viewModel: LocalFileViewModel,
    downloadEnable: Boolean,
    readOnly: Boolean,
    onBack: () -> Unit
) {
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

    LaunchedEffect(Unit) {
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

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(!isLoading) { if (!isLoading) isRefreshing = false }

    BackHandler {
        when {
            viewModel.isMultiSelectMode.value -> viewModel.exitMultiSelectMode()
            viewModel.canGoUp() -> viewModel.goUp()
            else -> onBack()
        }
    }

    val showMkdirDialog by viewModel.showMkdirDialog.collectAsState()
    val showCreateFileDialog by viewModel.showCreateFileDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showTextEditor by viewModel.showTextEditor.collectAsState()
    val defaultEditMode by viewModel.defaultEditMode.collectAsState()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()

    val renameFile by viewModel.renameFile.collectAsState()
    val deleteFile by viewModel.deleteFile.collectAsState()
    val moveFile by viewModel.moveFile.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()
    val editFileContent by viewModel.editFileContent.collectAsState()
    val moveTargetPath by viewModel.moveTargetPath.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // 导出相关状态
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val exportFile by viewModel.exportFile.collectAsState()
    val showBatchExportDialog by viewModel.showBatchExportDialog.collectAsState()

    // 导入文件选择器
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(context, uris)
        }
    }

    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()
    val showBatchMoveDialog by viewModel.showBatchMoveDialog.collectAsState()

    var pendingApkFile by remember { mutableStateOf<File?>(null) }
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("文件查找...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Clear, contentDescription = "Clear",
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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (isMultiSelectMode) {
                LocalMultiSelectBottomBar(
                    selectedCount = selectedFiles.size,
                    totalCount = files.size,
                    onSelectAll = { viewModel.selectAllFiles() },
                    onDeselectAll = { viewModel.deselectAllFiles() },
                    onDelete = { viewModel.showBatchDeleteDialog() },
                    onMove = { viewModel.showBatchMoveDialog() },
                    onExport = { viewModel.showBatchExportDialog() },
                    onCancel = { viewModel.exitMultiSelectMode() },
                    downloadEnable = downloadEnable,
                    readOnly = readOnly,
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
                        SPUtil.put(context, "lfm_sortBy", it.name)
                    },
                    onSortDirectionChanged = {
                        viewModel.toggleSortDirection()
                        SPUtil.put(context, "lfm_sortDir", viewModel.sortDirection.value.name)
                    },
                    onUploadClick = {
                        importFileLauncher.launch(arrayOf("*/*"))
                    },
                    onCreateFolderClick = { viewModel.showMkdirDialog() },
                    onCreateFileClick = { viewModel.showCreateFileDialog() },
                    onGoUpClick = { viewModel.goUp() },
                    canGoUp = viewModel.canGoUp(),
                    viewMode = viewMode,
                    onViewModeChanged = { newMode ->
                        viewModel.setViewMode(newMode)
                        SPUtil.put(context, "lfm_viewMode", newMode.name)
                    },
                    isMultiSelectMode = isMultiSelectMode,
                    onToggleMultiSelectMode = { viewModel.toggleMultiSelectMode() },
                    isLocalFile = true,
                    downloadEnable = downloadEnable,
                    readOnly = readOnly
                )

                Divider()

                Breadcrumb(
                    currentPath = currentPath,
                    onNavigate = { relPath ->
                        viewModel.setSearchQuery("")
                        viewModel.goToRelativePath(relPath)
                    },
                    rootLabel = viewModel.rootName
                )

                Divider()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    var lazyGridState by remember { mutableStateOf(LazyGridState()) }
                    val coroutineScope = rememberCoroutineScope()

                    FileList(
                        files = files,
                        selectedFile = selectedFile,
                        isRefreshing = isRefreshing,
                        lazyGridState = lazyGridState,

                        viewMode = viewMode,
                        isLocalFile = true,

                        getFileUrl = { file -> viewModel.getFullPath(file) },
                        onRefresh = {
                            coroutineScope.launch {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    viewModel.refreshFiles()
                                    isRefreshing = false
                                }
                            }
                        },
                        onFileClick = { file ->
                            handleFileClick(file, files, viewModel, context) { apkFile ->
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                    !context.packageManager.canRequestPackageInstalls()
                                ) {
                                    pendingApkFile = apkFile
                                } else {
                                    openFileInExternalApp(apkFile, context)
                                }
                            }
                        },
                        onPreview = { file -> },
                        onEditString = { file -> viewModel.readFileContent(file, defaultEditMode = true) },
                        onDownload = { file ->
                            viewModel.showExportDialog(file)
                        },
                        onRename = { file -> viewModel.showRenameDialog(file) },
                        onDelete = { file -> viewModel.showDeleteDialog(file) },
                        onMove = { file -> viewModel.showMoveDialog(file) },
                        onCopyLink = { file ->
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "file_path",
                                    viewModel.getFullPath(file)
                                )
                            )
                            Toast.makeText(context, "路径已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        onOpenInNew = { file ->
                            val f = File(viewModel.getFullPath(file))
                            if (file.name.endsWith(".apk", ignoreCase = true) &&
                                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                !context.packageManager.canRequestPackageInstalls()
                            ) {
                                pendingApkFile = f
                            } else {
                                openFileInExternalApp(f, context)
                            }
                        },
                        isMultiSelectMode = isMultiSelectMode,
                        selectedFiles = selectedFiles,
                        onToggleFileSelection = { file -> viewModel.toggleFileSelection(file) },
                        downloadEnable = downloadEnable, readOnly = readOnly,
                    )

                    var isShowLoading by remember { mutableStateOf(false) }
                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            lazyGridState = LazyGridState(
                                firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex,
                                firstVisibleItemScrollOffset = lazyGridState.firstVisibleItemScrollOffset
                            )
                            delay(100)
                            isShowLoading = true
                        } else {
                            lazyGridState = viewModel.getLazyGridState()
                            isShowLoading = false
                        }
                    }

                    if (isShowLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    if (!isLoading && errorMessage != null) {
                        ErrorState(errorMessage = errorMessage!!) { viewModel.loadFiles() }
                    }
                }
            }

            showToast?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.hideToastMessage() }) {
                            Text(
                                "关闭",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) { Text(it, color = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }

    if (!readOnly) {
        // 对话框
        CreateFolderDialog(
            show = showMkdirDialog,
            onDismiss = { viewModel.hideMkdirDialog() },
            onCreate = { viewModel.createFolder(it) })
        CreateFileDialog(
            show = showCreateFileDialog,
            onDismiss = { viewModel.hideCreateFileDialog() },
            onCreate = { viewModel.createFile(it) })
        RenameDialog(
            show = showRenameDialog,
            file = renameFile,
            onDismiss = { viewModel.hideRenameDialog() },
            onRename = { newName -> renameFile?.let { viewModel.renameFile(it, newName) } })
        DeleteDialog(
            show = showDeleteDialog,
            file = deleteFile,
            onDismiss = { viewModel.hideDeleteDialog() },
            onDelete = { deleteFile?.let { viewModel.deleteFile(it) } })
        MoveDialog(
            show = showMoveDialog,
            file = moveFile,
            folderTree = folderTree,
            selectedPath = moveTargetPath,
            onDismiss = { viewModel.hideMoveDialog() },
            onMove = { targetPath -> moveFile?.let { viewModel.moveFile(it, targetPath) } },
            onToggleFolder = { node -> viewModel.toggleFolder(node) },
            onSelectPath = { path -> viewModel.setMoveTargetPath(path) })

        BatchDeleteDialog(
            show = showBatchDeleteDialog,
            selectedFiles = selectedFiles,
            onDismiss = { viewModel.hideBatchDeleteDialog() },
            onDelete = { viewModel.deleteSelectedFiles() })
        BatchMoveDialog(
            show = showBatchMoveDialog, selectedFiles = selectedFiles, folderTree = folderTree,
            selectedPath = moveTargetPath, onDismiss = { viewModel.hideBatchMoveDialog() },
            onMove = { targetPath -> viewModel.moveSelectedFiles(targetPath) },
            onToggleFolder = { node -> viewModel.toggleFolder(node) },
            onSelectPath = { path -> viewModel.setMoveTargetPath(path) })
    }

    EditDialog(
        show = showEditDialog, file = previewFile, content = editFileContent,
        readOnly = readOnly,
        onDismiss = { viewModel.hideEditDialog() },
        onSave = { content ->
            previewFile?.let { file ->
                viewModel.saveFileContent(
                    file,
                    content
                )
            }
        })
    // 文本编辑器全屏界面
    if (showTextEditor && previewFile != null) {
        TextEditorScreen(
            filePath = viewModel.getFullPath(previewFile!!),
            fileName = previewFile!!.getFileName(),
            initialContent = editFileContent,
            readOnly = readOnly,
            defaultEditMode = defaultEditMode,
            isRemote = false,
            onBack = { viewModel.hideTextEditor() },
            onSaved = {
                viewModel.hideTextEditor()
            }
        )
    }
    LoadingDialog(show = showLoadingDialog)
    if (downloadEnable) {
        // 导出单个文件对话框
        DownloadDialog(
            show = showExportDialog,
            file = exportFile,
            isLocal = true,
            onDismiss = { viewModel.hideExportDialog() },
            onDownload = {
                exportFile?.let { file ->
                    viewModel.exportFile(context, file)
                }
            }
        )
        // 批量导出对话框
        BatchDownloadDialog(
            show = showBatchExportDialog,
            selectedFiles = selectedFiles,
            onDismiss = { viewModel.hideBatchExportDialog() },
            onDownload = {
                viewModel.exportSelectedFiles(context)
            }
        )
    }


    // APK 安装权限申请
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()
        ) {
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
}

/** 处理文件点击事件，返回需要安装权限的 APK 文件（否则返回 null） */
private fun handleFileClick(
    file: FileItem,
    files: List<FileItem>,
    viewModel: LocalFileViewModel,
    context: Context,
    onApkFile: (File) -> Unit
) {
    if (file.isFolder) {
        viewModel.navigateToFolder(file)
        viewModel.setSelectedFile(null)
        return
    }
    viewModel.setSelectedFile(file)
    when {
        FileType.isMediaType(file.name) -> openMediaPreview(files, file, viewModel, context)
        FileType.isEditStringType(file.name) && !file.isWithinTextEditorLimit() ->
            viewModel.readFileContent(file)

        file.name.endsWith(
            ".apk",
            ignoreCase = true
        ) -> onApkFile(File(viewModel.getFullPath(file)))

        else -> openFileInExternalApp(File(viewModel.getFullPath(file)), context)
    }
}

/** 处理文件预览事件 */
private fun handleFilePreview(
    file: FileItem,
    files: List<FileItem>,
    viewModel: LocalFileViewModel,
    context: Context
) {
    if (file.isFolder) {
        viewModel.navigateToFolder(file)
        viewModel.setSelectedFile(null)
        return
    }
    if (FileType.isMediaType(file.name)) {
        openMediaPreview(files, file, viewModel, context)
    } else {
        com.ohuang.filemanager.WebActivity.start(context, "file://${viewModel.getFullPath(file)}")
    }
}

/** 打开媒体预览 */
private fun openMediaPreview(
    files: List<FileItem>,
    currentFile: FileItem,
    viewModel: LocalFileViewModel,
    context: Context
) {
    val mediaFiles = buildMediaFileList(files) { viewModel.getFullPath(it) }
    val idx = mediaFiles.indexOfFirst { it.name == currentFile.getFileName() }
    if (idx >= 0) MediaPreviewActivity.start(context, mediaFiles, idx)
}

fun openMediaPreview(
    files: List<File>,
    currentFile: File, context: Context, isLoop: Boolean = true
) {
    val idx = files.indexOfFirst { it.name == currentFile.name }
    if (idx >= 0) MediaPreviewActivity.start(
        context, files.map { MediaFileInfo(url = it.absolutePath, name = it.name) }, idx,
        isLoop = isLoop
    )
}

/** 通过外部应用打开文件 */
fun openFileInExternalApp(file: File, context: Context) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(context, "没有找到可打开此文件的应用", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "txt" -> "text/plain"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "html", "htm" -> "text/html"
        "csv" -> "text/csv"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "tar" -> "application/x-tar"
        "gz" -> "application/gzip"
        "apk" -> "application/vnd.android.package-archive"
        "torrent" -> "application/x-bittorrent"
        else -> "*/*"
    }
}

/**
 * 多选模式底部操作栏
 */
@Composable
private fun LocalMultiSelectBottomBar(
    selectedCount: Int,
    totalCount: Int,
    downloadEnable: Boolean,
    readOnly: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onExport: () -> Unit,
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
                    if (!readOnly) {
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

                    if (downloadEnable) {
                        // 导出按钮
                        Button(
                            onClick = onExport,
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Output,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出")
                        }
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
                if (!readOnly) {
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
                }

                // 操作按钮 - 第二行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (downloadEnable) {
                        // 导出按钮
                        Button(
                            onClick = onExport,
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Output,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出")
                        }
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

// ============================================================
// Activity
// ============================================================

class LocalFileManagerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROOT_DIR = "root_dir"
        private const val EXTRA_SUB_DIR = "sub_dir"
        private const val EXTRA_READ_ONLY = "read_only"
        private const val EXTRA_DOWNLOAD_ENABLE = "download_enable"

        /**
         * 启动本地文件管理器
         * @param context 上下文
         * @param rootDir 根目录绝对路径
         * @param subDir 可选的初始子目录（相对于 rootDir）
         */
        fun start(
            context: Context,
            rootDir: String,
            subDir: String? = null,
            readOnly: Boolean = false
        ) {
            val intent = Intent(context, LocalFileManagerActivity::class.java).apply {
                putExtra(EXTRA_ROOT_DIR, rootDir)
                subDir?.let { putExtra(EXTRA_SUB_DIR, it) }
                putExtra(EXTRA_READ_ONLY, readOnly)
                putExtra(EXTRA_DOWNLOAD_ENABLE, !rootDir.contains(getDownloadDir().absolutePath))

            }

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootDir = intent.getStringExtra(EXTRA_ROOT_DIR)
        if (rootDir.isNullOrEmpty()) {
            Toast.makeText(this, "必须指定根目录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val subDir = intent.getStringExtra(EXTRA_SUB_DIR)
        val readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
        val downloadEnable = intent.getBooleanExtra(EXTRA_DOWNLOAD_ENABLE, true)
        val viewModel = LocalFileViewModel(rootDir, subDir)

        setContent {
            FileManagerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocalFileManagerScreen(
                        viewModel = viewModel,
                        downloadEnable = downloadEnable,
                        readOnly = readOnly,
                        onBack = { finish() })
                }
            }
        }
    }
}
