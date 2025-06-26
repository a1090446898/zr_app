package com.gsoft.inventory.utils;


import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpRequestUtils {
    protected static Gson gson = new Gson();
    protected static OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();


    public static void getAsync(String url, Callback callback) {
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(callback);
    }

}
