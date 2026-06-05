package com.ohuang.filemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ohuang.filemanager.ui.screens.FileManagerScreen
import com.ohuang.filemanager.ui.screens.SettingsScreen

@Composable
fun AppNavHost(onRootDirectoryChanged: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "filemanager"
    ) {
        composable("filemanager") {
            FileManagerScreen(navController, onRootDirectoryChanged)
        }
        
        composable("settings") {
            SettingsScreen(navController)
        }
    }
}