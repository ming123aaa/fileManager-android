package com.ohuang.filemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ohuang.filemanager.data.FileItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun FileList(
    files: List<FileItem>,
    selectedFile: FileItem?,
    onFileClick: (FileItem) -> Unit,
    lazyGridState: LazyGridState,
    gridColumns: Int = 2,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onPreview: (FileItem) -> Unit = {},
    onEditString: (FileItem) -> Unit = {},
    onDownload: (FileItem) -> Unit = {},
    onRename: (FileItem) -> Unit = {},
    onDelete: (FileItem) -> Unit = {},
    onMove: (FileItem) -> Unit = {},
    onCopyLink: (FileItem) -> Unit = {},
    onOpenInNew: (FileItem) -> Unit = {}
) {

    // 当前正在显示上下文菜单的文件（仅一个），确保同一时间只有一个菜单打开
    var contextMenuFile by remember { mutableStateOf<FileItem?>(null) }

    // 协程作用域用于滚动动画
    val coroutineScope = rememberCoroutineScope()

    // 监听滚动状态，超过一定距离显示返回顶部按钮
    var showScrollToTopButton by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(lazyGridState.firstVisibleItemIndex) {
        if (lazyGridState.firstVisibleItemIndex > 1) {
            showScrollToTopButton = true
        } else {
            showScrollToTopButton = false
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        PullRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()

        ) {


            if (files.isEmpty()) {
                EmptyState()
                return@PullRefresh
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                state = lazyGridState
            ) {
                items(files) { file ->
                    FileCard(
                        file = file,
                        onClick = { onFileClick(file) },
                        onLongClick = {
                            contextMenuFile = file
                        },
                        isSelected = selectedFile?.name == file.name,
                        // 仅当前卡片显示上下文菜单
                        showContextMenu = contextMenuFile == file,
                        onContextMenuDismiss = {
                            contextMenuFile = null
                        },
                        onOpen = { f ->
                            contextMenuFile = null
                            if (f.isFolder) onFileClick(f) else onPreview(f)
                        },
                        onPreview = { f ->
                            contextMenuFile = null
                            onPreview(f)
                        },
                        onEditString = { f ->
                            contextMenuFile = null
                            onEditString(f)
                        },
                        onDownload = { f ->
                            contextMenuFile = null
                            onDownload(f)
                        },
                        onRename = { f ->
                            contextMenuFile = null
                            onRename(f)
                        },
                        onMove = { f ->
                            contextMenuFile = null
                            onMove(f)
                        },
                        onDelete = { f ->
                            contextMenuFile = null
                            onDelete(f)
                        },
                        onCopyLink = { f ->
                            contextMenuFile = null
                            onCopyLink(f)
                        },
                        onOpenInNew = { f ->
                            contextMenuFile = null
                            onOpenInNew(f)
                        }
                    )
                }

                item {
                    Column(modifier = Modifier.height(80.dp)) { }

                }

            }


        }

        // 滚动到顶部按钮
        AnimatedVisibility(
            visible = showScrollToTopButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyGridState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "滚动到顶部"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullRefresh(
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    // 下拉刷新：PullToRefreshContainer 内部处理松手检测，自动 startRefresh()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        this.content()
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter)

        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Empty folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "此目录为空",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击上方\"上传\"按钮添加文件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
