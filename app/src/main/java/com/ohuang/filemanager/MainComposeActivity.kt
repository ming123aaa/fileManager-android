package com.ohuang.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ohuang.filemanager.ui.navigation.AppNavHost
import com.ohuang.filemanager.ui.theme.FileManagerTheme
import com.ohuang.filemanager.util.BatteryOptimizationHelper

class MainComposeActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 申请通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            FileManagerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isShowDialog by remember{
                        mutableStateOf(false)
                    }
                    LaunchedEffect(Unit) {
                        isShowDialog=!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this@MainComposeActivity)
                    }
                    if (isShowDialog){
                        AlertDialog(
                            onDismissRequest = { isShowDialog = false },
                            title = { Text("设置") },
                            text = { Text("后台运行需要忽略电池优化权限,是否去设置?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        isShowDialog=false
                                        BatteryOptimizationHelper.checkAndRequest(this@MainComposeActivity){
                                            if (!it) {
                                                BatteryOptimizationHelper.openAppSettings(this@MainComposeActivity)
                                            }
                                        }
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { isShowDialog = false }
                                ) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                    AppNavHost(onBack = {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
                }
            }
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }
}