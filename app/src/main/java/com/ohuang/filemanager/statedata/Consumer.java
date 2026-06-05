package com.ohuang.filemanager.statedata;

public interface Consumer<T> {

    boolean accept(T t);
}
