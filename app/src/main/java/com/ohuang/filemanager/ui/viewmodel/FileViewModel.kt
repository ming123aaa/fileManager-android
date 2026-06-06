package com.ohuang.filemanager.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohuang.filemanager.data.ApiService
import com.ohuang.filemanager.data.FileItem
import com.ohuang.filemanager.util.SPUtil
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private val _showPreviewDialog = MutableStateFlow(false)
    val showPreviewDialog: StateFlow<Boolean> = _showPreviewDialog

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog

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

    private var allFiles: List<FileItem> = emptyList()

    init {
        loadFiles()
    }

    fun loadFiles(path: String = "") {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val data = ApiService.getAllFiles(path).awaitOrNull { error ->
                _errorMessage.value = error.message ?: "未知错误"
            }

            _isLoading.value = false
            if (data != null) {
                allFiles = data
                _currentPath.value = path
                applyFilters()
            }
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
        filtered = filtered.sortedWith(compareByDescending<FileItem> { it.isFolder }.then(directionComparator))

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
        try { _sortBy.value = SortBy.valueOf(savedSortBy) } catch (_: Exception) {}
        try { _sortDirection.value = SortDirection.valueOf(savedSortDir) } catch (_: Exception) {}
        applyFilters()
    }

    fun createFolder(name: String) {
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

    fun readFileContent(file: FileItem) {
        _isLoading.value = true

        val fullPath = if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"

        viewModelScope.launch {
            val content = ApiService.readText(fullPath).awaitOrNull { error ->
                _isLoading.value = false
                showToastMessage(error.message ?: "未知错误")
            }

            _isLoading.value = false

            if (content != null) {
                _editFileContent.value = content
                _previewFile.value = file
                _showEditDialog.value = true
            }
        }
    }

    fun saveFileContent(file: FileItem, content: String) {
        _isLoading.value = true

        val fullPath = if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"

        viewModelScope.launch {
            val result = ApiService.writeText(fullPath, content).awaitOrNull { error ->
                _isLoading.value = false
                showToastMessage(error.message ?: "未知错误")
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
        _showMoveDialog.value = true
    }

    fun hideMoveDialog() {
        _showMoveDialog.value = false
        _moveFile.value = null
    }

    fun showPreviewDialog(file: FileItem) {
        _previewFile.value = file
        _showPreviewDialog.value = true
    }

    fun hidePreviewDialog() {
        _showPreviewDialog.value = false
        _previewFile.value = null
    }

    fun hideEditDialog() {
        _showEditDialog.value = false
        _previewFile.value = null
    }

    fun showToastMessage(message: String) {
        _showToast.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _showToast.value = null
        }
    }

    fun getFullPath(file: FileItem): String {
        return if (_currentPath.value.isEmpty()) file.name
        else "${_currentPath.value}/${file.name}"
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
