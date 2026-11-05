package com.gsoft.inventory.utils.down;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.gsoft.inventory.utils.FileProviderUtil;
import okhttp3.HttpUrl;

/**
 * H5 附件下载与打开工具类
 * 统一维护 Download/zr_app 目录、文件名清洗、MIME 映射与外部应用调起
 *
 * @author 10904
 */
public class DownloadedFileUtils {

    /** 附件统一保存目录：公共下载目录下的 zr_app 子目录 */
    public static final String DOWNLOAD_DIR_NAME = "zr_app";

    private static final String[] ILLEGAL_FILE_CHARS = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    private DownloadedFileUtils() {
    }

    /**
     * 获取附件保存目录，不存在时自动创建
     */
    public static File getDownloadDir() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File targetDir = new File(downloadDir, DOWNLOAD_DIR_NAME);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        return targetDir;
    }

    /**
     * 清洗文件名：替换文件系统非法字符，去掉首尾空格与结尾点
     * H5 回调用原始 fileName 匹配，落盘使用清洗后的名字
     */
    public static String sanitizeFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return buildUnnamedFileName();
        }
        String name = fileName;
        for (String illegalChar : ILLEGAL_FILE_CHARS) {
            name = name.replace(illegalChar, "_");
        }
        name = name.replace("\r", "_").replace("\n", "_").trim();
        while (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        if (TextUtils.isEmpty(name)) {
            return buildUnnamedFileName();
        }
        // 超长文件名截断，保留扩展名
        if (name.length() > 100) {
            int dotIndex = name.lastIndexOf('.');
            String extension = dotIndex > 0 && name.length() - dotIndex <= 10 ? name.substring(dotIndex) : "";
            name = name.substring(0, 100 - extension.length()) + extension;
        }
        return name;
    }

    /**
     * 根据 H5 传入的 fileName 获取本地文件
     */
    public static File getLocalFile(String fileName) {
        return new File(getDownloadDir(), sanitizeFileName(fileName));
    }

    /**
     * 通知栏展示名：去掉 H5 约定的 "{fileId}_" 前缀
     */
    public static String getDisplayName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int index = fileName.indexOf('_');
        if (index > 0 && index < fileName.length() - 1) {
            String prefix = fileName.substring(0, index);
            boolean fileIdPrefix = true;
            for (int i = 0; i < prefix.length(); i++) {
                if (!Character.isLetterOrDigit(prefix.charAt(i))) {
                    fileIdPrefix = false;
                    break;
                }
            }
            if (fileIdPrefix) {
                return fileName.substring(index + 1);
            }
        }
        return fileName;
    }

    /**
     * 按扩展名获取 MIME 类型，未匹配到使用 MimeTypeMap 兜底，仍未知则返回 "
     * */
    public static String getMimeType(String fileName) {
        String extension = "";
        if (!TextUtils.isEmpty(fileName)) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.getDefault());
            }
        }
        String mimeType = null;
        switch (extension) {
            case "pdf":
                mimeType = "application/pdf";
                break;
            case "doc":
            case "docx":
                mimeType = "application/msword";
                break;
            case "xls":
            case "xlsx":
                mimeType = "application/vnd.ms-excel";
                break;
            case "ppt":
            case "pptx":
                mimeType = "application/vnd.ms-powerpoint";
                break;
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "png":
                mimeType = "image/png";
                break;
            case "gif":
                mimeType = "image/gif";
                break;
            case "txt":
                mimeType = "text/plain";
                break;
            case "zip":
                mimeType = "application/zip";
                break;
            case "rar":
                mimeType = "application/x-rar-compressed";
                break;
            case "7z":
                mimeType = "application/x-7z-compressed";
                break;
            case "mp4":
                mimeType = "video/mp4";
                break;
            case "mp3":
                mimeType = "audio/mpeg";
                break;
            default:
                break;
        }
        if (TextUtils.isEmpty(mimeType) && !TextUtils.isEmpty(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return TextUtils.isEmpty(mimeType) ? "*/*" : mimeType;
    }

    /**
     * 规范化下载地址：H5 已做 encodeURI，这里对裸中文/空格等做兜底编码
     */
    public static String normalizeUrl(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return "";
        }
        String trimmedUrl = rawUrl.trim();
        try {
            HttpUrl httpUrl = HttpUrl.parse(trimmedUrl);
            if (httpUrl != null) {
                return httpUrl.toString();
            }
        } catch (Exception ignored) {
        }
        try {
            // 保留协议、查询串等分隔符，仅编码中文与空格等非法字符
            String encodedUrl = Uri.encode(trimmedUrl, ":/?#[]@!$&'()*+,;=%");
            HttpUrl httpUrl = HttpUrl.parse(encodedUrl);
            return httpUrl != null ? httpUrl.toString() : encodedUrl;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 使用外部应用打开文件
     *
     * @return null 表示成功调起；否则返回失败文案
     */
    public static String openWithExternalApp(Context context, File file, String mimeType) {
        if (file == null || !file.exists() || file.length() <= 0) {
            return "文件不存在或内容为空";
        }
        try {
            Uri contentUri = FileProviderUtil.getFileUri(context, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, TextUtils.isEmpty(mimeType) ? "*/*" : mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return null;
        } catch (ActivityNotFoundException e) {
            return "未找到可打开该文件的应用";
        } catch (Exception e) {
            return "打开文件失败";
        }
    }

    private static String buildUnnamedFileName() {
        return "未命名_" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
    }
}