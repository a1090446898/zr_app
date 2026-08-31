package com.gsoft.inventory.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import id.zelory.compressor.Compressor;

import java.io.*;
import java.util.List;

/**
 * @author 10904
 */
public class FileToolUtils {

    /**
     * 将图片文件转换为 Base64 编码
     * @return Base64 编码的字符串
     * @throws IOException 读取文件时发生错误
     */
    public static String convertImageToBase64(Context context, Uri uri) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        try {

            // 通过 ContentResolver 直接读取 URI 内容（支持所有类型 URI）
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("无法打开文件输入流");
            }

            // 临时文件处理（用于图片压缩）
            File tempFile = File.createTempFile("temp_image", ".jpg", context.getCacheDir());
            try (FileOutputStream tempOutputStream = new FileOutputStream(tempFile)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    tempOutputStream.write(buffer, 0, bytesRead);
                }
            }

            // 使用 Compressor 压缩临时文件
            File compressedImageFile = new Compressor(context)
                    .setMaxWidth(1280)
                    .setMaxHeight(720)
                    .setQuality(80)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .compressToFile(tempFile);

            // 读取压缩后的文件内容
            try (FileInputStream compressedInputStream = new FileInputStream(compressedImageFile)) {
                while ((bytesRead = compressedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 清理临时文件
            tempFile.delete();
            compressedImageFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    /**
     * 将文件转换为 Base64 编码
     * @return Base64 编码的字符串
     * @throws IOException 读取文件时发生错误
     */
    public static String convertFileToBase64(Context context, Uri uri) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("无法打开文件输入流");
            }

            // 读取文件内容到字节数组输出流
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        byte[] fileBytes = outputStream.toByteArray();
        // 使用 NO_WRAP 标志确保输出不包含换行符，与图片转换方法保持一致
        return Base64.encodeToString(fileBytes, Base64.NO_WRAP);
    }

    /**
     * 获取文件MIME类型
     * @param context 上下文
     * @param uri 文件Uri
     * @return MIME类型，默认返回"*//*
    */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return StringUtils.isNullOrEmpty(mimeType) ? "*/*" : mimeType;
    }

    /**
     * 获取文件名
     * @param context 上下文
     * @param uri 文件Uri
     * @return 文件名，默认返回"unknown_file"
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (!cursor.isNull(nameIndex)) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        return fileName != null ? fileName : "unknown_file";
    }

    /**
     * 删除文件
     * @param path
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

    }


    // 添加文件删除方法
    public static void deleteFileByUri(Context context, Uri uri) {
        if (uri == null) return;

        try {
            // 根据Uri获取文件路径
            String path = getPathFromUri(context, uri);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Uri转文件路径方法（修复FileProvider Uri解析）
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        // 处理FileProvider的content:// Uri（关键修复）
        if ("content".equals(uri.getScheme()) && uri.getAuthority() != null) {
            // 检查是否为应用内FileProvider生成的Uri
            if (uri.getAuthority().equals(context.getPackageName() + ".provider")) {
                // 提取Uri中的路径片段（对应file_paths.xml配置）
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() >= 2) {
                    // 拼接真实文件路径：外部文件目录 + FileProvider配置的路径 + 文件名
                    // 例如：/storage/emulated/0/Android/data/com.gsoft.inventory/files/temp_pictures/xxx.jpg
                    // 对应file_paths.xml中的name属性（temp_pictures）
                    String type = pathSegments.get(0);
                    // 文件名
                    String fileName = pathSegments.get(1);

                    File externalFilesDir = context.getExternalFilesDir(type);
                    if (externalFilesDir != null) {
                        return new File(externalFilesDir, fileName).getAbsolutePath();
                    }
                }
            }
        }
        // 处理file:// Uri（低版本兼容）
        else if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}
