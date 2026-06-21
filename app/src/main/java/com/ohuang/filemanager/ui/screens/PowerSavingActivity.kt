package com.ohuang.filemanager.ui.screens

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohuang.filemanager.ui.theme.FileManagerTheme
import kotlinx.coroutines.delay

/**
 * 省电模式Activity
 * 全屏显示黑色，防止自动锁屏，减少手机功耗
 * 点击显示提示信息，双击退出
 */
class PowerSavingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 防止自动锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 全屏显示
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContent {
            FileManagerTheme {
                PowerSavingScreen(
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除防锁屏标志
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PowerSavingScreen(
    onClose: () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    LaunchedEffect(showInfo) {
        if (showInfo) {
            delay(3000)
            showInfo = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            // 双击退出
                            onClose()
                        } else {
                            // 单击显示信息
                            showInfo = true
                            lastClickTime = currentTime
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 提示信息（点击后显示3秒）
        AnimatedVisibility(
            visible = showInfo,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatterySaver,
                    contentDescription = "省电模式",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "省电模式已启用",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "双击屏幕退出",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }
        }
    }
}