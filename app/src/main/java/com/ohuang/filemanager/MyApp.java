package com.ohuang.filemanager;

import android.app.Application;
import com.ohuang.filemanager.config.HttpConfig;
import com.ohuang.filemanager.server.util.AppContext;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HttpConfig.INSTANCE.loadBaseUrl(this);
        AppContext.INSTANCE.init(this);
        AndServerManager.INSTANCE.autoStart(this);
    }
}