package com.ohuang.filemanager.statedata;

public interface MapConsumer<T, U> {

    boolean accept(T t, U u);
}
