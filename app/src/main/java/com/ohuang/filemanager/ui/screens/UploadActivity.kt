package com.ohuang.filemanager.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.State
import androidx.lifecycle.MutableLiveData
import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.filemanager.service.UploadService
import com.ohuang.filemanager.ui.theme.FileManagerTheme
import java.io.File

class UploadActivity : ComponentActivity() {

    private val REQUEST_PERMISSION_CODE = 1001

    private var binder: UploadService.DownUpBinder? = null
    private var progressData = mutableStateOf("")

    private var isUpLoading = mutableStateOf(false)

    private var pendingAction: String? = null
    private var pendingAppend: Boolean = false

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

    private fun requestStoragePermission(action: String, append: Boolean = false) {
        pendingAction = action
        pendingAppend = append
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                when (pendingAction) {
                    "folder" -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                        startActivityForResult(intent, 2001)
                    }
                    "file" -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        startActivityForResult(intent, 2002)
                    }
                }
            } else {
                android.widget.Toast.makeText(this, "需要存储权限才能选择文件或文件夹", android.widget.Toast.LENGTH_SHORT).show()
            }
            pendingAction = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                2001 -> {
                    data?.data?.let { uri ->
                        onFolderSelected.invoke(listOf(uri))
                    }
                }
                2002 -> {
                    val uris = mutableListOf<Uri>()
                    if (data?.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            uris.add(data.clipData!!.getItemAt(i).uri)
                        }
                    } else {
                        data?.data?.let { uris.add(it) }
                    }
                    if (uris.isNotEmpty()) {
                        onFilesSelected.invoke(uris, pendingAppend)
                    }
                }
            }
        }
    }

    var onFilesSelected: (List<Uri>, Boolean) -> Unit = { _, _ -> }
    var onFolderSelected: (List<Uri>) -> Unit = {}

    override fun onResume() {
        super.onResume()
        if (pendingAction != null && checkStoragePermission()) {
            when (pendingAction) {
                "folder" -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, 2001)
                }
                "file" -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    startActivityForResult(intent, 2002)
                }
            }
            pendingAction = null
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as UploadService.DownUpBinder
            binder?.getLivedata()?.observe(this@UploadActivity) { progress ->
                progressData.value = progress
                // 检查上传完成的各种可能消息
                if (progress.contains("完成") || progress.contains("成功")) {
                    setResult(RESULT_OK)
                }
            }
            binder?.isUpload()?.addObserverForSticky(this@UploadActivity) {
                isUpLoading.value = it
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HttpConfig.loadBaseUrl(this.applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val serviceIntent = Intent(this, UploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        val initialUris = when (intent.action) {
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                uri?.let { listOf(it) }
            }
            else -> {
                null
            }
        }
        val path = intent.getStringExtra("path")




        setContent {
            FileManagerTheme {
                UploadScreen(
                    initialUris = initialUris ?: emptyList(),
                    getPath = { path ?: "" },
                    getFileName = { getFileName(it) },
                    getBinder = { binder },
                    onBack = { finish() },
                    progressData = progressData.value,
                    isUploading = isUpLoading.value,
                    onPickFiles = { isAppend ->
                        if (checkStoragePermission()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            pendingAppend = isAppend
                            startActivityForResult(intent, 2002)
                        } else {
                            requestStoragePermission("file", isAppend)
                        }
                    },
                    onPickFolder = {
                        if (checkStoragePermission()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                            startActivityForResult(intent, 2001)
                        } else {
                            requestStoragePermission("folder")
                        }
                    }
                )
            }
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
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        val uriToPath = uriToPath(uri)
        if (uriToPath != null) {
            return Uri.decode(File(uriToPath).name)
        }
        return getNotDirFileName(uri)
    }


    private fun getNotDirFileName(uri: Uri): String? {
        var name: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return name?.let { Uri.decode(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(connection)
        } catch (_: Exception) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadScreen(
    initialUris: List<Uri>,
    getPath: () -> String,
    getFileName: (Uri) -> String?,
    getBinder: () -> UploadService.DownUpBinder?,
    onBack: () -> Unit,
    progressData: String,
    isUploading: Boolean,
    onPickFiles: (Boolean) -> Unit,
    onPickFolder: () -> Unit
) {
    var selectedUris by remember { mutableStateOf(initialUris) }

    val handleFilesSelected: (List<Uri>, Boolean) -> Unit = { uris, isAppend ->
        selectedUris = if (isAppend) {
            (selectedUris + uris).distinctBy { it.toString() }
        } else {
            uris
        }
        getBinder()?.getLivedata()?.postValue("")
    }

    val handleFolderSelected: (List<Uri>) -> Unit = { uris ->
        selectedUris = (selectedUris + uris).distinctBy { it.toString() }
        getBinder()?.getLivedata()?.postValue("")
    }
    val current = LocalContext.current

    DisposableEffect(Unit) {
        val activity = current as UploadActivity
        activity.onFilesSelected = { uris, isAppend -> handleFilesSelected(uris, isAppend) }
        activity.onFolderSelected = handleFolderSelected
        onDispose {
            activity.onFilesSelected = { _, _ -> }
            activity.onFolderSelected = {}
        }
    }





    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("上传文件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .sizeIn(0.dp, 0.dp, Dp.Unspecified, 100.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "文件上传位置:\n根目录 >" + getPath().replace("/", ">"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,

                    )
            }


            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        if (!isUploading) {
                            onPickFiles(false)
                        }
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedUris.isEmpty()) "未选择文件" else "已选择 ${selectedUris.size} 个文件",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedUris.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { selectedUris = emptyList() },
                            enabled = !isUploading,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部清空")
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(selectedUris) { uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (DocumentsContract.isTreeUri(uri)) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = getFileName(uri) ?: "未知文件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        selectedUris = selectedUris.filter { it != uri }
                                    },
                                    enabled = !isUploading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when {
                isUploading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = progressData,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            getBinder()?.stopUpLoad()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("停止")
                    }
                }

                else -> {
                    if (selectedUris.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onPickFiles(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("选择文件")
                            }
                            OutlinedButton(
                                onClick = { onPickFolder() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("选择文件夹")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPickFiles(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加文件")
                            }
                            OutlinedButton(
                                onClick = { onPickFolder() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加文件夹")
                            }

                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = progressData,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                getBinder()?.startMultiUpload(selectedUris, getPath())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始上传")
                        }
                    }
                }
            }
        }
    }
}
