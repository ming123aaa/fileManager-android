package com.ohuang.filemanager.server.scan

import com.google.gson.Gson

import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

/**
 * 服务端设备发现响应器
 * 监听 UDP 广播请求，当收到客户端的 discover 请求时，回复设备信息
 */
class DeviceResponder(
    private val deviceInfo: DeviceInfo,
    private val port: Int = getDeviceScannerPort()
) {
    private val json = Gson()
    private var socket: DatagramSocket? = null
    private var running = false
    private var responderThread: Thread? = null

    fun start(onError: (Exception)-> Unit ={}) {
        if (running) return
        running = true
        responderThread = thread(name = "DeviceResponder", isDaemon = true) {
            try {
                socket = DatagramSocket(port)
                socket?.broadcast = true
                val buf = ByteArray(4096)
                while (running) {
                    try {
                        val requestPacket = DatagramPacket(buf, buf.size)
                        socket?.receive(requestPacket)
                        val requestJson = String(requestPacket.data, 0, requestPacket.length, Charsets.UTF_8)
                        if (requestJson.contains("\"discover\"")) {
                            val response = json.toJson( deviceInfo)
                            val responseBytes = response.toByteArray(Charsets.UTF_8)
                            val responsePacket = DatagramPacket(
                                responseBytes, responseBytes.size,
                                requestPacket.address, requestPacket.port
                            )
                            socket?.send(responsePacket)
                        }
                    } catch (e: Exception) {
                        if (!running) break
                    }
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {}
        responderThread = null
    }
}