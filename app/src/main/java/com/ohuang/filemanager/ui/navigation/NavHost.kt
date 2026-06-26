package com.ohuang.filemanager.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ohuang.filemanager.ui.screens.DownloadScreen
import com.ohuang.filemanager.ui.screens.FileManagerScreen
import com.ohuang.filemanager.ui.screens.SettingsScreen
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.FragmentBox
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.ui.utils.rememberSettingScreenWidth

@Composable
fun AppNavHost(onBack: () -> Unit = {}) {
    val navController = rememberNavController()
    val deviceType = rememberDeviceType()
    val settingWidth = rememberSettingScreenWidth()
    var showScreenName by remember { mutableStateOf("") }


    NavHost(
        navController = navController,
        startDestination = "fileManager"
    ) {
        composable("fileManager") {
       
            Row() {

                val state = remember() { SnapshotStateMap<String,MutableTransitionState<Boolean>>().apply {
                    put("settings",MutableTransitionState<Boolean>(false))
                    put("",MutableTransitionState<Boolean>(true))
                } }
                LaunchedEffect(showScreenName) {
                    state.forEach { entry ->
                        entry.value.targetState=
                            showScreenName==entry.key
                    }
                }

                FragmentBox(
                    modifier = Modifier.weight(1f),
                    isChange = if (deviceType == DeviceType.TABLET) {
                        state[""]!!.isIdle
                    } else {
                        true
                    }
                ) {
                    FileManagerScreen(navController, onBack = {
                        onBack()
                    }, goSetting = {
                        if (deviceType == DeviceType.TABLET) {
                            showScreenName = "settings"
                        } else {
                            navController.navigate("settings")
                        }
                    }, goDownload = {
                            navController.navigate("downloads")
                    })
                    BackHandler(showScreenName.isNotEmpty()) {
                        showScreenName = ""
                    }
                }

                if (deviceType == DeviceType.TABLET) {

                    AnimatedVisibility(state[""]!!) { }

                    AnimatedVisibility(state["settings"]!!) {
                        Box(modifier = Modifier.width(settingWidth)) {
                            SettingsScreen(navController, onBack = { showScreenName = "" })
                        }
                    }

                }
            }

        }

        composable("settings") {
            SettingsScreen(navController, onBack = { navController.navigateUp() })
        }

        composable("downloads") {
            DownloadScreen(navController, onBack = { navController.navigateUp() })
        }
    }
}