package com.ohuang.filemanager.ui.viewmodel

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.data.AppDownloadManager
import com.ohuang.filemanager.data.FileItem
import com.ohuang.filemanager.util.SPUtil
import com.ohuang.kthttp.call.awaitOrNull
import com.ohuang.kthttp.download.FileInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// 文件夹树节点数据类
data class FolderTreeNode(
    val path: String,
    val name: String,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val children: List<FolderTreeNode> = emptyList(),
    val hasSubfolders: Boolean? = null
)

// 视图模式枚举
enum class ViewMode {
    GRID,    // 网格模式
    PREVIEW  // 预览模式
}

class FileViewModel : ViewModel() {
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

    private val _showMkdirDialog = MutableStateFlow(false)
    val showMkdirDialog: StateFlow<Boolean> = _showMkdirDialog

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog

    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog

    private val _showCreateFileDialog = MutableStateFlow(false)
    val showCreateFileDialog: StateFlow<Boolean> = _showCreateFileDialog

    // 多选模式相关状态
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles

    // 批量删除对话框
    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog

    // 批量移动对话框
    private val _showBatchMoveDialog = MutableStateFlow(false)
    val showBatchMoveDialog: StateFlow<Boolean> = _showBatchMoveDialog

    // 批量下载对话框
    private val _showBatchDownloadDialog = MutableStateFlow(false)
    val showBatchDownloadDialog: StateFlow<Boolean> = _showBatchDownloadDialog

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

    private val _downloadFile = MutableStateFlow<FileItem?>(null)
    val downloadFile: StateFlow<FileItem?> = _downloadFile

    // 移动对话框相关状态
    private val _moveTargetPath = MutableStateFlow("")
    val moveTargetPath: StateFlow<String> = _moveTargetPath

    private val _folderTree = MutableStateFlow<List<FolderTreeNode>>(emptyList())
    val folderTree: StateFlow<List<FolderTreeNode>> = _folderTree

    private var allFiles: List<FileItem> = emptyList()


    private var mLazyGridStateMap: SnapshotStateMap<String, LazyGridState> =
        mutableStateMapOf("" to LazyGridState())

    init {
        loadFiles()
    }


    fun getLazyGridState(): LazyGridState {
        val path = _currentPath.value
        val lazyGridState = if (mLazyGridStateMap.contains(path)) {
            mLazyGridStateMap[path]!!
        } else {
            LazyGridState().apply {
                mLazyGridStateMap[path] = this@apply
            }
        }
        val removeKeys =
            mLazyGridStateMap.filterKeys { it.isNotBlank() && !path.startsWith(it) }.map { it.key }
        removeKeys.forEach {
            mLazyGridStateMap.remove(it)
        }
        return lazyGridState
    }

    fun loadFiles(path: String = "") {


        viewModelScope.launch {
            requestFiles(path)
        }
    }

    private suspend fun requestFiles(path: String,isRefresh: Boolean=false): Boolean {
        if (isRefresh) {
            delay(500)
        }
        _isLoading.value = true
        _errorMessage.value = null
        val data = ApiService.getAllFiles(path).awaitOrNull { error ->
            _errorMessage.value = error.message ?: "未知错误"
        }



        _isLoading.value = false
        if (data != null) {
            allFiles = data
            _currentPath.value = path
            applyFilters()
        }


        return data != null
    }

    suspend fun refreshFiles() {
        if (requestFiles(_currentPath.value,true)) {
            showToastMessage("刷新完成")
        }
    }

    fun applyFilters() {
        var filtered = allFiles

        when (_filterMode.value) {
            FilterMode.ALL -> {}
            FilterMode.FILES -> filtered = filtered.filter { !it.isFolder }
            FilterMode.FOLDERS -> filtered = filtered.filter { it.isFolder }
        }

        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter { it.name.lowercase().contains(query) }
        }

        // 排序：文件夹始终优先，再按所选字段排序（与 Web 端一致）
        val comparator: Comparator<FileItem> = when (_sortBy.value) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareBy { it.length }
            SortBy.DATE -> compareBy { it.lastModified }
        }
        val directionComparator = if (_sortDirection.value == SortDirection.DESC) {
            comparator.reversed()
        } else {
            comparator
        }
        // 文件夹最先，文件在后
        filtered = filtered.sortedWith(
            compareByDescending<FileItem> { it.isFolder }.then(
                directionComparator
            )
        )

        _files.value = filtered
    }

    fun setSelectedFile(file: FileItem?) {
        _selectedFile.value = file
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
        applyFilters()
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
        applyFilters()
    }

    fun setSortDirection(direction: SortDirection) {
        _sortDirection.value = direction
        applyFilters()
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) {
            SortDirection.DESC
        } else {
            SortDirection.ASC
        }
        applyFilters()
    }

    /**
     * 从 SharedPreferences 加载排序偏好（与 Web 端 localStorage key 对齐）
     */
    fun loadSortState(context: Context) {
        val savedSortBy = SPUtil.get(context, "fm_sortBy", "NAME") as String
        val savedSortDir = SPUtil.get(context, "fm_sortDir", "ASC") as String
        try {
            _sortBy.value = SortBy.valueOf(savedSortBy)
        } catch (_: Exception) {
        }
        try {
            _sortDirection.value = SortDirection.valueOf(savedSortDir)
        } catch (_: Exception) {
        }
        applyFilters()
    }

    /**
     * 切换视图模式
     */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.PREVIEW else ViewMode.GRID
    }

    /**
     * 设置视图模式
     */
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    /**
     * 从 SharedPreferences 加载视图模式偏好
     */
    fun loadViewModeState(context: Context) {
        val savedViewMode = SPUtil.get(context, "fm_viewMode", "GRID") as String
        try {
            _viewMode.value = ViewMode.valueOf(savedViewMode)
        } catch (_: Exception) {
        }
    }

    fun createFolder(name: String) {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            val result = ApiService.createFolder(name, _currentPath.value).awaitOrNull { error ->
                _isLoading.value = false
                _showMkdirDialog.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false
            _showMkdirDialog.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage("文件夹创建成功")
                    loadFiles(_currentPath.value)
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true

        val fullPath = if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"

        viewModelScope.launch {
            val result = ApiService.renameFile(fullPath, newName).awaitOrNull { error ->
                _isLoading.value = false
                _showRenameDialog.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false
            _showRenameDialog.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage("重命名成功")
                    loadFiles(_currentPath.value)
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    fun deleteFile(file: FileItem) {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true

        val fullPath = if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"

        viewModelScope.launch {
            val result = ApiService.deleteFile(fullPath).awaitOrNull { error ->
                _isLoading.value = false
                _showDeleteDialog.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false
            _showDeleteDialog.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage(if (file.isFolder) "文件夹删除成功" else "文件删除成功")
                    loadFiles(_currentPath.value)
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    fun moveFile(file: FileItem, targetPath: String) {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true

        val fullPath = if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"

        viewModelScope.launch {
            val result = ApiService.moveFile(fullPath, targetPath).awaitOrNull { error ->
                _isLoading.value = false
                _showMoveDialog.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false
            _showMoveDialog.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage("移动成功")
                    loadFiles(_currentPath.value)
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    suspend fun downloadFileInfo(file: FileItem): FileInfo? {
        val fileUrl = getFileUrl(file)
        val fileInfo = withTimeoutOrNull(1000) {
            ApiService.checkDownloadPath(fileUrl).awaitOrNull()
        }
        return fileInfo
    }

    fun readFileContent(file: FileItem) {

        val fullPath = getFullPath(file)

        viewModelScope.launch {

            val job = launch {
                delay(100)
                showLoadingDialog()
            }


            val fileInfo = downloadFileInfo(file)
            if (fileInfo == null) {
                showToastMessage("获取文件信息失败")
                job.cancel()
                hideLoadingDialog()
                return@launch
            }

            if (file.isWithinTextEditorLimit(fileInfo.contentLength)) {
                showToastMessage("文件过大,请下载后编辑")
                job.cancel()
                hideLoadingDialog()
                return@launch
            }
            val content = ApiService.readText(fullPath).awaitOrNull { error ->
                showToastMessage(error.message ?: "未知错误")
            }
            job.cancel()
            hideLoadingDialog()


            if (content != null) {
                _previewFile.value = file
                _editFileContent.value = content
                _showEditDialog.value = true
            }

        }
    }

    fun saveFileContent(file: FileItem, content: String) {
        if (_isLoading.value) {
            return
        }

        _isLoading.value = true

        val fullPath = getFullPath(file)

        viewModelScope.launch {
            val result = ApiService.writeText(fullPath, content).awaitOrNull { error ->
                _isLoading.value = false
                showToastMessage(error.toString() ?: "未知错误")
            }

            _isLoading.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage("保存成功")
                    _showEditDialog.value = false
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    fun navigateToFolder(file: FileItem) {
        if (file.isFolder) {
            val newPath = if (_currentPath.value.isEmpty()) file.name
            else "${_currentPath.value}/${file.name}"
            loadFiles(newPath)
            _selectedFile.value = null
        }
    }

    fun goUp() {
        if (_currentPath.value.isEmpty()) return
        val parts = _currentPath.value.split("/").filter { it.isNotEmpty() }
        val newPath = parts.dropLast(1).joinToString("/")
        loadFiles(newPath)
        _selectedFile.value = null
    }

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

    fun createFile(name: String) {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            val result = ApiService.createFile(name, _currentPath.value).awaitOrNull { error ->
                _isLoading.value = false
                _showCreateFileDialog.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false
            _showCreateFileDialog.value = false

            if (result != null) {
                if (result.contains("成功")) {
                    showToastMessage("文件创建成功")
                    loadFiles(_currentPath.value)
                } else {
                    showToastMessage(result)
                }
            }
        }
    }

    fun showRenameDialog(file: FileItem) {
        _renameFile.value = file
        _showRenameDialog.value = true
    }

    fun hideRenameDialog() {
        _showRenameDialog.value = false
        _renameFile.value = null
    }

    fun showDeleteDialog(file: FileItem) {
        _deleteFile.value = file
        _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
        _deleteFile.value = null
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

    // 加载文件夹树
    fun loadFolderTree(dirPath: String) {
        viewModelScope.launch {
            val data = ApiService.getAllFiles(dirPath).awaitOrNull() ?: emptyList()
            val folders = data.filter { it.isFolder }

            if (dirPath.isEmpty()) {
                // 根目录
                val rootNode = FolderTreeNode(
                    path = "", name = "根目录", isExpanded = true, children = folders.map { f ->
                        val fullPath = f.name
                        FolderTreeNode(
                            path = fullPath, name = f.name
                        )
                    })
                _folderTree.value = listOf(rootNode)
            } else {
                // 非根目录，更新对应节点的子节点
                updateFolderTreeWithChildren(dirPath, folders)
            }
        }
    }

    // 切换文件夹展开/折叠
    fun toggleFolder(node: FolderTreeNode) {
        viewModelScope.launch {
            val currentTree = _folderTree.value.toMutableList()
            updateNodeInTree(currentTree, node.path) { existingNode ->
                val newNode = if (existingNode.isExpanded) {
                    existingNode.copy(isExpanded = false)
                } else {
                    existingNode.copy(isExpanded = true, isLoading = true)
                }
                newNode
            }
            _folderTree.value = currentTree.toList()

            if (!node.isExpanded) {
                // 需要加载子文件夹
                val data = ApiService.getAllFiles(node.path).awaitOrNull() ?: emptyList()
                val folders = data.filter { it.isFolder }

                val updatedTree = _folderTree.value.toMutableList()
                updateNodeInTree(updatedTree, node.path) { existingNode ->
                    existingNode.copy(
                        isExpanded = true,
                        isLoading = false,
                        hasSubfolders = folders.isNotEmpty(),
                        children = folders.map { f ->
                            val fullPath =
                                if (node.path.isEmpty()) f.name else "${node.path}/${f.name}"
                            FolderTreeNode(
                                path = fullPath, name = f.name
                            )
                        })
                }
                _folderTree.value = updatedTree.toList()
            }
        }
    }

    // 递归更新树中的节点
    private fun updateNodeInTree(
        tree: MutableList<FolderTreeNode>,
        targetPath: String,
        updater: (FolderTreeNode) -> FolderTreeNode
    ): Boolean {
        for (i in tree.indices) {
            if (tree[i].path == targetPath) {
                tree[i] = updater(tree[i])
                return true
            }
            if (tree[i].children.isNotEmpty()) {
                val childrenList = tree[i].children.toMutableList()
                if (updateNodeInTree(childrenList, targetPath, updater)) {
                    tree[i] = tree[i].copy(children = childrenList.toList())
                    return true
                }
            }
        }
        return false
    }

    // 更新文件夹树的子节点
    private fun updateFolderTreeWithChildren(dirPath: String, folders: List<FileItem>) {
        val currentTree = _folderTree.value.toMutableList()
        updateNodeInTree(currentTree, dirPath) { existingNode ->
            existingNode.copy(
                hasSubfolders = folders.isNotEmpty(), children = folders.map { f ->
                    val fullPath = if (dirPath.isEmpty()) f.name else "$dirPath/${f.name}"
                    FolderTreeNode(
                        path = fullPath, name = f.name
                    )
                })
        }
        _folderTree.value = currentTree.toList()
    }


    fun showDownloadDialog(file: FileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val fileInfo = downloadFileInfo(file)
            if (fileInfo != null) {
                _downloadFile.value = file.copy(length = fileInfo.contentLength)
            }
            _isLoading.value = false
            _showDownloadDialog.value = true
        }


    }

    fun hideDownloadDialog() {
        _showDownloadDialog.value = false
        _downloadFile.value = null
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

    fun showToastMessage(message: String) {
        _showToast.value = message
        viewModelScope.launch {
            delay(2000)
            _showToast.value = null
        }
    }

    fun hideToastMessage() {
        _showToast.value = null
    }

    fun getFullPath(file: FileItem): String {
        return if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"
    }

    fun getFileUrl(file: FileItem): String {
        val fullPath = getFullPath(file)
        return ApiService.getDownloadPath(fullPath, file.isFolder)
    }

    // ========== 多选功能相关方法 ==========

    /**
     * 切换多选模式
     */
    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) {
            // 退出多选模式时清空选中项
            _selectedFiles.value = emptySet()
        }
    }

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode() {
        _isMultiSelectMode.value = true
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedFiles.value = emptySet()
    }

    /**
     * 切换文件的选中状态
     */
    fun toggleFileSelection(file: FileItem) {
        val currentSelected = _selectedFiles.value.toMutableSet()
        if (currentSelected.contains(file)) {
            currentSelected.remove(file)
        } else {
            currentSelected.add(file)
        }
        _selectedFiles.value = currentSelected
    }

    /**
     * 选择文件
     */
    fun selectFile(file: FileItem) {
        val currentSelected = _selectedFiles.value.toMutableSet()
        currentSelected.add(file)
        _selectedFiles.value = currentSelected
    }

    /**
     * 取消选择文件
     */
    fun deselectFile(file: FileItem) {
        val currentSelected = _selectedFiles.value.toMutableSet()
        currentSelected.remove(file)
        _selectedFiles.value = currentSelected
    }

    /**
     * 全选当前目录下的所有文件
     */
    fun selectAllFiles() {
        _selectedFiles.value = _files.value.toSet()
    }

    /**
     * 取消全选
     */
    fun deselectAllFiles() {
        _selectedFiles.value = emptySet()
    }

    /**
     * 显示批量删除对话框
     */
    fun showBatchDeleteDialog() {
        if (_selectedFiles.value.isNotEmpty()) {
            _showBatchDeleteDialog.value = true
        }
    }

    /**
     * 隐藏批量删除对话框
     */
    fun hideBatchDeleteDialog() {
        _showBatchDeleteDialog.value = false
    }

    /**
     * 显示批量移动对话框
     */
    fun showBatchMoveDialog() {
        if (_selectedFiles.value.isNotEmpty()) {
            _moveTargetPath.value = ""
            _showBatchMoveDialog.value = true
            loadFolderTree("")
        }
    }

    /**
     * 隐藏批量移动对话框
     */
    fun hideBatchMoveDialog() {
        _showBatchMoveDialog.value = false
        _moveTargetPath.value = ""
        _folderTree.value = emptyList()
    }

    /**
     * 显示批量下载对话框
     */
    fun showBatchDownloadDialog() {
        if (_selectedFiles.value.isNotEmpty()) {
            _showBatchDownloadDialog.value = true
        }
    }

    /**
     * 隐藏批量下载对话框
     */
    fun hideBatchDownloadDialog() {
        _showBatchDownloadDialog.value = false
    }

    /**
     * 批量删除选中的文件
     */
    fun deleteSelectedFiles() {
        if (_isLoading.value || _selectedFiles.value.isEmpty()) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            var successCount = 0
            var failCount = 0

            for (file in _selectedFiles.value) {
                val fullPath = if (_currentPath.value.isEmpty()) file.name
                else "${_currentPath.value}/${file.name}"

                val result = ApiService.deleteFile(fullPath).awaitOrNull()
                if (result != null && result.contains("成功")) {
                    successCount++
                } else {
                    failCount++
                }
            }

            _isLoading.value = false
            _showBatchDeleteDialog.value = false

            val message = when {
                failCount == 0 -> "成功删除 $successCount 个项目"
                successCount == 0 -> "删除失败"
                else -> "成功删除 $successCount 个，失败 $failCount 个"
            }
            showToastMessage(message)

            // 清空选中项并退出多选模式
            _selectedFiles.value = emptySet()
            _isMultiSelectMode.value = false

            // 刷新文件列表
            loadFiles(_currentPath.value)
        }
    }

    /**
     * 批量移动选中的文件
     */
    fun moveSelectedFiles(targetPath: String) {
        if (_isLoading.value || _selectedFiles.value.isEmpty()) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            var successCount = 0
            var failCount = 0

            for (file in _selectedFiles.value) {
                val fullPath = if (_currentPath.value.isEmpty()) file.name
                else "${_currentPath.value}/${file.name}"

                val result = ApiService.moveFile(fullPath, targetPath).awaitOrNull()
                if (result != null && result.contains("成功")) {
                    successCount++
                } else {
                    failCount++
                }
            }

            _isLoading.value = false
            _showBatchMoveDialog.value = false

            val message = when {
                failCount == 0 -> "成功移动 $successCount 个项目"
                successCount == 0 -> "移动失败"
                else -> "成功移动 $successCount 个，失败 $failCount 个"
            }
            showToastMessage(message)

            // 清空选中项并退出多选模式
            _selectedFiles.value = emptySet()
            _isMultiSelectMode.value = false
            _folderTree.value = emptyList()

            // 刷新文件列表
            loadFiles(_currentPath.value)
        }
    }

    /**
     * 批量下载选中的文件和文件夹
     */
    fun downloadSelectedFiles(context: android.content.Context) {
        if (_selectedFiles.value.isEmpty()) {
            return
        }

        var skipCount = 0
        for (file in _selectedFiles.value) {
            if (file.isFolder) {
                val folderPath = getFullPath(file)

                val added = AppDownloadManager.downloadFolder(folderPath, file.name)
                if (!added) skipCount++

            } else {
                val fullPath = getFullPath(file)
                val added =
                    AppDownloadManager.downloadFile(fullPath, file.getFileName(), file.length)
                if (!added) skipCount++
            }
        }

        val addedCount = _selectedFiles.value.size - skipCount
        if (skipCount > 0) {
            showToastMessage("已添加 $addedCount 个下载任务，跳过 $skipCount 个重复文件")
        } else {
            showToastMessage("已添加 ${_selectedFiles.value.size} 个下载任务")
        }

        // 清空选中项并退出多选模式
        _selectedFiles.value = emptySet()
        _isMultiSelectMode.value = false
    }

    /**
     * 下载单个文件或文件夹
     */
    fun downloadFileOrFolder(context: android.content.Context, file: FileItem) {
        if (file.isFolder) {
            val folderPath = getFullPath(file)

            val added = AppDownloadManager.downloadFolder(folderPath, file.name)
            if (added) {
                showToastMessage("已开始下载: ${file.getFileName()}")
            } else {
                showToastMessage("文件已在下载列表中: ${file.getFileName()}")
            }

        } else {
            val fullPath = getFullPath(file)
            val added = AppDownloadManager.downloadFile(fullPath, file.getFileName(), file.length)
            if (added) {
                showToastMessage("已开始下载: ${file.getFileName()}")
            } else {
                showToastMessage("文件已在下载列表中: ${file.getFileName()}")
            }
        }
    }
}

enum class FilterMode {
    ALL, FILES, FOLDERS
}

enum class SortBy {
    NAME, SIZE, DATE
}

enum class SortDirection {
    ASC, DESC
}
