package com.ohuang.filemanager.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ohuang.filemanager.server.util.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*

object VideoThumbnailCache {

    private const val DISK_CACHE_DIR = "video_thumbnails"
    private const val MAX_DISK_CACHE_SIZE = 1000 * 1024 * 1024 // 1000MB

    private val diskCacheDir: File by lazy {
        File(AppContext.instance.cacheDir, DISK_CACHE_DIR)
    }


    private val cache =
        Collections.synchronizedMap(object : LinkedHashMap<String, Bitmap>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>): Boolean {
                return size > 10
            }
        })


    fun get(url: String): Bitmap? = cache[url]

    fun putCache(url: String, bitmap: Bitmap) {
        cache[url] = bitmap
    }

    fun put(url: String, bitmap: Bitmap) {
        cache[url] = bitmap
        saveToDiskCache(url, bitmap)
    }

    fun getFromDiskCache(url: String): Bitmap? {
        val file = getCacheFile(url)

        return if (file.exists()) {
            if (System.currentTimeMillis() - file.lastModified() > 7 * 24 * 3600*1000) {
                file.delete()
                null
            } else {
                BitmapFactory.decodeFile(file.absolutePath)
            }
        } else {
            null
        }
    }

    private fun saveToDiskCache(url: String, bitmap: Bitmap) {
        val file = getCacheFile(url)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCacheFile(url: String): File {
        val hashedName = md5(url)
        return File(diskCacheDir, hashedName)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())

        // 将字节数组转换为 32位十六进制字符串
        val hexString = StringBuilder()
        for (b in digest) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()

    }

    fun clear() {
        cache.clear()
        diskCacheDir?.listFiles()?.forEach { it.delete() }
    }
}

@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Dp = 150.dp,
    placeholder: @Composable () -> Unit = { DefaultPlaceholder() }
) {
    val density = LocalDensity.current
    val targetSize = remember(thumbnailSize) {
        with(density) { thumbnailSize.toPx().toInt() }
    }

    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(videoUrl) {
        isLoading.value = true
        bitmapState.value = null

        job?.cancel()

        // 初始化磁盘缓存


        val cachedBitmap = VideoThumbnailCache.get(videoUrl)

        if (cachedBitmap != null) {
            bitmapState.value = cachedBitmap
            isLoading.value = false
            return@LaunchedEffect
        }





        job = scope.launch(Dispatchers.IO) {
            // 检查磁盘缓存
            delay(100)
            val diskCachedBitmap = withContext(Dispatchers.IO) {
                VideoThumbnailCache.getFromDiskCache(videoUrl)
            }
            if (diskCachedBitmap != null) {
                VideoThumbnailCache.putCache(url = videoUrl, diskCachedBitmap)
                bitmapState.value = diskCachedBitmap
                isLoading.value = false
                return@launch
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoUrl, emptyMap())
                val frame: Bitmap? = retriever.frameAtTime

                if (frame != null) {
                    val scaledBitmap = scaleBitmap(frame, targetSize)
                    VideoThumbnailCache.put(videoUrl, scaledBitmap)

                    with(Dispatchers.Main) {
                        bitmapState.value = scaledBitmap
                        isLoading.value = false
                    }
                } else {
                    with(Dispatchers.Main) {
                        isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                with(Dispatchers.Main) {
                    isLoading.value = false
                }
            } finally {
                retriever.release()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading.value) {
            placeholder()
        }

        bitmapState.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= targetSize && height <= targetSize) {
        return bitmap
    }

    val scale = targetSize.toFloat() / maxOf(width, height)
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

@Composable
fun DefaultPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 2.dp
        )
    }
}
