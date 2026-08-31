package com.gsoft.inventory.abs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.common.AssetReceiveAndroidByJs;
import com.gsoft.inventory.config.WebChromeClientConfig;
import com.gsoft.inventory.config.WebViewClientConfig;
import com.gsoft.inventory.utils.CommonPermissionUtils;
import com.gsoft.inventory.utils.FileToolUtils;

import static android.app.Activity.RESULT_OK;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.*;

/**
 * @author 10904
 */
public abstract class WebFragment extends BaseFragment {

    protected WebView webView;

    private WebViewClientConfig webViewClientConfig;
    private WebChromeClientConfig webChromeClientConfig;

    private AssetReceiveAndroidByJs androidByJs;

    private ProgressBar progressBar;

    private FragmentActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.activity = getActivity();
        this.mContext = getContext();

        return inflater.inflate(R.layout.activity_asset_web, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        progressBar = view.findViewById(R.id.progressBar);
        webView = view.findViewById(R.id.webView);

        webViewClientConfig = new WebViewClientConfig(this.activity, progressBar);
        webChromeClientConfig = new WebChromeClientConfig(this.activity, progressBar);

        bindWebView();


    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindWebView() {



        // 配置WebSettings
        WebSettings settings = webView.getSettings();
        // 启用JS
        settings.setJavaScriptEnabled(true);
        // 支持缩放
        settings.setSupportZoom(false);
        // 允许JS打开新窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 自适应屏幕
        settings.setLoadWithOverviewMode(true);
        // 宽视口
        settings.setUseWideViewPort(true);
        // 缓存模式
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // 允许 WebView 访问文件系统。启用此设置后，WebView 可以读取设备上的文件。
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // 启用 DOM 存储 API，例如 localStorage 和 sessionStorage。
        // 这些存储 API 允许网页在客户端存储数据，方便实现一些功能，如记住用户偏好设置、保存临时数据等。
        settings.setDomStorageEnabled(true);

        // 允许混合内容（http/https）
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);


        // 启用硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // 设置 WebViewClientConfig并使用 assetLoader 处理资源加载
        webView.setWebViewClient(webViewClientConfig);

        // 设置WebChromeClientConfig处理文件上传
        webView.setWebChromeClient(webChromeClientConfig);

        // 注册JS接口（供网页调用安卓功能）
        webView.addJavascriptInterface(this.getAndroidByJs(), "AssetsAndroid");

        // 监听网页日志
        listenWebView();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CommonPermissionUtils.STORAGE_PERMISSION_REQUEST_CODE) {
            if (!CommonPermissionUtils.checkPermissionResult(grantResults)) {
                showShortText("需要存储权限才能上传文件");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 释放WebView资源，避免内存泄漏
        webView.destroy();
    }



    // 处理Activity结果
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫码结果处理
        handleScanCodeCallBack(requestCode, resultCode, data);
        // 拍照结果处理
        handleTakePhotoCallBack(requestCode, resultCode, data);
        // 处理文件选择
        handleTakeFileCallBack(requestCode, resultCode, data);

    }


    private void listenWebView() {
        // 监听网页日志输出并打印到日志
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                // 打印日志到日志
                Log.d("WebViewConsole", message + " (Line: " + lineNumber + ", Source: " + sourceID + ")");
            }
        });
    }


    private void handleTakePhotoCallBack(int requestCode, int resultCode, Intent data) {
        //  添加拍照结果状态判断
        if (requestCode == TAKE_PHOTO_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri capturedImageUri = this.getAndroidByJs().getCapturedImageURI();

            if (capturedImageUri != null) {
                try {
                    String base64Image = FileToolUtils.convertImageToBase64(mContext, capturedImageUri);
                    String imageBase64 = "data:image/jpeg;base64," + base64Image;
                    this.getAndroidByJs().returnImagePath(imageBase64);
                    //  添加临时文件清理
                    FileToolUtils.deleteFileByUri(mContext, capturedImageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                    //  添加错误处理
                    showShortText("图片处理失败");
                }
            }
        } else {
            Uri capturedImageUri = this.getAndroidByJs().getCapturedImageURI();
            if (capturedImageUri != null) {
                // 删除未使用的临时文件
                FileToolUtils.deleteFileByUri(mContext, capturedImageUri);
                // 重置 Uri 状态，避免下次拍照复用旧 Uri
                this.getAndroidByJs().returnImagePath("");
            }
        }
    }

    private void handleTakeFileCallBack(int requestCode, int resultCode, Intent data) {

        // 只处理文件选择器的返回结果
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) {
            return;
        }

        // 情况1：用户取消选择
        if (resultCode != RESULT_OK) {
            // 通知H5用户取消选择
            this.getAndroidByJs().onFileSelected(null);
            showShortText("已取消选择");
            return;
        }

        // 情况2：用户选择了文件，但未返回数据（异常处理）
        if (data == null) {
            this.getAndroidByJs().onFileSelected(null);
            showShortText("未获取到文件信息");
            return;
        }

        // 检查是否有临时读取权限
        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (takeFlags == 0) {
            showShortText("未获取文件读取权限");
            return;
        }

        // 情况3：正常获取到文件，处理返回的Uri
        Uri selectedUri = data.getData();
        if (selectedUri != null) {
            try {
                String base64Str = FileToolUtils.convertFileToBase64(mContext, selectedUri);
                // 使用工具类获取MIME类型和文件名
                String mimeType = FileToolUtils.getMimeType(mContext, selectedUri);
                String fileName = FileToolUtils.getFileName(mContext, selectedUri);
                // 使用实际MIME类型构建base64前缀
                String fileBase64 = "data:" + mimeType + ";base64," + base64Str;
                this.getAndroidByJs().returnFilePath(fileName, fileBase64);
            } catch (Exception e) {
                e.printStackTrace();
                //  添加错误处理
                showShortText("文件处理失败");
            }
        } else {
            // 重置 Uri 状态，避免下次拍照复用旧 Uri
            this.getAndroidByJs().returnFilePath("", "");
        }
    }


    /**
     * 处理扫码回传结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    private void handleScanCodeCallBack(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BARCODE_SCAN && resultCode == RESULT_OK) {
            if (data == null) {
                return;
            }
            String barcodeResult = data.getStringExtra("barcode_result");
            Log.d("扫码结果： ", barcodeResult);
            // 将扫码结果直接返回给 WebView
            if (barcodeResult != null) {
                this.getAndroidByJs().returnScanCode(barcodeResult);
            }
        }
    }


    public AssetReceiveAndroidByJs getAndroidByJs() {
        // 加双重锁
        synchronized (AssetReceiveAndroidByJs.class){
            if(this.androidByJs == null){
                this.androidByJs = new AssetReceiveAndroidByJs(getActivity(), webView, this);
            }
            return this.androidByJs;
        }
    }
}
