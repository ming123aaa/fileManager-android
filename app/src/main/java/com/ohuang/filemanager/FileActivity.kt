package com.ohuang.filemanager

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter

import com.ohuang.filemanager.databinding.ActivityFileBinding
import com.ohuang.filemanager.service.UploadService
import com.ohuang.filemanager.ui.dashboard.DashboardFragment
import com.ohuang.filemanager.ui.home.HomeFragment

class FileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileBinding
    var homeFragment=HomeFragment()
    var dashboardFragment=DashboardFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.vpFile.adapter=object :FragmentPagerAdapter(supportFragmentManager){
            override fun getCount(): Int {
                return 2
            }

            override fun getItem(position: Int): Fragment {
                var fg:Fragment?=null
                when(position){
                    0->fg=homeFragment
                    1->fg=dashboardFragment
                }
                return fg!!
            }
        }
        val intent2 = Intent(this, UploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent2)
        }
        bindService(intent2, connection, BIND_AUTO_CREATE)
    }



    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {

        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }
    var lastTime: Long = 0
    override fun onBackPressed() {
        if (binding.vpFile.currentItem==0){
            if (homeFragment.goBack()){
                if (SystemClock.uptimeMillis()-lastTime<3000) {
                    super.onBackPressed()
                }
                lastTime=SystemClock.uptimeMillis()
                Toast.makeText(this,"再按一下退出",Toast.LENGTH_LONG).show()

            }
        }else {
            if (SystemClock.uptimeMillis()-lastTime<3000) {
                super.onBackPressed()
            }
            lastTime=SystemClock.uptimeMillis()
            Toast.makeText(this,"再按一下退出",Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}