package com.ohuang.filemanager.server.scan

import com.google.gson.Gson
import com.ohuang.filemanager.server.util.AppContext
import com.ohuang.filemanager.util.SPUtil

import com.ohuang.kthttp.transform.gsonTransForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * 设备绑定管理器
 * 管理已绑定的设备列表，支持持久化存储
 */
object DeviceBindingManager {
    private val json = Gson()
    private val _boundDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val boundDevices: StateFlow<List<DeviceInfo>> = _boundDevices

    private val _scanDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val scanDevices: StateFlow<List<DeviceInfo>> = _scanDevices
    val scanner = DeviceScanner()
    val scope = CoroutineScope(Dispatchers.IO)


    private val _connectDeviceId = MutableStateFlow<String>("")
    val connectDeviceId: StateFlow<String> = _connectDeviceId


    fun getUrl(): String? {
        return getDevice(connectDeviceId.value)?.baseUrl
    }

    fun getDevice(): DeviceInfo? {
        return getDevice(connectDeviceId.value)
    }

    private fun saveConnectDeviceId(deviceId: String) {
        SPUtil.put(AppContext.instance, "connect_device_id", deviceId)
        _connectDeviceId.value = deviceId
    }

    private fun loadBoundDeviceId() {
        _connectDeviceId.value = SPUtil.get(AppContext.instance, "connect_device_id", "") as String
    }


    fun isUseDevice(): Boolean = connectDeviceId.value.isNotEmpty()

    fun init() {
        loadBoundDevices()
        loadBoundDeviceId()
    }


    suspend fun scanDevices(deviceId: String,timeOut: Long=5000):DeviceInfo?{
        return suspendCancellableCoroutine<DeviceInfo?> {  continuation ->
            scope.launch{
                val devices = scanner.scan(timeOut){ devices->
                    val dev=devices.find{ deviceId==it.deviceId}
                    if (dev!=null){
                        if (continuation.isActive){
                            continuation.resume(dev)
                        }
                    }
                    updateBindDevices(devices)

                }

                _scanDevices.value = devices.toMutableList()
                if (continuation.isActive) {
                    continuation.resume(null)
                }

            }

        }

    }
    suspend fun scanBindDevice(): DeviceInfo? {
        try {
            return scanDevices(connectDeviceId.value)
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun scan(timeOut: Long = 10000): List<DeviceInfo> {
        val devices = scanner.scan(timeOut){ devices->
            _scanDevices.value = devices.toMutableList()
            updateBindDevices(devices)
        }

        return devices
    }

    private fun updateBindDevices(devices: List<DeviceInfo>) {
        val current = _boundDevices.value.toMutableList()
        devices.forEach { device ->
            val existingIdx = current.indexOfFirst { it.deviceId == device.deviceId }
            if (existingIdx >= 0) {
                current[existingIdx] = device
            }
        }
        _boundDevices.value = current
        saveBoundDevices()
    }

    /** 绑定设备 */
    suspend fun bindDevice(device: DeviceInfo) {
        val current = _boundDevices.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.deviceId == device.deviceId }
        if (existingIdx >= 0) {
            current[existingIdx] = device
        } else {
            current.add(device)
        }
        _boundDevices.value = current
        saveBoundDevices()
        saveConnectDeviceId(device.deviceId)
        scanBindDevice()
    }

     fun unbindDevice() {
        saveConnectDeviceId("")
    }


    /** 删除设备 */
     fun deleteDevice(deviceId: String) {
        _boundDevices.value = _boundDevices.value.filter { it.deviceId != deviceId }
        saveBoundDevices()
    }

    /** 根据 deviceId 获取绑定的设备信息 */
    fun getDevice(deviceId: String): DeviceInfo? {
        return _boundDevices.value.find { it.deviceId == deviceId }
    }

    /** 获取设备的 URL */
    fun getDeviceUrl(deviceId: String): String? {
        return getDevice(deviceId)?.baseUrl
    }


    private fun saveBoundDevices() {
        val jsonStr = json.toJson(_boundDevices.value)
        SPUtil.put(AppContext.instance, "bound_devices", jsonStr)
    }

    private fun loadBoundDevices() {

        val jsonStr = SPUtil.get(AppContext.instance, "bound_devices", "") as String
        if (jsonStr.isNotEmpty()) {
            try {
                _boundDevices.value = gsonTransForm<List<DeviceInfo>>().transform(jsonStr)
            } catch (_: Exception) {
                _boundDevices.value = emptyList()
            }
        }
    }
}