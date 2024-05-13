package com.ohuang.filemanager;

import android.app.Application;

import com.example.myhttp.GsonInterface;
import com.example.myhttp.Ihttp;
import com.example.myhttp.OkHttpInterface;
import com.ohuang.filemanager.config.Http;
import com.ohuang.filemanager.config.SpConfig;
import com.ohuang.filemanager.util.CrashHandler;
import com.ohuang.filemanager.util.SPUtil;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Ihttp.getInstance().setHttpInterface(OkHttpInterface.getInstance());
        Ihttp.getInstance().setJsonInterFace(GsonInterface.getInstance());
        Http.setBaseUrl(SpConfig.INSTANCE.getUrl(this));

//        CrashHandler crashHandler = CrashHandler.getInstance(); //获取异常处理实例
//        crashHandler.init(getApplicationContext());
    }
}
