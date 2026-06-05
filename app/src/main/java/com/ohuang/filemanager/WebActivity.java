package com.ohuang.filemanager;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WebActivity extends Activity {
    public static void start(Context context, String url) {
        Intent starter = new Intent(context, WebActivity.class);
        starter.putExtra("url", url);
        context.startActivity(starter);
    }

    private WebView webView;
    private TextView tvProgress;
    private FrameLayout frameLayout;
    private View mCustomView;
    WebChromeClient.CustomViewCallback mCustomViewCallback;
    public String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        url = getIntent().getStringExtra("url");
        webView = (WebView) findViewById(R.id.wv_webview);
        frameLayout = findViewById(R.id.fl_web);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        webView.requestFocus();
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        WebViewSetting.INSTANCE.setting(webView, this);
        //获得其他控件
        tvProgress = (TextView) findViewById(R.id.tv_progress);
        //访问网页
        webView.loadUrl(url);
//系统默认会通过手机浏览器打开网页，为了能够直接通过WebView显示网页，则必须设置

        //设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //使用WebView加载显示url
                view.loadUrl(url);
//返回true
                return true;
            }

            //加载前
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                tvProgress.setText("开始加载！！");

            }

            //加载完成
            @Override
            public void onPageFinished(WebView view, String url) {

                tvProgress.setText("加载完成...");

            }
        });
//设置WebChromeClient类
        webView.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {

            }

            //进度显示
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    tvProgress.setText(newProgress + "%");
                } else {
                    tvProgress.setText("100%");
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
                webView.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                mCustomView.setVisibility(View.GONE);
                frameLayout.removeView(mCustomView);
                mCustomView = null;
                webView.setVisibility(View.VISIBLE);

                try {
                    if(mCustomViewCallback!=null) {
                        mCustomViewCallback.onCustomViewHidden();
                    }

                } catch (Exception e){
                }
                mCustomViewCallback=null;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

    }

    //点击返回上一页面而不是退出浏览器
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //销毁Webview
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}