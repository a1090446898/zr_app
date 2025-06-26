package com.gsoft.inventory.update;

import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.xuexiang.xupdate.proxy.IUpdateHttpService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OKHttpUpdateHttpService implements IUpdateHttpService {
    private OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
    private Gson gson = new Gson();

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, Object> params, @NonNull Callback callBack) {
        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (String key : params.keySet()) {
            builder.appendQueryParameter(key, params.get(key) + "");
        }
        String remoteUrl = builder.toString();
        Request request = new Request.Builder().url(remoteUrl).build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseTxt = response.body().string();
                    new Runnable() {
                        @Override
                        public void run() {
                            callBack.onSuccess(responseTxt);
                        }
                    };
                } else {
                    new Runnable() {
                        @Override
                        public void run() {
                            callBack.onError(new Exception("请求失败"));
                        }
                    };
                }
            }
        });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, Object> params, @NonNull Callback callBack) {
        Request request = new Request.Builder().url(SysDefine.SERVER_APK_UPDATE()).build();
        OkHttpUtils.getInstance().getClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callBack.onSuccess(response.body().toString());
                }
            }
        });
    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull DownloadCallback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    double current = 0;
                    long total = response.body().contentLength();
                    is = response.body().byteStream();
                    File file = new File(SysConfig.apk_temp_file, FileSearchHelper.getFileName(url));
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        Log.e("downloadAPK", "current------>" + current);
                        final double sum = current;
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress((float) (sum / total), total);
                            }
                        };
                    }
                    fos.flush();
                    //如果下载文件成功，第一个参数为文件的绝对路径
                    new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(file);
                        }
                    };
                } catch (IOException e) {
                    new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e);
                        }
                    };
                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (fos != null) fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void cancelDownload(@NonNull String url) {

    }
}
