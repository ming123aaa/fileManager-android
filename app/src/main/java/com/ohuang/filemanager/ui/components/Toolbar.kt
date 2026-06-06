package com.ohuang.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ohuang.filemanager.ui.viewmodel.FilterMode
import com.ohuang.filemanager.ui.viewmodel.SortBy
import com.ohuang.filemanager.ui.viewmodel.SortDirection

@Composable
fun Toolbar(
    filterMode: FilterMode,
    onFilterModeChanged: (FilterMode) -> Unit,
    sortBy: SortBy,
    sortDirection: SortDirection,
    onSortChanged: (SortBy) -> Unit,
    onSortDirectionChanged: () -> Unit,
    onUploadClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onGoUpClick: () -> Unit,
    canGoUp: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoUpClick) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Go up",
                    tint = if (canGoUp) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilterDropdown(
                filterMode = filterMode,
                onFilterModeChanged = onFilterModeChanged
            )

            Spacer(modifier = Modifier.weight(1f))

            SortSelector(
                sortBy = sortBy,
                sortDirection = sortDirection,
                onSortChanged = onSortChanged,
                onSortDirectionChanged = onSortDirectionChanged
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onCreateFolderClick) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "Create folder",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onUploadClick) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(
    filterMode: FilterMode,
    onFilterModeChanged: (FilterMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val filterText = when (filterMode) {
        FilterMode.ALL -> "全部"
        FilterMode.FILES -> "文件"
        FilterMode.FOLDERS -> "文件夹"
    }

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = filterText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Filter dropdown",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    onFilterModeChanged(FilterMode.ALL)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("文件") },
                onClick = {
                    onFilterModeChanged(FilterMode.FILES)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("文件夹") },
                onClick = {
                    onFilterModeChanged(FilterMode.FOLDERS)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun SortSelector(
    sortBy: SortBy,
    sortDirection: SortDirection,
    onSortChanged: (SortBy) -> Unit,
    onSortDirectionChanged: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 点击文字区域展开排序字段选择
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val sortText = when (sortBy) {
                    SortBy.NAME -> "名称"
                    SortBy.SIZE -> "大小"
                    SortBy.DATE -> "时间"
                }
                Text(
                    text = sortText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Sort field",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 点击箭头切换排序方向
            IconButton(
                onClick = onSortDirectionChanged,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.ASC) Icons.Default.ArrowUpward
                                 else Icons.Default.ArrowDownward,
                    contentDescription = "Sort direction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("名称") },
                onClick = {
                    onSortChanged(SortBy.NAME)
                    expanded = false
                },
                leadingIcon = {
                    if (sortBy == SortBy.NAME) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("大小") },
                onClick = {
                    onSortChanged(SortBy.SIZE)
                    expanded = false
                },
                leadingIcon = {
                    if (sortBy == SortBy.SIZE) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("时间") },
                onClick = {
                    onSortChanged(SortBy.DATE)
                    expanded = false
                },
                leadingIcon = {
                    if (sortBy == SortBy.DATE) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    }
}
