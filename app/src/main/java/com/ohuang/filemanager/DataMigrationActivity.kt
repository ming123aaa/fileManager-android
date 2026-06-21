package com.ohuang.filemanager

import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import com.ohuang.filemanager.ui.theme.FileManagerTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ohuang.filemanager.ui.components.LoadingDialog
import com.ohuang.filemanager.util.ClipboardUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DataMigrationActivity : ComponentActivity() {

    private val REQUEST_PERMISSION_CODE = 1001

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            FileManagerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            SmallTopAppBar(
                                title = { Text("本地服务器文件") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        finish()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                actions = {}
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            Content()
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 5.dp)
        ) {

            val baseLacalFilePath: String by remember {
                mutableStateOf(getServiceFilePath())
            }

            var localFilePath: String by remember {
                mutableStateOf(baseLacalFilePath)
            }

            var targetFilePath: String by remember {
                mutableStateOf("")
            }

            var tipString: String by remember {
                mutableStateOf("")
            }


            var isCopyData: Boolean by remember {
                mutableStateOf(false)
            }


            var showLoading: Boolean by remember {
                mutableStateOf(false)
            }

            var showFolderDialog by remember {
                mutableStateOf(false)
            }

            var currentDir by remember {
                mutableStateOf(File(baseLacalFilePath))
            }

            LoadingDialog(show = showLoading)

            if (showFolderDialog) {
                val folders = remember(currentDir.absolutePath) {
                    currentDir.listFiles { file -> file.isDirectory }?.filter { it.canRead() } ?: emptyList()
                }
                val canGoBack = currentDir.absolutePath != baseLacalFilePath

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showFolderDialog = false },
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            if (canGoBack) {
                                IconButton(onClick = {
                                    currentDir = currentDir.parentFile ?: currentDir
                                }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                                }
                            }
                            Text(
                                "选择文件夹",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().horizontalScroll(
                                rememberScrollState()
                            )) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    currentDir.absolutePath.replace(baseLacalFilePath,"根目录"),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,

                                )
                            }

                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(folders) { folder ->
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentDir = folder
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = folder.name,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowRight,
                                            contentDescription = null,
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (folders.isEmpty()) {
                                    item {
                                        Text(
                                            "无文件夹",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            localFilePath = currentDir.absolutePath
                            showFolderDialog = false
                            currentDir = File(baseLacalFilePath)
                        }) {
                            Text("确认选择")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showFolderDialog = false
                            currentDir = File(baseLacalFilePath)
                        }) {
                            Text("取消")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFolderDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本地服务器文件目录:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = baseLacalFilePath,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "当前选择的路径:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().horizontalScroll(
                            rememberScrollState()
                        )) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                currentDir.absolutePath.replace(baseLacalFilePath,"根目录"),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,

                                )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "选择",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            Spacer(modifier = Modifier.height(15.dp))

            val directoryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri: Uri? ->
                    uri?.let {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val path = uriToPath(it)
                        path?.let { filePath ->
                            targetFilePath = filePath
                        }
                    }
                }
            )

            Button(onClick = {
                if (checkStoragePermission()) {
                    directoryLauncher.launch(null)
                } else {
                    requestStoragePermission()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择目标文件夹")
            }
            TextField(
                value = targetFilePath,
                onValueChange = { targetFilePath = it },
                label = { Text("选择或输入目标文件夹路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        ClipboardUtils.copyText(targetFilePath, this@DataMigrationActivity)
                        Toast.makeText(this@DataMigrationActivity, "已复制", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Output, contentDescription = "复制")
                    }
                }
            )




            Spacer(modifier = Modifier.height(15.dp))
            fun moveDir(isMoveToLocal: Boolean, isCopy: Boolean = true) {
                val op = if (isCopy) "复制" else "移动"
                if (targetFilePath.isBlank()) {
                    tipString = "请选择目标文件夹"
                    return
                }
                val inFile = if (isMoveToLocal) {
                    targetFilePath
                } else {
                    localFilePath
                }
                val outFile = if (isMoveToLocal) {
                    localFilePath
                } else {
                    targetFilePath
                }
                val oldFile = File(inFile)
                val newFile = File(outFile)
                if (!newFile.exists()) {
                    newFile.mkdirs()
                }
                val file = File(newFile.absolutePath, oldFile.name)

                if (oldFile.exists() && (oldFile.listFiles()?.size ?: 0) > 0) {

                    if (file.exists()) {
                        tipString = "文件已存在-" + file.absolutePath
                        return
                    }
                    val str = if (if (isCopy) {
                            oldFile.copyRecursively(file)
                        } else {
                            oldFile.renameTo(file)
                        }
                    ) {
                        op + "成功"
                    } else {
                        op + "失败"
                    }
                    tipString = op + "到${file.absolutePath}  \n${str} "
                } else {
                    tipString = "无需要${op}的内容"
                }
            }

            val coroutineScope = rememberCoroutineScope()

            Row {
                Switch(checked = isCopyData, onCheckedChange = {
                    isCopyData = it
                })
                Text(
                    "当前方式:" + if (isCopyData) "复制文件" else "移动文件",
                    modifier = Modifier.align(
                        Alignment.CenterVertically
                    ).padding(horizontal = 3.dp)
                )
            }


            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    showLoading = true
                    moveDir(true, isCopyData)
                    showLoading = false
                }


            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Input,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导入(目标文件夹->本地)")
            }


            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    showLoading = true
                    moveDir(false, isCopyData)
                    showLoading = false
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Output,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出(本地->目标文件夹")
            }

            Text("" + tipString)
        }

    }


    fun openFolder(targetPath: String) {

        try {

            // 1. 将文件路径转换为 Uri
            // 注意：这个方法主要适用于外部存储，如 /storage/emulated/0/
            // 对于 /Android/data/ 等特殊目录，在Android 11+上访问受限
            val initialUri = convertPathToUri(targetPath) ?: return


            // 2. 创建 Intent
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // 3. 设置初始显示的目录
                putExtra(EXTRA_INITIAL_URI, initialUri)
            }

            // 4. 启动文件选择器
            startActivity(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // 一个简单的转换函数，示例场景为外部存储根目录
    private fun convertPathToUri(path: String): Uri? {
        val externalStoragePath = "/storage/emulated/0/"
        return if (path.startsWith(externalStoragePath)) {
            // 移除根路径前缀，并将剩余部分进行URL编码，构造成Document Uri
            val subPath = path.substring(externalStoragePath.length).replace("/", "%2F")
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$subPath")
        } else {
            // 对于其他路径，构造可能不准确，这里返回null并降级处理
            null
        }
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        val readGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION_CODE
            )
        }
    }


    private fun uriToPath(uri: Uri): String? {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}"
            } else {
                split[1]
            }
        } catch (e: Exception) {
            null
        }
    }


}



