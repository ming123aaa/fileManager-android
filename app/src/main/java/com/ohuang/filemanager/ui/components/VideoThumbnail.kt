package com.ohuang.filemanager.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

object VideoThumbnailCache {
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, Bitmap>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>): Boolean {
            return size > 50
        }
    })

    fun get(url: String): Bitmap? = cache[url]

    fun put(url: String, bitmap: Bitmap) {
        cache[url] = bitmap
    }

    fun clear() = cache.clear()
}

@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Dp = 200.dp,
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

        val cachedBitmap = VideoThumbnailCache.get(videoUrl)
        if (cachedBitmap != null) {
            bitmapState.value = cachedBitmap
            isLoading.value = false
            return@LaunchedEffect
        }

        job = scope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoUrl, emptyMap())
                val frame : Bitmap? = retriever.frameAtTime

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
