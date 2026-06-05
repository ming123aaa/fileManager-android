package com.ohuang.filemanager;

import android.app.Application;
import com.ohuang.filemanager.config.HttpConfig;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HttpConfig.INSTANCE.loadBaseUrl(this);
    }
}