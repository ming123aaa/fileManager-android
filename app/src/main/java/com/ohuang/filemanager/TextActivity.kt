package com.ohuang.filemanager

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ohuang.filemanager.config.Http
import com.ohuang.filemanager.databinding.ActivityTextBinding
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toObservable

class TextActivity : AppCompatActivity() {
    lateinit var binding:ActivityTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readText()

    }

    fun upLoadText(view: View) {
        writeText()
    }


    private fun readText(){


           RxHttp.get(Http.Main.ReadText())

               .toObservable<String>()
               .subscribe(  {

                   binding.item=it
                   binding.btnUpload.isEnabled=true
                   }
               ,  {
                Toast.makeText(this@TextActivity,"数据加载失败!",Toast.LENGTH_SHORT).show()
               })

    }

    private fun writeText(){
        RxHttp.postForm(Http.Main.WriteText())
            .add("txt",binding.item)
            .toObservable<String>()
            .subscribe(  {

                runOnUiThread {
                    if (it.equals("成功")) {
                        Toast.makeText(this, "编辑成功", Toast.LENGTH_LONG).show()
                    }else{
                        Toast.makeText(this, "服务端编辑失败", Toast.LENGTH_LONG).show()
                    }
                }

            }
                ,  {
                    runOnUiThread {
                        Toast.makeText(this@TextActivity,"网络失败!"+it,Toast.LENGTH_LONG).show()
                    }
                })
    }
}