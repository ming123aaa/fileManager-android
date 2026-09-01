package com.ohuang.filemanager;

import android.app.Application;
import com.ohuang.filemanager.config.HttpConfig;
import com.ohuang.filemanager.server.util.AppContext;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.INSTANCE.init(this);
        HttpConfig.INSTANCE.loadBaseUrl(this);
        AndServerManager.INSTANCE.autoStart(this);
    }
}