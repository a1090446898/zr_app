package com.gsoft.inventory.config;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.FILE_CHOOSER_REQUEST_CODE;
import static com.gsoft.inventory.utils.DateFormatUtils.YYYYMMDD_HHMMSS;

/**
 * @author 10904
 */
public class WebChromeClientConfig extends WebChromeClient {

    private Activity activity;
    private ValueCallback<Uri[]> filePathCallback;


    public WebChromeClientConfig(Activity activity, ProgressBar progressBar) {
        this.activity = activity;
    }

    // 处理文件选择回调
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

        // 如果已有回调未处理，先取消
        if(this.filePathCallback != null){
            this.filePathCallback.onReceiveValue(null);
        }
        this.filePathCallback = filePathCallback;
        return openFileChooser(fileChooserParams);
    }

    // 打开文件选择器
    private boolean openFileChooser(FileChooserParams fileChooserParams) {
        // 根据网页要求的MIME类型决定可选择的文件类型
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        Intent intent = createFileChooserIntent(acceptTypes);

        try {
            this.activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
        } catch (Exception e) {
            this.filePathCallback = null;
            Toast.makeText(this.activity, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 根据接受的MIME类型创建文件选择意图
     */
    private Intent createFileChooserIntent(String[] acceptTypes) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 检查是否有指定的MIME类型
        if (acceptTypes != null && acceptTypes.length > 0) {
            boolean isImageType = false;
            boolean isPdfType = false;

            // 检查接受的文件类型
            for (String type : acceptTypes) {
                if (type.contains("image/")) {
                    isImageType = true;
                }
                if (type.contains("application/pdf")) {
                    isPdfType = true;
                }
            }

            // 设置对应的MIME类型
            if (isImageType && isPdfType) {
                // 同时支持图片和PDF
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            } else if (isImageType) {
                // 只支持图片
                intent.setType("image/*");
            } else if (isPdfType) {
                // 只支持PDF
                intent.setType("application/pdf");
            } else {
                // 支持所有文件类型
                intent.setType("*/*");
            }
        } else {
            // 默认支持所有文件类型
            intent.setType("*/*");
        }

        return intent;
    }

    public ValueCallback<Uri[]> getFilePathCallback() {
        return filePathCallback;
    }

    public void setFilePathCallback(ValueCallback<Uri[]> filePathCallback) {
        this.filePathCallback = filePathCallback;
    }
}
