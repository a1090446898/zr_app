package com.gsoft.inventory.service.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;
import com.gsoft.inventory.utils.down.DownloadedFileUtils;


import java.io.File;

/**
 * 下载完成通知的点击入口：使用外部应用打开已下载附件
 *
 * @author 10904
 */
public class OpenDownloadedFileReceiver extends BroadcastReceiver {

    public static final String ACTION_OPEN_DOWNLOADED_FILE = "com.gsoft.inventory.action.OPEN_DOWNLOADED_FILE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String filePath = intent.getStringExtra(DownloadNotificationHelper.EXTRA_FILE_PATH);
        String mimeType = intent.getStringExtra(DownloadNotificationHelper.EXTRA_FILE_MIME);
        String fileName = intent.getStringExtra(DownloadNotificationHelper.EXTRA_FILE_NAME);
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        String error = DownloadedFileUtils.openWithExternalApp(context, new File(filePath), mimeType);
        if (error != null) {
            Toast.makeText(context, (TextUtils.isEmpty(fileName) ? "" : fileName + "：") + error,
                    Toast.LENGTH_LONG).show();
        }
    }
}