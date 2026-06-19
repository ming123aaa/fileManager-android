package com.ohuang.filemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

/**
 * 媒体文件信息
 */
data class MediaFileInfo(
    val url: String,
    val name: String
)

class MediaPreviewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MEDIA_LIST = "extra_media_list"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"

        fun start(context: Context, mediaList: List<MediaFileInfo>, currentIndex: Int) {
            val intent = Intent(context, MediaPreviewActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_LIST, Gson().toJson(mediaList))
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            }
            context.startActivity(intent)
        }
    }

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mediaListJson = intent.getStringExtra(EXTRA_MEDIA_LIST) ?: "[]"
        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        val type = object : TypeToken<List<MediaFileInfo>>() {}.type
        val mediaList: List<MediaFileInfo> =
            Gson().fromJson(mediaListJson, type) ?: emptyList()

        toggleFullscreen()
        enableEdgeToEdge()
        setContent {

            MaterialTheme(colorScheme = darkColorScheme()) {

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .safeDrawingPadding()
                    ) {
                    MediaPreviewScreen(
                        mediaList = mediaList,
                        initialIndex = currentIndex,
                        onClose = { finish() },
                        onToggleOrientation = { toggleOrientation() }
                    )
                }

            }
        }
    }

    private fun toggleFullscreen() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun toggleOrientation() {
        requestedOrientation =
            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
    }
}

private var autoNext=false

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    mediaList: List<MediaFileInfo>,
    initialIndex: Int,
    onClose: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    if (mediaList.isEmpty()) {
        return
    }
    val videoInfoMap = remember { SnapshotStateMap<String, Long>() }
    val context = LocalContext.current
    var isAutoNext by remember { mutableStateOf(autoNext) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { 100000 }
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    var uiVisible by remember { mutableStateOf(true) }
    var pagerScrollEnabled by remember { mutableStateOf(true) }
    val rememberCoroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerScrollEnabled
        ) { page ->
            val mediaFile = mediaList[page % mediaList.size]
            val isVideo = isVideoFile(mediaFile.name)
            val isCurrentPage = page == currentPage

            Box(modifier = Modifier.fillMaxSize()) {
                if (isVideo) {
                    VideoPlayerPage(
                        url = mediaFile.url,
                        infoMap = videoInfoMap,
                        isActive = isCurrentPage,
                        onControllerVisibilityChanged = { visible ->
                            uiVisible = visible
                        },
                        onSeekStart = { pagerScrollEnabled = false },
                        onSeekEnd = { pagerScrollEnabled = true },
                        isAutoNext = isAutoNext,
                        onNext = {
                            rememberCoroutineScope.launch {
                                pagerState.scrollToPage(currentPage + 1)
                            }

                        }
                    )
                } else {
                    ZoomableImage(
                        url = mediaFile.url,
                        onTap = {
                            if (pagerScrollEnabled) {
                                uiVisible = !uiVisible
                            }
                        },
                        onScaleChanged = { scale ->
                            pagerScrollEnabled = scale <= 1f
                            if (!pagerScrollEnabled) {
                                uiVisible = pagerScrollEnabled
                            }
                        }, isAutoNext = isAutoNext,
                        onNext = {
                            rememberCoroutineScope.launch {
                                pagerState.scrollToPage(currentPage + 1)
                            }

                        }
                    )
                }
            }
        }

        val mediaFile = mediaList[currentPage % mediaList.size]
        val isVideo = isVideoFile(mediaFile.name)


        // 顶部工具栏
        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = mediaList.getOrNull(currentPage)?.name ?: "",
                        color = Color.White,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Row {
                        if (mediaList.size > 10) {
                            IconButton(onClick = {
                                isAutoNext = !isAutoNext
                                autoNext=isAutoNext
                                Toast.makeText(
                                    context, if (isAutoNext) "自动翻页" else "循环播放",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }) {
                                Icon(
                                    imageVector = if (isAutoNext) Icons.Default.FastForward else Icons.Default.Repeat,
                                    contentDescription = "自动翻页/循环播放",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onToggleOrientation) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "旋转屏幕",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            )
        }

        // 底部页码
        AnimatedVisibility(
            visible = uiVisible && !isVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {

            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                Text(
                    text = "${currentPage + 1} / ${mediaList.size}",
                    color = Color.White
                )

            }
        }
    }
}

@Composable
fun ZoomableImage(
    url: String,
    onTap: () -> Unit,
    onScaleChanged: (Float) -> Unit,
    isAutoNext: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(url, isAutoNext) {
        if (isAutoNext) {
            delay(15_000)
            onNext()
        }

    }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {

                awaitTransformGestures({ scale >= 1.01 }) { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 3f)
                    val maxX = (size.width * (newScale - 1)) / 2
                    val maxY = (size.height * (newScale - 1)) / 2
                    val newOffsetX = (offset.x + pan.x * newScale).coerceIn(-maxX, maxX)
                    val newOffsetY = (offset.y + pan.y * newScale).coerceIn(-maxY, maxY)
                    scale = newScale
                    offset = Offset(newOffsetX, newOffsetY)
                }

            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                    },
                    onTap = { onTap() }
                )
            }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}


suspend fun PointerInputScope.awaitTransformGestures(
    onConsume: () -> Boolean = { true },
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged() && onConsume()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerPage(
    url: String,
    infoMap: SnapshotStateMap<String, Long>,
    isActive: Boolean,
    isAutoNext: Boolean,
    onNext: () -> Unit,
    onControllerVisibilityChanged: (Boolean) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit
) {
    val context = LocalContext.current
    var isPlayingVideo by remember { mutableStateOf(false) }
    var showPlayController by remember { mutableStateOf(false) }
    var autoNextState by remember { mutableStateOf(isAutoNext) }
    var playbackPosition by remember { mutableLongStateOf(0L) }

    // 快进快退状态
    var showSeekIndicator by remember { mutableStateOf(false) }
    var seekDelta by remember { mutableLongStateOf(0L) }

    // 播放进度
    var currentPosition by remember(url) { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // 触摸检测状态
    var touchStartX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // 创建播放器
    val player = remember {
        ExoPlayer.Builder(context).apply {
            setSeekBackIncrementMs(15_000)
            setSeekForwardIncrementMs(30_000)


        }.build().apply {

            addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            if (autoNextState) {
                                onNext()
                                currentPosition = 0
                            }
                        }

                        Player.STATE_BUFFERING -> {

                        }

                        Player.STATE_IDLE -> {

                        }

                        Player.STATE_READY -> {

                        }
                    }
                }


                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    isPlayingVideo = isPlaying
                }
            })
        }
    }

    LaunchedEffect(isAutoNext) {
        autoNextState = isAutoNext
        if (isAutoNext) {
            player.repeatMode = REPEAT_MODE_OFF
        } else {
            player.repeatMode = REPEAT_MODE_ALL
        }
    }




    LaunchedEffect(isActive, url) {
        if (isActive) {

            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.seekTo(infoMap[url] ?: 0)
            player.playWhenReady = true
        } else {
            player.pause()
        }
    }

    // 更新播放进度
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                currentPosition = player.currentPosition
                infoMap[url] = player.currentPosition
                duration = player.duration
                kotlinx.coroutines.delay(300)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    controllerHideOnTouch = true

                    controllerShowTimeoutMs = 2000


                    // 控制器显示/隐藏回调
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        onControllerVisibilityChanged(visibility == View.VISIBLE)
                        showPlayController = visibility == View.VISIBLE
                    })



                    setShowNextButton(false)
                    setShowPreviousButton(false)


                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPlayingVideo) {
                    if (isPlayingVideo) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                touchStartX = offset.x
                                isDragging = true

                            },
                            onDragEnd = {
                                if (isDragging && kotlin.math.abs(seekDelta) > 500) {
                                    val newPosition = (currentPosition + seekDelta).coerceAtLeast(0)
                                    player.seekTo(newPosition)
                                }
                                isDragging = false
                                showSeekIndicator = false
                                seekDelta = 0L
                                onSeekEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                showSeekIndicator = false
                                seekDelta = 0L
                                onSeekEnd()

                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (isDragging) {
                                    val totalDrag = change.position.x - touchStartX
                                    // 每15像素约等于1秒
                                    val deltaMs = (totalDrag / 15f * 1000).toLong()
                                    seekDelta = deltaMs
                                    showSeekIndicator = true
                                    onSeekStart()
                                    change.consume()
                                }
                            }
                        )
                    }
                }
        )


        // 快进快退指示器
        AnimatedVisibility(
            visible = showSeekIndicator && isActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        16.dp
                    )
                ) {
                    if (seekDelta < 0) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "快退",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    if (seekDelta > 0) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "快进",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatSeekTime(seekDelta),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                val previewPosition = (currentPosition + seekDelta).coerceAtLeast(0)
                Text(
                    text = "${formatDuration(previewPosition)} / ${formatDuration(duration)}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatSeekTime(deltaMs: Long): String {
    val absMs = kotlin.math.abs(deltaMs)
    val sign = if (deltaMs >= 0) "+" else "-"
    val totalSeconds = absMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "$sign${minutes}m${seconds}s"
    } else {
        "$sign${seconds}s"
    }
}

fun isVideoFile(name: String): Boolean {
    val ext = name.split(".").lastOrNull()?.lowercase() ?: return false
    return listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp").contains(ext)
}
