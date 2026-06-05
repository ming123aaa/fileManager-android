package com.ohuang.filemanager.statedata;

public interface Observer<T> {

    void onchange(T t);
}
