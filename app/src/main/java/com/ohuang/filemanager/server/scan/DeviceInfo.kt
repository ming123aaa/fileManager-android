package com.ohuang.filemanager.server.scan


data class DeviceInfo(
    val deviceId: String= "",
    val deviceName: String= "",
    val host: String="",
    val port: Int=0,
    val useHttps: Boolean = false,
    val version: String = "1.0"
) {
    val baseUrl: String
        get() = "${if (useHttps) "https" else "http"}://$host:$port"

    val displayLabel: String
        get() = "$deviceName ($host:$port)"
}

