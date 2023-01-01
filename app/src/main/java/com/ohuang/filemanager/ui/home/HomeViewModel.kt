package com.ohuang.filemanager.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myhttp.CallBackObjects
import com.example.myhttp.Ihttp
import com.kennyc.view.MultiStateView
import com.ohuang.filemanager.bean.FileBean
import com.ohuang.filemanager.config.Http

class HomeViewModel : ViewModel() {
    private var _data=MutableLiveData<List<FileBean?>>()
    private var _toastData=MutableLiveData<String>()
    private var _State=MutableLiveData<MultiStateView.ViewState>()

    val data: LiveData<List<FileBean?>> = _data
    val toastData: LiveData<String> = _toastData
    val state:LiveData<MultiStateView.ViewState> =_State
    var refreshSate= MutableLiveData<Boolean>()
    var filePath=""

    fun loadData(path:String="",isRefersh:Boolean=false){
        filePath=path
        var hashMap = HashMap<String,String>()
        hashMap["path"] = path
        Ihttp.getInstance().post(Http.Main.GetAllFile(),hashMap, FileBean::class.java, object :
            CallBackObjects<FileBean?>() {
            override fun fail(s: String) {
                _toastData.value=s
                _State.value=MultiStateView.ViewState.ERROR
                refreshSate.value=false
            }
            override fun success(ojb: List<FileBean?>) {

                if (ojb.isNotEmpty()){
                    _State.value=MultiStateView.ViewState.CONTENT
                }else{
                    _State.value=MultiStateView.ViewState.EMPTY
                }
                if (isRefersh) {
                    refreshSate.value = false
                }
                _data.value=ojb
            }
        })
    }
}