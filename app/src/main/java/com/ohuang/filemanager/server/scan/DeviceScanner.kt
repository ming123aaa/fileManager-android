package com.ohuang.filemanager.server.scan


import com.ohuang.kthttp.transform.gsonTransForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

fun getDeviceScannerPort(): Int {
    return 24680
}

class DeviceScanner {

    private val discoveryPort = getDeviceScannerPort()
    private var cancelled = false
    private var isScanning=false

    fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        NetworkInterface.getNetworkInterfaces().toList().forEach { ni ->
            if (ni.isUp && !ni.isLoopback) {
                ni.interfaceAddresses.forEach { ia ->
                    ia.broadcast?.let { addresses.add(it) }
                }
            }
        }
        if (addresses.isEmpty()) {
            addresses.add(InetAddress.getByName("255.255.255.255"))
        }
        return addresses
    }

    suspend fun scan(timeoutMs: Long,onUpdate:(List<DeviceInfo>)->Unit={}): List<DeviceInfo> = withContext(Dispatchers.IO) {
        cancelled = false
        isScanning=true
        val results = mutableListOf<DeviceInfo>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = timeoutMs.toInt()

            val request = """{"type":"discover","version":"1.0"}"""
            val requestBytes = request.toByteArray(Charsets.UTF_8)
            getBroadcastAddresses().forEach { broadcastAddr ->
                val requestPacket = DatagramPacket(requestBytes, requestBytes.size, broadcastAddr, discoveryPort)
                socket.send(requestPacket)
            }


            val buf = ByteArray(4096)
            while (!cancelled) {
                try {
                    val responsePacket = DatagramPacket(buf, buf.size)
                    socket.receive(responsePacket)
                    val responseJson = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                    val device = gsonTransForm<DeviceInfo>().transform(responseJson).let {
                        if (!responsePacket.address.hostAddress.isNullOrBlank()){
                            it.copy(host = responsePacket.address.hostAddress!!)
                        }else it
                    }

                    if (results.none { it.deviceId == device.deviceId }) {
                        results.add(device)
                        onUpdate(results)
                    }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
            isScanning=false
        }
        results
    }

    fun cancel() {
        cancelled = true
    }
}