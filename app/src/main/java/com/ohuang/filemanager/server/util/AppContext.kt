package com.ohuang.filemanager.server.util

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import java.net.InetAddress

object AppContext {
    lateinit var instance: Application
        private set

    fun init(context: Application) {
        instance = context
    }

    fun getAndroidId(): String {
        try {

            val name = Settings.Secure.getString(instance.contentResolver, Settings.Secure.ANDROID_ID)
            if (!TextUtils.isEmpty(name)) {
                return name
            }
        } catch (e: Exception) {

        }

        return ""

    }

    fun getDeviceID(): String {
        return "android_" + getAndroidId()
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(): String {
        // 方法1：通过蓝牙适配器获取（最可靠）
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                val btName = bluetoothAdapter.name
                if (!btName.isNullOrBlank()) {
                    return btName
                }
            }
        } catch (e: Exception) {

            Log.d("DeviceName",""+e.message)
        }

        // 方法2：通过 Settings.System 获取（部分设备有效）
        try {
            val name = Settings.System.getString(instance.contentResolver, "device_name")
            if (!name.isNullOrBlank()) {
                return name
            }
        } catch (e: Exception) {

            Log.d("DeviceName",""+e.message)
        }

        // 方法3：通过 Settings.Global 获取（需要系统权限）
        try {
            val name = Settings.Global.getString(instance.contentResolver, Settings.Global.DEVICE_NAME)
            if (!name.isNullOrBlank()) {
                return name
            }
        } catch (e: Exception) {

            Log.d("DeviceName",""+e.message)
        }

        // 方法4：获取设备的主机名
        try {
            val propName = getHostName()
            if (!propName.isNullOrBlank()) {
                return propName
            }
        } catch (e: Exception) {

            Log.d("DeviceName",""+e.message)
        }

        // 方法5：通过系统属性获取（部分设备有效）
        try {
            val propName = getSystemProperty("ro.product.device")
            if (!propName.isNullOrBlank()) {
                return propName
            }
        } catch (e: Exception) {

            Log.d("DeviceName",""+e.message)
        }







        // 最终降级
        return Build.MODEL
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取设备的主机名
     */
    private fun getHostName(): String? {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            // 在某些设备或网络环境下可能抛出异常
            null
        }
    }
}