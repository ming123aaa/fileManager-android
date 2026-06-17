package com.ohuang.filemanager;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class WebActivity extends Activity {
    public static void start(Context context, String url) {
        Intent starter = new Intent(context, WebActivity.class);
        starter.putExtra("url", url);
        context.startActivity(starter);
    }

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout frameLayout;
    private View mCustomView;
    WebChromeClient.CustomViewCallback mCustomViewCallback;
    public String url;
    private LinearLayout toolbar;
    private ImageView ivBack;
    private TextView tvTitle;
    private View layoutError;
    private TextView tvErrorMsg;
    private Button btnRetry;
    private boolean isPageError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        url = getIntent().getStringExtra("url");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initViews();
        initToolbar();
        initWebView();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        progressBar = findViewById(R.id.progress_bar);
        webView = findViewById(R.id.wv_webview);
        frameLayout = findViewById(R.id.fl_web);
        layoutError = findViewById(R.id.layout_error);
        tvErrorMsg = findViewById(R.id.tv_error_msg);
        btnRetry = findViewById(R.id.btn_retry);

        tvTitle.setText("加载中...");

        btnRetry.setOnClickListener(v -> {
            layoutError.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            isPageError = false;
            webView.loadUrl(url);
        });
    }

    private void initToolbar() {
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        WebViewSetting.INSTANCE.setting(webView, this);
        // --- 核心基础配置 ---
        settings.setJavaScriptEnabled(true);          // 启用 JS (现代网页必备)
        settings.setDomStorageEnabled(true);          // 开启 DOM Storage (H5本地存储)
        settings.setDefaultTextEncodingName("utf-8"); // 默认 UTF-8 编码防乱码

        // --- 屏幕适配与缩放 ---
        settings.setUseWideViewPort(true);            // 支持 viewport meta 标签
        settings.setLoadWithOverviewMode(true);       // 将页面缩放到适合 WebView 宽度
        settings.setSupportZoom(true);                // 允许缩放
        settings.setBuiltInZoomControls(true);        // 显示内置缩放控件
        settings.setDisplayZoomControls(false);       // 隐藏原生缩放按钮 (UI更美观)

        // --- 缓存与性能优化 ---
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // 优先使用缓存
        settings.setLoadsImagesAutomatically(true);   // 自动加载图片

        // --- 文件与跨域访问 ---
        settings.setAllowFileAccess(true);            // 允许访问本地文件
        settings.setJavaScriptCanOpenWindowsAutomatically(true); // 允许JS打开新窗口
        webView.requestFocus();

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                isPageError = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    tvTitle.setText(getRealTitle(title));
                } else {
                    tvTitle.setText("网页");
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                        errorCode == WebViewClient.ERROR_CONNECT ||
                        errorCode == WebViewClient.ERROR_TIMEOUT) {
                    showErrorView("网络连接失败\n请检查网络后重试");
                }
            }

            public void onReceivedError(WebView view, WebResourceError error) {
                if (error.getErrorCode() != -1) {
                    showErrorView("加载失败\n" + error.getDescription());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.isEmpty()) {
                    tvTitle.setText(getRealTitle(title));
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.postDelayed(() -> progressBar.setVisibility(View.GONE), 300);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                if (mCustomView != null) {
                    if (callback != null) {
                        callback.onCustomViewHidden();
                    }
                    return;
                }
                mCustomView = view;
                mCustomView.setVisibility(View.VISIBLE);
                mCustomViewCallback = callback;
                frameLayout.addView(mCustomView);
                frameLayout.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                
                // 全屏显示：隐藏状态栏和导航栏
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
                
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                if (mCustomView != null) {
                    mCustomView.setVisibility(View.GONE);
                    frameLayout.removeView(mCustomView);
                    mCustomView = null;
                    frameLayout.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    
                    // 恢复状态栏和导航栏
                    getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    );
                    
                    try {
                        if (mCustomViewCallback != null) {
                            mCustomViewCallback.onCustomViewHidden();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mCustomViewCallback = null;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            }
        });

        webView.loadUrl(url);
    }

    String getRealTitle(String str){
        if (TextUtils.isEmpty(str)){
            return "网页";
        }
        try {
            String[] strArr=str.split("/");
            if (strArr.length>1){
                String newStr=strArr[strArr.length-1];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return URLDecoder.decode(newStr, StandardCharsets.UTF_8);
                }
                return newStr;
            } else {
                return str;
            }
        } catch (Exception e) {

        }

        return "网页";
    }

    private void showErrorView(String message) {
        isPageError = true;
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvErrorMsg.setText(message);
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCustomView != null) {
                webView.getWebChromeClient().onHideCustomView();
                return true;
            } else if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}