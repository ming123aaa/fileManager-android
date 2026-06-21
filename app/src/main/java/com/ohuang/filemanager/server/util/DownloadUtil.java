package com.ohuang.filemanager.server.util;


import android.os.Build;

import com.ohuang.filemanager.server.adapter.RandomAccessFileBody;
import com.yanzhenjie.andserver.framework.body.FileBody;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import kotlin.text.Charsets;

public class DownloadUtil {

    // 特定 Content-Type 映射
    static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<String, String>() {{
        put("html", "text/html");
        put("htm", "text/html");
        put("json", "application/json");
        put("xml", "application/xml");
        put("css", "text/css");
        put("js", "application/javascript");
        put("svg", "image/svg+xml");
    }};

    static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "md", "txt", "log", "csv", "yaml", "yml", "ini", "cfg", "conf", "properties",
            "gitignore", "ets", "kt", "java", "py", "ts", "php",
            "c", "cpp", "h", "go", "rs", "rb", "sh", "bat", "sql", "vue", "jsx", "tsx"
    ));
    static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "webp"
    ));

    static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "mpeg", "mpg", "ogg", "webm"
    ));
    static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "mp2", "mpa"
    ));

    static final Set<String> PDF_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf"
    ));


    /**
     * 服务器端-文件下载 支持断点下载
     *
     * @param request
     * @param response
     * @param file
     * @param path
     * @return
     */
    public static ResponseBody download(HttpRequest request, HttpResponse response, File file, String path) {
        //文件目录

        String fileName = file.getName();

        if (!file.exists() || file.isDirectory()) {
            try {
                if (file.exists()) {
                    response.setStatus(302);
                    response.addHeader("Location", "/?path=" + URLEncoder.encode(path, "utf-8").replaceAll("\\+", "%20"));
                    StringBody stringBody = new StringBody("这是一个文件夹" + path);
                    response.setBody(stringBody);
                    return stringBody;
                } else {
                    response.setStatus(404);
                    StringBody stringBody = new StringBody("找不到文件" + path);
                    response.setBody(stringBody);
                    return stringBody;
                }

            } catch (Exception e) {

            }

        }

        //下载开始位置
        long startByte = 0;
        //下载结束位置
        long endByte = file.length() - 1;

        //获取下载范围
        String range = request.getHeader("range");
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=") + 1).trim();
            String rangeArray[] = range.split("-");
            if (rangeArray.length == 1) {
                //Example: bytes=1024-
                if (range.endsWith("-")) {
                    startByte = Long.parseLong(rangeArray[0]);
                } else { //Example: bytes=-1024
                    endByte = Long.parseLong(rangeArray[0]);
                }
            }
            //Example: bytes=2048-4096
            else if (rangeArray.length == 2) {
                startByte = Long.parseLong(rangeArray[0]);
                endByte = Long.parseLong(rangeArray[1]);
            }
        }

        long contentLength = endByte - startByte + 1;

        String ext = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = fileName.substring(dotIndex + 1).toLowerCase();
        }
        String contentType = "";

        // 优先使用特定 Content-Type 映射
        if (CONTENT_TYPE_MAP.containsKey(ext)) {
            contentType = CONTENT_TYPE_MAP.get(ext);
        } else if (TEXT_EXTENSIONS.contains(ext)) {
            contentType = "text/plain";
        } else if (IMAGE_EXTENSIONS.contains(ext)) {
            contentType = "image/" + ext;
        } else if (VIDEO_EXTENSIONS.contains(ext)) {
            contentType = "video/" + ext;
        } else if (AUDIO_EXTENSIONS.contains(ext)) {
            contentType = "audio/" + ext;
        } else if (PDF_EXTENSIONS.contains(ext)) {
            contentType = "application/pdf";
        }else {
            contentType="application/octet-stream";
        }


        //HTTP 响应头设置
        //断点续传，HTTP 状态码必须为 206，否则不设置，如果非断点续传设置 206 状态码，则浏览器无法下载

        if (contentType.startsWith("text/")) {
            contentType += ";charset=UTF-8";
        }


        response.setHeader("Content-Type", contentType);

        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Access-Control-Allow-Origin", "*");
        //Content-Range: 下载开始位置-下载结束位置/文件大小
        response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());
        //Content-disposition: inline; filename=xxx.xxx 表示浏览器内嵌显示该文件
        //Content-disposition: attachment; filename=xxx.xxx 表示浏览器下载该文件

        String encodeFilename =file.getName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            encodeFilename=URLEncoder.encode(file.getName(),Charsets.UTF_8);
        }else {
            encodeFilename = URLEncoder.encode(file.getName());
        }
        response.setHeader("Content-Disposition", "inline;filename=" + encodeFilename);

        if (range != null) {
            response.setStatus(206);
        } else {
            response.setStatus(200);
        }

        RandomAccessFileBody streamBody = new RandomAccessFileBody(file, contentLength, startByte);
        response.setBody(streamBody);


        return streamBody;

    }
}


