package com.ohuang.filemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.ohuang.filemanager.ui.theme.FileManagerTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ohuang.filemanager.ui.components.VideoThumbnail
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import com.ohuang.filemanager.util.ClipboardUtils
import com.ohuang.filemanager.util.ExoPlayerPoolManager
import com.ohuang.filemanager.util.ImageGlide
import com.ohuang.filemanager.util.SPUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
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

    val videoInfoMap = SnapshotStateMap<String, Long>()

    val playerManager = ExoPlayerPoolManager(this)


    companion object {
        const val EXTRA_MEDIA_LIST = "extra_media_list"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"

        const val EXTRA_LIST_LOOP="extra_list_loop"

        fun start(context: Context, mediaList: List<MediaFileInfo>, currentIndex: Int,isLoop: Boolean=true) {
            val intent = Intent(context, MediaPreviewActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_LIST, Gson().toJson(mediaList))
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
                putExtra(EXTRA_LIST_LOOP, isLoop)
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
        val isLoop = intent.getBooleanExtra(EXTRA_LIST_LOOP, true)
        val type = object : TypeToken<List<MediaFileInfo>>() {}.type
        val mediaList: List<MediaFileInfo> =
            Gson().fromJson(mediaListJson, type) ?: emptyList()

        toggleFullscreen()
        enableEdgeToEdge()
        setContent {
            FileManagerTheme(darkTheme = true) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .safeDrawingPadding()
                ) {
                    MediaPreviewScreen(
                        mediaList = mediaList,
                        initialIndex = currentIndex,
                        onClose = { finish() },
                        isLoop=isLoop,
                        videoInfoMap = videoInfoMap,
                        playerManager = playerManager,
                        onToggleOrientation = { toggleOrientation() }
                    )
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.releaseAll()
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
            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    mediaList: List<MediaFileInfo>,
    initialIndex: Int,
    isLoop: Boolean,
    videoInfoMap: SnapshotStateMap<String, Long>,
    playerManager: ExoPlayerPoolManager,
    onClose: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    if (mediaList.isEmpty()) {
        return
    }

    val context = LocalContext.current
    val isAutoNextDefault = SPUtil.get(context, "media_preview_auto_next", false) as Boolean
    val isLockScrollDefault = SPUtil.get(context, "media_preview_lock_scroll", false) as Boolean
    var isAutoNext by rememberSaveable { mutableStateOf(isAutoNextDefault) }
    var isLockScroll by rememberSaveable { mutableStateOf(isLockScrollDefault) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { if (isLoop) 100000 else mediaList.size }
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val currentPageOffsetFraction by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }
    var selectPage by rememberSaveable { mutableStateOf(currentPage) }
    LaunchedEffect(currentPage, currentPageOffsetFraction) {
        if (currentPageOffsetFraction == 0f) {
            delay(200)//需要停留一段时间 才认为是选中状态
            selectPage = currentPage
        }
    }
    val isPageFlippingEnabled =pagerState.pageCount>1&&isLoop  //翻页是否可用
    var uiVisible by rememberSaveable { mutableStateOf(true) }

    var pagerScrollEnabled by rememberSaveable { mutableStateOf(true) }
    val rememberCoroutineScope = rememberCoroutineScope()



    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {

        val fling = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1), // 设置滑动贴靠距离

        )



        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                ,
            beyondBoundsPageCount = 1,
            flingBehavior = fling,
            key = { it },
            userScrollEnabled = pagerScrollEnabled&&!isLockScroll
        ) { page ->

            val mediaFile = mediaList[page % mediaList.size]
            val isVideo = isVideoFile(mediaFile.name)
            val isCurrentPage = page == selectPage
            val isPrePage = abs(page - selectPage) <= 1
            val isActivity =
                isCurrentPage && currentPage == page && abs(pagerState.currentPageOffsetFraction) < 0.25


            Box(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                if (isVideo) {

                    VideoPlayerPage(
                        playerManager = playerManager,
                        url = mediaFile.url,
                        name = mediaFile.name,
                        infoMap = videoInfoMap,
                        isActive = isActivity,
                        onControllerVisibilityChanged = { visible ->
                            uiVisible = visible
                        },
                        onSeekStart = { pagerScrollEnabled = false },
                        onSeekEnd = { pagerScrollEnabled = true },
                        isAutoNext = isAutoNext&&isPageFlippingEnabled,
                        isPre = isPrePage,
                        onNext = {
                            rememberCoroutineScope.launch {
                                if (currentPage+1<pagerState.pageCount) {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
                            }
                        }
                    )
                } else {
                    ZoomableImage(
                        isActive = isActivity,
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
                        }, isAutoNext = isAutoNext&&isPageFlippingEnabled,
                        onNext = {
                            rememberCoroutineScope.launch {
                                if (currentPage+1<pagerState.pageCount) {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
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
                        text = mediaFile.name ?: "",
                        color = Color.White,
                        maxLines = 1,
                        overflow= TextOverflow.Ellipsis
                        , modifier = Modifier.clickable{
                            ClipboardUtils.copyText(mediaFile.name,context)
                            Toast.makeText(context,"已复制",Toast.LENGTH_SHORT).show()
                        }
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

                        if (isPageFlippingEnabled) {
                            IconButton(onClick = {
                                isAutoNext = !isAutoNext
                                SPUtil.put(context, "media_preview_auto_next", isAutoNext)
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
                        if (isPageFlippingEnabled) {
                            IconButton(onClick = {
                                isLockScroll = !isLockScroll
                                SPUtil.put(context, "media_preview_lock_scroll", isLockScroll)
                            }) {
                                Icon(
                                    imageVector = if (isLockScroll) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "锁定",
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
                    text = "${currentPage % mediaList.size + 1} / ${mediaList.size}",
                    color = Color.White
                )

            }
        }
    }
}

@Composable
fun ZoomableImage(
    isActive : Boolean,
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

    LaunchedEffect(url, isAutoNext,isActive) {
        if (isAutoNext&&isActive) {
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
        ImageGlide(
            url = url,
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
    playerManager: ExoPlayerPoolManager,
    url: String,
    name: String,
    infoMap: SnapshotStateMap<String, Long>,
    isActive: Boolean,
    isPre: Boolean,
    isAutoNext: Boolean,
    onNext: () -> Unit,
    onControllerVisibilityChanged: (Boolean) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit
) {

    var isReady by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isReady || !isPre,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.zIndex(1f)
    ) {

        Box(modifier = Modifier.fillMaxSize()) {
            VideoThumbnail(
                modifier = Modifier.fillMaxSize(),
                videoUrl = url,
                contentScale = ContentScale.Fit
            )
        }

    }


    if (!isPre) {

        return
    }
    val context = LocalContext.current
    var isPlayingVideo by remember { mutableStateOf(false) }
    var showPlayController by remember { mutableStateOf(false) }
    var autoNextState by remember { mutableStateOf(isAutoNext) }
    var playbackPosition by remember { mutableLongStateOf(0L) }

    // 快进快退状态
    var showSeekIndicator by remember { mutableStateOf(false) }
    var seekDelta by remember { mutableLongStateOf(0L) }

    // 播放进度
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // 触摸检测状态
    var touchStartX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val rememberCoroutineScope = rememberCoroutineScope()

    // 创建播放器
    val player = remember {
        playerManager.acquirePlayer().apply {


            setListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            if (autoNextState) {
                                onNext()
                            }
                            this@apply.player.seekTo(0)
                            infoMap[url] = 0

                        }

                        Player.STATE_BUFFERING -> {

                        }

                        Player.STATE_IDLE -> {

                        }

                        Player.STATE_READY -> {
                            isReady = true
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
            player.player.repeatMode = REPEAT_MODE_OFF
        } else {
            player.player.repeatMode = REPEAT_MODE_ALL
        }
    }



    LaunchedEffect(Unit) {
        Log.d(
            "VideoPlayerPage",
            "prepare ->isActive=${isActive} name=$name itemSize=${player.player.mediaItemCount}"
        )
        if (player.player.mediaItemCount == 0) {
            player.player.setMediaItem(MediaItem.fromUri(url))
            player.player.prepare()
            player.player.seekTo(infoMap[url] ?: 0)
            player.player.playWhenReady = true
            Log.d("VideoPlayerPage", "setMediaItem-> name=$name")
        }
    }


    LaunchedEffect(isActive) {
        Log.d("VideoPlayerPage", "LaunchedEffect->isActive=${isActive} name=$name")
        if (isActive) {
            player.player.playWhenReady = true

        } else {
            player.player.pause()
            infoMap[url] = player.player.currentPosition

        }
    }

    // 更新播放进度
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                currentPosition = player.player.currentPosition
                duration = player.player.duration
                delay(300)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("VideoPlayerPage", "onDispose-> name=$name")
            try {
                infoMap[url] = player.player.currentPosition
                playerManager.releasePlayer(player)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    val rememberDeviceType = rememberDeviceType()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->

                val playView =
                    LayoutInflater.from(ctx).inflate(R.layout.oh_exo_player, null) as PlayerView
                playView.apply {
                    Log.d("VideoPlayerPage", "PlayerView -> name=$name")
                    this.player = player.player
                    useController = true
                    controllerAutoShow = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    controllerHideOnTouch = true
                    keepScreenOn = true

                    controllerShowTimeoutMs = 2000


                    // 控制器显示/隐藏回调
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        onControllerVisibilityChanged(visibility == View.VISIBLE)
                        showPlayController = visibility == View.VISIBLE
                    })
                    setShowNextButton(false)
                    setShowPreviousButton(false)





                    Log.d(
                        "Effect",
                        "PlayerView isActive=" + isActive + " url=" + URLDecoder.decode(url)
                    )
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
                                    player.player.seekTo(newPosition)
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
                            contentDescription = "后退",
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
