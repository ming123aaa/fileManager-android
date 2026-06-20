package com.ohuang.filemanager.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

// 1. 定义播放器池管理接口
interface PlayerPoolManager {
    fun acquirePlayer(): PlayerWrapper // 获取空闲播放器
    fun releasePlayer(player: PlayerWrapper)           // 释放播放器到池
    fun setMaxPoolSize(size: Int)                      // 设置池大小
    fun releaseAll()                                   // 释放所有资源
}

/**
 * ExoPlayer 包装器
 * @param player 原生的 ExoPlayer 实例
 */
class PlayerWrapper(val player: ExoPlayer) {

    private var mListener: Player.Listener? = null


    fun setListener(listener: Player.Listener): PlayerWrapper {
        removeListener()
        mListener = listener
        player.addListener(listener)
        return this
    }

    fun removeListener() {
        mListener?.let { player.removeListener(it) }
    }


}

/**
 * 重置播放器状态，以便复用
 * 在归还到播放器池时调用，清除当前播放的视频和监听器
 */
private fun PlayerWrapper.reset() {
    player.stop()
    player.clearMediaItems()
    player.playWhenReady = false
    // 移除所有监听器，防止下次复用时触发旧的业务逻辑
    removeListener()


}

/**
 * 彻底释放播放器资源
 * 当播放器池达到上限需要销毁，或应用退出时调用
 */
private fun PlayerWrapper.release() {
    player.release()

}

// 2. 核心实现类
class ExoPlayerPoolManager(val context: Context) : PlayerPoolManager {


    private val playerPool = ArrayDeque<PlayerWrapper>() // 空闲播放器队列
    private val usedPlayers = mutableListOf<PlayerWrapper>() // 正在使用的播放器
    private var maxPoolSize = 5 // 默认池大小

    override fun acquirePlayer(): PlayerWrapper {
        // 1. 优先从空闲池中获取
        val idlePlayer = playerPool.removeFirstOrNull()
        if (idlePlayer != null) {
            usedPlayers.add(idlePlayer)
            return idlePlayer
        }

        // 2. 空闲池为空，且未达上限，则创建新实例
        if (usedPlayers.size < maxPoolSize) {
            val newPlayer = createNewPlayer(context)
            usedPlayers.add(newPlayer)
            return newPlayer
        }

        // 3. 超过最大池大小，回收最旧的已暂停播放器（LRU策略）
        val oldestPlayer = usedPlayers.removeFirst()
        oldestPlayer.reset()
        usedPlayers.add(oldestPlayer)
        return oldestPlayer
    }


    override fun releasePlayer(player: PlayerWrapper) {
        usedPlayers.remove(player)
        player.reset()
        playerPool.addLast(player) // 归还到空闲池
    }

    override fun setMaxPoolSize(size: Int) {
        maxPoolSize = size
    }

    override fun releaseAll() {
        playerPool.forEach {
            it.release()
        }
        playerPool.clear()
        usedPlayers.forEach {
            it.release()
        }
        usedPlayers.clear()


    }

    @OptIn(UnstableApi::class)
    private fun createNewPlayer(context: Context): PlayerWrapper {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,  // minBufferMs: 最小缓冲时间（毫秒），保证流畅播放的底线
                15000, // maxBufferMs: 最大缓冲时间，限制内存占用的关键
                2500,  // bufferForPlaybackMs: 开始播放所需缓冲
                5000   // bufferForPlaybackAfterRebufferMs: 重新缓冲后开始播放所需缓冲
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // 不限制缓冲大小，按时间控制
            .setPrioritizeTimeOverSizeThresholds(true) // 优先按时间而非字节数控制缓冲
            .build()
        val exoPlayer = ExoPlayer.Builder(context).apply {
            setSeekBackIncrementMs(15_000)
            setSeekForwardIncrementMs(30_000)
            setLoadControl(loadControl)//一定要设置 否则会oom

        }.build()
        return PlayerWrapper(exoPlayer)
    }


}