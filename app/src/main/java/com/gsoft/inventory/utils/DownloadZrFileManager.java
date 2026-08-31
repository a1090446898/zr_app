package com.gsoft.inventory.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

/**
 * @author 10904
 */
public class DownloadZrFileManager {

    private static final String TAG = "DownloadZrFileManager";
    private static final String ZR_DIR_NAME = "zr";

    /**
     * 获取Downloads/zr目录
     * @param context 上下文
     * @return 目标目录文件对象
     */
    public static File getZrDirectory(Context context) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File zrDir = new File(downloadDir, ZR_DIR_NAME);

        if (!zrDir.exists()) {
            if (!zrDir.mkdirs()) {
                Log.e(TAG, "创建zr目录失败: " + zrDir.getAbsolutePath());
            }
        }

        return zrDir;
    }

    /**
     * 检查是否有外部存储写入权限
     * @param context 上下文
     * @return 是否有权限
     */
    public static boolean hasWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 检查 MANAGE_EXTERNAL_STORAGE 权限
                return Environment.isExternalStorageManager();
            } else {
                // Android 6.0-10 检查 WRITE_EXTERNAL_STORAGE 权限
                return ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }
        }
        // Android 6.0以下默认有权限
        return true;
    }

    /**
     * 请求写入权限
     * @param activity 上下文
     * @param requestCode 请求码
     */
    public static void requestWritePermission(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 引导用户去设置页面开启 MANAGE_EXTERNAL_STORAGE 权限
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivityForResult(intent, requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-10 请求 WRITE_EXTERNAL_STORAGE 权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        }
    }

    /**
     * 创建指定文件名的文件
     * @param context 上下文
     * @param fileName 文件名
     * @return 文件对象
     */
    public static File createFileInZrDir(Context context, String fileName) {
        File zrDir = getZrDirectory(context);
        return new File(zrDir, fileName);
    }

    /**
     * 检查是否可以写入文件
     * @param context 上下文
     * @return 是否可以写入
     */
    public static boolean canWriteToZrDir(Context context) {
        File zrDir = getZrDirectory(context);
        return zrDir.exists() && zrDir.canWrite();
    }
}
