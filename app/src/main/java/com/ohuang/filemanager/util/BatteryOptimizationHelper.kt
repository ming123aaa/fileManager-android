package com.ohuang.filemanager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast


object BatteryOptimizationHelper {

    /**
     * 检查是否已忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName())
        }
        // Android 6.0 以下没有电池优化概念，默认允许
        return true
    }

    /**
     * 方法1：直接请求忽略电池优化（会弹系统对话框）
     * 返回 true 表示成功发起请求，false 表示无需请求或失败
     */
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android 6.0 以下不支持，直接返回

            return false
        }

        // 如果已经忽略了，不需要再请求
        if (isIgnoringBatteryOptimizations(context)) {

            return false
        }

        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:" + context.getPackageName()))
            // 注意：这里要用 Activity 启动，不能直接用 context.startActivity
            // 建议在 Activity 中调用，或者用 FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "无法打开设置页面，请手动设置", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * 方法2：引导用户跳转到应用详情设置页（用户手动开启）
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.getPackageName(), null)
            intent.setData(uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果跳转失败，跳转到系统设置首页
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(context, "无法打开设置，请手动前往设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 智能组合方法：先请求忽略电池优化，如果用户拒绝或失败，弹窗引导去设置
     * 建议在 Activity 中调用此方法
     */
    fun checkAndRequest(context: Context,call: (Boolean) -> Unit) {
        // 1. 先检查是否已忽略
        if (isIgnoringBatteryOptimizations(context)) {
            call(true)
            return
        }

        // 2. 尝试请求忽略（会弹系统对话框）
        val requested = requestIgnoreBatteryOptimizations(context)
        call(requested)

        // 如果请求成功，用户会在系统弹窗中做选择：
        // - 点击"允许" → 忽略电池优化
        // - 点击"拒绝" → 我们需要在 onActivityResult 中检测并引导
    }

    /**
     * 在 Activity 的 onActivityResult 中调用此方法处理请求结果
     */
    fun handleRequestResult(context: Context?, requestCode: Int, resultCode: Int,call:(isIgnoring: Boolean)-> Unit) {
        if (requestCode == 10086) { // 自定义请求码
            val isIgnoring = isIgnoringBatteryOptimizations(context!!)
            call(isIgnoring)
        }
    }
}