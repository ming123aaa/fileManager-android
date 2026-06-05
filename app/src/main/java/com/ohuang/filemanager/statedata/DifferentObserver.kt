package com.ohuang.filemanager.statedata

class DifferentObserver<T> (private val observer:Observer<T>):Observer<T> {
    private var  data:T?=null
    override fun onchange(t: T) {
       if (t!=data){
           data=t
           observer.onchange(t)
       }
    }
}