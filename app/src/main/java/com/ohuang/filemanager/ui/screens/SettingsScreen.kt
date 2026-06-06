package com.ohuang.filemanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ohuang.filemanager.config.HttpConfig
import com.ohuang.filemanager.data.ApiService
import com.ohuang.kthttp.call.awaitOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController,onBack:()-> Unit) {
    val context = LocalContext.current
    val serverUrl = remember { mutableStateOf(HttpConfig.getBaseUrl()) }
    val isSaving = remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {

                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "服务器配置",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                )

                IconButton(
                    onClick = {
                        if (serverUrl.value.isNotEmpty()) {
                            isSaving.value = true
                            HttpConfig.saveBaseUrl(context, serverUrl.value)
                            isSaving.value = false

                        }
                    },
                    enabled = !isSaving.value && serverUrl.value.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save"
                    )
                }
            }
            OutlinedTextField(
                value = serverUrl.value,
                onValueChange = { serverUrl.value = it },
                label = { Text("服务器地址") },
                placeholder = { Text("http://localhost:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "提示: 请确保服务器地址正确，包括协议(http/https)和端口号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var testMsg by remember { mutableStateOf("") }
            val coroutineScope=rememberCoroutineScope()
            Row (modifier = Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically){
                Button(onClick = {
                    coroutineScope.launch {
                        testMsg="正在测试中..."
                        var data=ApiService.testConnect(serverUrl.value).awaitOrNull {
                            testMsg=it.message?:"请求失败"
                        }
                        if (data!=null){
                            testMsg=data
                        }
                    }
                }) {
                    Text("测试地址")
                }

                Text("测试结果:${testMsg}", modifier = Modifier.padding(horizontal = 5.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))



            // Web端 入口按钮
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(HttpConfig.getWebUrl()))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("网页版")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "关于",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "文件管理器",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 ${getAppVersion(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 动态获取应用版本号
 */
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}