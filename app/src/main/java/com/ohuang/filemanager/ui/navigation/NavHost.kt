package com.ohuang.filemanager.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ohuang.filemanager.ui.screens.FileManagerScreen
import com.ohuang.filemanager.ui.screens.SettingsScreen
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.FragmentBox
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.utils.rememberSettingScreenInRight
import com.ohuang.filemanager.ui.utils.rememberSettingScreenWidth

@Composable
fun AppNavHost(onBack: () -> Unit = {}) {
    val navController = rememberNavController()
    val deviceType = rememberDeviceType()
    val settingWidth= rememberSettingScreenWidth()
    var showSetting by remember { mutableStateOf(false) }
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(showSetting) {
        if (showSetting){
            state.targetState=true
        }else{
            state.targetState=false
        }
    }

    NavHost(
        navController = navController,
        startDestination = "filemanager"
    ) {
        composable("filemanager") {
            Row() {

                FragmentBox (modifier = Modifier.weight(1f),
                    isChange = if (deviceType==DeviceType.TABLET){ state.isIdle }else{true}
                ) {
                    FileManagerScreen(navController, onBack = onBack, goSetting = {
                        if (deviceType==DeviceType.TABLET){
                            showSetting=true
                        }else {
                            navController.navigate("settings")
                        }
                    })
                }

                if (deviceType==DeviceType.TABLET){
                   AnimatedVisibility(state) {
                       Box(modifier = Modifier.width(settingWidth)) {
                           SettingsScreen(navController, onBack = { showSetting = false })
                       }
                   }
}
            }

        }

        composable("settings") {
            SettingsScreen(navController, onBack = { navController.popBackStack() })
        }
    }
}