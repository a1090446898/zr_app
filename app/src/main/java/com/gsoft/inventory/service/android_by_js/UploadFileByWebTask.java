package com.gsoft.inventory.service.android_by_js;

import android.content.Context;
import android.net.Uri;
import com.gsoft.inventory.utils.OkHttpUtils;

import java.util.concurrent.Callable;

/**
 * @author 10904
 * 导出资产到文件操作
 */
public class UploadFileByWebTask implements Callable<String> {

    private Context context;
    private String tempFileUri;
    private String uploadUrl;
    private String id;

    public UploadFileByWebTask(Context context, String tempFileUri, String uploadUrl, String id) {
        this.id = id;

        this.context = context;
        this.tempFileUri = tempFileUri;
        this.uploadUrl = uploadUrl;
    }


    @Override
    public String call() {
        return OkHttpUtils.getInstance().uploadFileByContentUri(context, Uri.parse(tempFileUri), uploadUrl);
    }
}
