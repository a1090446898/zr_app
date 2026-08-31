package com.gsoft.inventory.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.*;
import okio.BufferedSink;

public class OkHttpUtils {
    private static OkHttpUtils okHttpUtils = null;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    private OkHttpUtils() {
    }

    public OkHttpClient getClient() {
        return client;
    }

    public static OkHttpUtils getInstance() {
        if (okHttpUtils == null) {
            synchronized (OkHttpClient.class) {
                if (okHttpUtils == null) {
                    okHttpUtils = new OkHttpUtils();
                }
            }
        }
        return okHttpUtils;
    }

    public String get(String url) {
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            String result = body.string();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    public void get(String url, StringCallback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    public String post(String url, Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) return response.body().string();
            return "ERROR:".concat("网络返回数据错误！");
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    public String postJson(String url, String jsonString) {
        try {
            RequestBody body = RequestBody.create(JSON, jsonString);
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = client.newCall(request).execute();
            if (response.body() != null) return response.body().string();
            return "ERROR:".concat("网络返回数据错误！");
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    public String postAssetsJson(String url, String jsonString) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("data", jsonString);
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) return response.body().string();
            return "ERROR:".concat("网络返回数据错误！");
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    public void postAssetsJson(String url, String jsonString, StringCallback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("data", jsonString);
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        client.newCall(request).enqueue(callback);
    }

    public void postJsonAsync(String url, String jsonString, StringCallback callback) {
        Log.e("okHttp-post", jsonString);
        RequestBody body = RequestBody.create(JSON, jsonString);
        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void uploadImage(String url, File file, MediaType mediaType, Map<String, String> params, Callback callback) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        if (params != null && !params.keySet().isEmpty()) {
            for (String key : params.keySet()) {
                builder.addFormDataPart(key, params.get(key) + "");
            }
        }

        RequestBody imgBody = RequestBody.create(mediaType, file);
        builder.addFormDataPart("file", file.getName(), imgBody);
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder().url(url).post(requestBody).build();
        client.newCall(request).enqueue(callback);
    }

    public void uploadImageFile(String url, File imageFile, Map<String, String> params, Callback callback) {
        uploadImage(url, imageFile, MediaType.parse("image/jpeg"), params, callback);
    }

    public void uploadImageFile(String url, String filePath, Map<String, String> params, Callback callback) {
        File upFile = new File(filePath);
        uploadImage(url, upFile, MediaType.parse("image/jpeg"), params, callback);
    }


    /**
     * 使用OkHttp上传content://格式的文件
     *
     * @param context    上下文
     * @param contentUri 要上传的文件Uri（content://格式）
     * @param uploadUrl  后端上传接口
     */
    public String uploadFileByContentUri(Context context, Uri contentUri, String uploadUrl) {
        try {
            // 1. 获取ContentResolver和输入流
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(contentUri);
            if (inputStream == null) {
                return "无法打开文件输入流";
            }

            // 2. 获取文件MIME类型
            String mimeType = resolver.getType(contentUri);
            if (mimeType == null) {
                // 默认二进制类型
                mimeType = "application/octet-stream";
            }

            // 3. 构建Multipart请求体
            String finalMimeType = mimeType;
            RequestBody fileBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse(finalMimeType);
                }

                @Override
                public void writeTo(@NonNull BufferedSink sink) throws IOException {
                    // 直接写入输入流内容
                    byte[] buffer = new byte[1024 * 8];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        sink.write(buffer, 0, len);
                    }
                    inputStream.close(); // 关闭输入流
                }

                @Override
                public long contentLength() {
                    try {
                        return inputStream.available();
                    } catch (IOException e) {
                        return -1; // 无法获取长度时返回-1
                    }
                }
            };

            // 获取文件名
            String fileNameByUri = CommonUtils.getFileNameFromUri(context, contentUri);
            if (StringUtils.isNullOrEmpty(fileNameByUri)) {
                fileNameByUri = "uploadFile";
            }

            // 4. 构建Multipart表单
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "file",
                            fileNameByUri,
                            fileBody
                    )
                    .build();

            // 5. 构建请求
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(multipartBody)
                    .build();

            // 6. 执行请求
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                return response.body().string();
            }

        } catch (Exception e) {
            Log.e("uploadFileByContentUri", "上传异常", e);
            return "上传异常：" + e.getMessage();
        }

        return "上传失败";
    }

}
