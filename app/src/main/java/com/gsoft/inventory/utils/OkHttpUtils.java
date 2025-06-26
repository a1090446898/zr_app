package com.gsoft.inventory.utils;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

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
        try  {
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
        try{
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
}
