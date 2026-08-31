package com.gsoft.inventory.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.gsoft.inventory.BuildConfig;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 10904
 */
public class CommonUtils {

    private final static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final static Handler HANDLER = new Handler(Looper.getMainLooper());


    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
    public static Handler getHandler() {
        return HANDLER;
    }

    /**
     * 生成两位字符串，第一位随机从a-z，A-Z中随机取一个，第二位随机从0-9中随机取一个
     * @return 生成的两位随机字符串
     */
    public static String getRandomTwoString(){
        Random random = new Random();
        // 生成 0 - 51 的随机数，0 - 25 对应 a - z，26 - 51 对应 A - Z
        int firstCharIndex = random.nextInt(52);
        char firstChar;
        if (firstCharIndex < 26) {
            // 生成 a - z
            firstChar = (char) ('a' + firstCharIndex);
        } else {
            // 生成 A - Z
            firstChar = (char) ('A' + firstCharIndex - 26);
        }
        // 生成 0 - 9 的随机数
        int secondCharIndex = random.nextInt(10);
        char secondChar = (char) ('0' + secondCharIndex);
        return "" + firstChar + secondChar;
    }

    /**
     * 获取文件扩展名（如.pdf、.jpg）
     */
    private static String getFileExtension(ContentResolver resolver, Uri uri) {
        String mimeType = resolver.getType(uri);
        if (mimeType == null) {
            // 未知类型默认扩展名
            return ".bin";
        }
        // 根据MIME类型映射扩展名（可扩展更多类型）
        switch (mimeType) {
            case "application/pdf":
                return ".pdf";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "application/vnd.ms-excel":
                return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            default:
                return "." + mimeType.replace("/", ".");
        }
    }


    /**
     * 根据Uri获取文件名（Android 11+ 通用）
     * @param context 上下文（需传入Activity/Fragment的Context，避免内存泄漏）
     * @param uri 目标文件的Uri（来自MediaStore或SAF）
     * @return 文件名（含后缀），若查询失败返回null
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }

        // 1. 特殊处理：若Uri是"content://com.android.externalstorage.documents"类型（SAF获取的外部存储文件）
        // 无需额外逻辑，直接查询DISPLAY_NAME即可

        // 2. 通过ContentResolver查询文件名
        String fileName = null;
        ContentResolver contentResolver = context.getContentResolver();
        // 定义要查询的字段：仅查询文件名（DISPLAY_NAME）
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        // 执行查询（第三个参数selection为null，表示查询所有匹配Uri的记录）
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        if (cursor != null) {
            try {
                // 游标移动到第一条记录（Uri对应单个文件，仅一条记录）
                if (cursor.moveToFirst()) {
                    // 获取DISPLAY_NAME字段的索引，再提取值
                    int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                e.printStackTrace(); // 捕获查询异常（如Uri无效、权限不足）
            } finally {
                cursor.close(); // 必须关闭Cursor，释放资源
            }
        }

        return fileName;
    }

    public static String getCompanyNameByFlavor() {
        String flavor = BuildConfig.FLAVOR;
        switch (flavor) {
            case "companyKaiYang":
                return "开阳县人民检察院";
            case "mainFlavor":
            default:
                return "贵州智软科技（集团）有限公司";
        }
    }
}
