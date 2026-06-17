package com.ohuang.filemanager.server.controller


import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@RestController
@RequestMapping("/test")
class TestApiController {

    @GetMapping("/connect")
    fun canConnect(): String {
        return "成功"
    }

    @GetMapping("/version")
    fun version(request: HttpRequest,response: HttpResponse){

        response.setBody(StringBody("1.0"))
    }



    @GetMapping("/agent")
    fun agent(@RequestParam("url") u: String): String {
        return try {
            val url = URL(u)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000
            urlConnection.requestMethod = "GET"
            urlConnection.connect()
            
            val code = urlConnection.responseCode
            if (code == 200) {
                val inputStream = urlConnection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val buffer = StringBuffer()
                var readLine: String?
                while (reader.readLine().also { readLine = it } != null) {
                    buffer.append(readLine)
                }
                buffer.toString()
            } else {
                "错误: HTTP $code"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "错误"
        }
    }
}