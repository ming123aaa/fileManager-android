package com.ohuang.filemanager

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.ohuang.filemanager.databinding.ActivityMainBinding
import com.ohuang.filemanager.service.UploadService
import okhttp3.*


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val READ_REQUEST_CODE = 1
    private var textContent = ""
    private lateinit var fileUri: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            startActivityForResult(
                intent,
                READ_REQUEST_CODE
            )
        }


        binding.btnUpload.setOnClickListener {

            if (!::fileUri.isInitialized) {
                Toast.makeText(this, "还没有选择文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(binder==null){
                Toast.makeText(this, "上传服务启动失败", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binder?.let {
                if (!it.isUpload()) {
                    it.startUpLoad(fileUri)
                } else {
                    Toast.makeText(this, "文件正在上传中...", Toast.LENGTH_SHORT).show()
                }
            }

        }

        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            binding.tvUri.text = uri.toString()
            if (uri != null) {
                fileUri = uri
            }
        }
        val intent2 = Intent(this, UploadService::class.java)
        bindService(intent2, connection, BIND_AUTO_CREATE)
    }

    private var binder: UploadService.DownUpBinder? = null
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as UploadService.DownUpBinder
            binder?.getLivedata()?.observe(this@MainActivity, {
                binding.tvProcess.text = it
            })
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE) {
            try {
                textContent = data!!.dataString.toString()
                fileUri = data.data!!

                binding.tvUri.text = "uri: $textContent"
            } catch (e: NullPointerException) {
                Toast.makeText(this, "用户没有选择文件", Toast.LENGTH_LONG).show()
                return
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}





