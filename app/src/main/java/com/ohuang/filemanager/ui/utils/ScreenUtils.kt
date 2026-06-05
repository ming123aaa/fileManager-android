package com.ohuang.filemanager.ui.utils

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

enum class DeviceType {
    PHONE, TABLET
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    return remember(widthDp, heightDp) {
        if (widthDp >= 600.dp) DeviceType.TABLET else DeviceType.PHONE
    }
}

@Composable
fun rememberGridColumns(deviceType: DeviceType): Int {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    return remember(widthDp, deviceType) {
        when {
            widthDp >= 1200 -> 6
            widthDp >= 900 -> 5
            widthDp >= 720 -> 4
            widthDp >= 500 -> 3
            else -> 2
        }
    }
}
