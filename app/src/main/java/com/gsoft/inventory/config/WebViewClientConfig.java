package com.gsoft.inventory.config;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.webkit.WebViewAssetLoader;

/**
 * @author 10904
 */
public class WebViewClientConfig extends WebViewClient {

    private WebViewAssetLoader assetLoader;

    private Activity activity;

    private ProgressBar progressBar;


    public WebViewClientConfig(Activity activity, ProgressBar progressBar) {
        this.activity = activity;
        this.progressBar = progressBar;

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(activity))
                .addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(activity))
                .addPathHandler("/files/", new WebViewAssetLoader.InternalStoragePathHandler(activity, activity.getFilesDir()))
                .build();
    }


    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        // 开始加载时显示进度条
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 使用 WebViewAssetLoader 安全地加载本地资源
     * @param view
     * @param request
     * @return
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return assetLoader.shouldInterceptRequest(request.getUrl());
    }

    @Override
    // 使用新的方法替代弃用的方法
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // 获取请求的 URL
        String url = request.getUrl().toString();
        // 所有链接在当前 WebView 中打开，不跳转浏览器
        view.loadUrl(url);
        return true;
    }

    // 保留旧方法，确保兼容低版本 Android
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // 所有链接在当前 WebView 中打开，不跳转浏览器
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // 页面加载完成后的操作
        super.onPageFinished(view, url);
        // 隐藏进度条
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

}
