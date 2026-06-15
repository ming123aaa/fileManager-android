package com.ohuang.filemanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServiceLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column() {


                        Button({
                            if (SpringServiceManager.isRunning) {
                                SpringServiceManager.stop()
                            } else {
                                SpringServiceManager.run(this@ServiceLauncherActivity)
                            }

                        }) {
                            Text(if (SpringServiceManager.isRunning) "停止" else "启动")
                        }
                        LazyColumn {
                            items(SpringServiceManager.msgList) {
                                val textColor = when (it.type) {
                                    SpringServiceType.THROW -> MaterialTheme.colorScheme.error
                                    SpringServiceType.ERROR -> MaterialTheme.colorScheme.error
                                    SpringServiceType.STOP -> MaterialTheme.colorScheme.onError
                                    else -> MaterialTheme.colorScheme.primary

                                }
                                SelectionContainer {
                                    Text(it.msg, color = textColor)
                                }


                            }

                        }
                    }

                }
            }
        }
    }
}

enum class SpringServiceType {
    MSG, ERROR, THROW, STOP
}

data class SpringServiceMsg(
    val type: SpringServiceType = SpringServiceType.MSG, val msg: String,
    val time: Long = System.currentTimeMillis()
)


object SpringServiceManager {

    val msgList = SnapshotStateList<SpringServiceMsg>()

    var isRunning by mutableStateOf(false)
        private set


    private var shouldStop = false

    private var mThread: Thread? = null

    fun stop() {
        shouldStop = true
        mThread?.stop()
    }

    fun run(context: Context, dir: String = context.filesDir.absolutePath, port: String = "8080") {
        if (isRunning) {
            return
        }
        isRunning = true
        shouldStop = false
        msgList.clear()
        GlobalScope.launch(Dispatchers.IO) {

            try {
                msgList.add(SpringServiceMsg(msg = "检查环境..."))
                // 这里执行启动服务的代码



            } catch (e: Throwable) {
                msgList.add(
                    SpringServiceMsg(
                        type = SpringServiceType.THROW,
                        msg = "${e.message} \n ${e.stackTraceToString()}"
                    )
                )
            }
            msgList.add(SpringServiceMsg(type = SpringServiceType.STOP, msg = "服务停止"))
            mThread = null
            isRunning = false

        }

    }






}