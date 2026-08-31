package com.gsoft.inventory.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.PERMISSION_REQUEST_CAMERA;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.PERMISSION_REQUEST_CODE;

/**
 * 权限检查工具类
 * @author 10904
 */
public class CommonPermissionUtils {

    // 权限请求码
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 101;

    // 所需存储权限
    private static final String[] STORAGE_PERMISSIONS = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    /**
     * 检查存储权限
     * @param activity
     * @return
     */
    public static boolean checkStoragePermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public static boolean checkCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求相机权限
     */
    public static boolean requestCameraPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            return false;
        }
        return true;

    }

    /**
     * 请求获取文件权限
     * @param activity
     * @return
     */
    public static boolean requestReadFilePermission(Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    /**
     * 请求获取相册权限
     * @param activity
     * @return
     */
    public static boolean requestAlbumPermission(Activity activity){

        if (ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }



    public static boolean checkPermissionResult(int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }



    // 检查是否拥有所有必要的存储权限
    public static boolean checkStoragePermissions(Activity activity) {
        for (String permission : STORAGE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 请求存储权限
    public static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                STORAGE_PERMISSIONS,
                STORAGE_PERMISSION_REQUEST_CODE
        );
    }




}
