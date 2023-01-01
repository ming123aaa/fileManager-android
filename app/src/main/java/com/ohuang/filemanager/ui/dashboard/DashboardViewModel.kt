package com.ohuang.filemanager.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myhttp.CallBackString
import com.example.myhttp.Ihttp
import com.ohuang.filemanager.config.Http


class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>();
    val text: LiveData<String> = _text

    fun TestUrl(){
        Ihttp.getInstance().get(Http.Test.Connect(),object: CallBackString(){
            override fun success(ojb: String?) {
               _text.value= "测试:请求成功:$ojb"
            }

            override fun fail(s: String?) {
                _text.value= "测试:失败$s"
            }

        })
    }
}