package com.gsoft.inventory.service.download;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.webkit.CookieManager;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.gsoft.inventory.utils.down.DownloadedFileUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 附件下载管理器
 * 负责 Download/zr_app 目录下的下载、同名覆盖、并发任务合并、进度节流与通知
 * 下载统一写入 .tmp 临时文件，成功后再重命名，保证正式文件始终完整
 *
 * @author 10904
 */
public class ZRFileDownloadManager {

    /**
     * 单任务进度回调最小间隔（毫秒）
     */
    private static final long PROGRESS_INTERVAL = 200L;
    /**
     * 多任务并发时全局进度回调最小间隔，避免 WebView 卡顿
     */
    private static final long GLOBAL_PROGRESS_INTERVAL = 80L;
    /**
     * 通知栏进度刷新间隔
     */
    private static final long NOTIFICATION_INTERVAL = 500L;

    private static volatile ZRFileDownloadManager instance;

    private final OkHttpClient httpClient;
    private final ConcurrentHashMap<String, DownloadTask> taskMap = new ConcurrentHashMap<>();
    private final Object taskLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicLong lastGlobalProgressTime = new AtomicLong(0);
    private final DownloadNotificationHelper notificationHelper;

    /**
     * 单个发起方的回调
     */
    public interface RequestCallback {
        /**
         * 下载进度，percent 为 -1 表示不确定进度
         */
        void onProgress(String fileName, int percent, long currentBytes, long totalBytes);

        /**
         * 任务终态：success 为 true 时 savedPath 为完整保存路径
         */
        void onFinished(boolean success, String savedPath, String msg);
    }

    private ZRFileDownloadManager(android.content.Context context) {
        android.content.Context appContext = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.notificationHelper = new DownloadNotificationHelper(appContext);
    }

    public static ZRFileDownloadManager getInstance(android.content.Context context) {
        if (instance == null) {
            synchronized (ZRFileDownloadManager.class) {
                if (instance == null) {
                    instance = new ZRFileDownloadManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 加入下载任务
     *
     * @param needNotification true：downloadFile 发起，显示通知栏进度与页面进度；
     *                         false：openFile 的静默下载，不显示通知与进度
     */
    public void enqueue(String fileUrl, String fileName, boolean needNotification, RequestCallback callback) {
        File targetFile = DownloadedFileUtils.getLocalFile(fileName);
        String key = targetFile.getAbsolutePath();
        synchronized (taskLock) {
            DownloadTask task = taskMap.get(key);
            if (task != null) {
                task.addRequester(new Requester(fileName, needNotification, callback));
                if (needNotification) {
                    task.ensureNotification();
                }
                return;
            }
            // openFile 场景：本地已有完整文件，直接返回成功，不发起任何网络请求
            if (!needNotification && targetFile.exists() && targetFile.length() > 0) {
                final String savedPath = targetFile.getAbsolutePath();
                mainHandler.post(() -> callback.onFinished(true, savedPath, ""));
                return;
            }
            task = new DownloadTask(key, fileUrl, targetFile);
            task.addRequester(new Requester(fileName, needNotification, callback));
            taskMap.put(key, task);
            task.start();
        }
    }

    /**
     * 单个发起方
     */
    private static class Requester {
        final String fileName;
        final boolean needNotification;
        final RequestCallback callback;

        Requester(String fileName, boolean needNotification, RequestCallback callback) {
            this.fileName = fileName;
            this.needNotification = needNotification;
            this.callback = callback;
        }
    }

    /**
     * 下载任务，按目标文件路径合并（同一文件不重复下载）
     */
    private class DownloadTask {
        private final String key;
        private final String fileUrl;
        private final File targetFile;
        private final File tmpFile;
        private final List<Requester> requesters = new ArrayList<>();

        private boolean finished;
        private long lastProgressTime;
        private long lastNotificationTime;

        DownloadTask(String key, String fileUrl, File targetFile) {
            this.key = key;
            this.fileUrl = fileUrl;
            this.targetFile = targetFile;
            this.tmpFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        }

        synchronized void addRequester(Requester requester) {
            requesters.add(requester);
        }

        synchronized boolean hasNotificationRequester() {
            for (Requester requester : requesters) {
                if (requester.needNotification) {
                    return true;
                }
            }
            return false;
        }

        synchronized String getPrimaryFileName() {
            for (Requester requester : requesters) {
                if (requester.needNotification) {
                    return requester.fileName;
                }
            }
            return requesters.isEmpty() ? "" : requesters.get(0).fileName;
        }

        synchronized List<Requester> snapshotRequesters() {
            return new ArrayList<>(requesters);
        }

        /**
         * 静默任务被 downloadFile 复用时，补一个进度通知
         */
        void ensureNotification() {
            if (hasNotificationRequester()) {
                notificationHelper.showProgress(getPrimaryFileName(), -1);
            }
        }

        void start() {
            String normalizedUrl = DownloadedFileUtils.normalizeUrl(fileUrl);
            if (TextUtils.isEmpty(normalizedUrl)) {
                finish(false, "下载地址无效");
                return;
            }
            Request request;
            try {
                Request.Builder builder = new Request.Builder().url(normalizedUrl);
                // WebView 的登录态以 Cookie 为准，这里做尽力而为的注入
                String cookie = CookieManager.getInstance().getCookie(normalizedUrl);
                if (!TextUtils.isEmpty(cookie)) {
                    builder.header("Cookie", cookie);
                }
                request = builder.get().build();
            } catch (Exception e) {
                finish(false, "下载地址无效");
                return;
            }
            if (hasNotificationRequester()) {
                notificationHelper.showProgress(getPrimaryFileName(), -1);
            }
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    finish(false, e instanceof SocketTimeoutException ? "连接超时" : "网络异常，下载失败");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    handleResponse(response);
                }
            });
        }

        private void handleResponse(Response response) {
            ResponseBody body = response.body();
            try {
                int code = response.code();
                if (code == 401 || code == 403) {
                    finish(false, "登录已失效或无权访问");
                    return;
                }
                if (code == 404) {
                    finish(false, "文件不存在(404)");
                    return;
                }
                if (code < 200 || code >= 300 || body == null) {
                    finish(false, "下载失败(" + code + ")");
                    return;
                }
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    finish(false, "文件保存失败");
                    return;
                }
                if (tmpFile.exists() && !tmpFile.delete()) {
                    finish(false, "文件保存失败");
                    return;
                }
                long totalBytes = body.contentLength();
                long currentBytes = 0;
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = body.byteStream();
                    outputStream = new FileOutputStream(tmpFile);
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                        currentBytes += length;
                        notifyProgress(currentBytes, totalBytes, false);
                    }
                    outputStream.flush();
                    outputStream.getFD().sync();
                    outputStream.close();
                    outputStream = null;
                    inputStream.close();
                    inputStream = null;
                    if (!replaceTargetFile()) {
                        finish(false, "文件保存失败");
                        return;
                    }
                    notifyProgress(currentBytes, totalBytes, true);
                    finish(true, "");
                } catch (IOException e) {
                    String message = e.getMessage();
                    if (message != null && message.contains("ENOSPC")) {
                        finish(false, "存储空间不足");
                    } else {
                        finish(false, "网络异常，下载失败");
                    }
                } finally {
                    closeQuietly(inputStream);
                    closeQuietly(outputStream);
                }
            } finally {
                if (body != null) {
                    body.close();
                }
            }
        }

        /**
         * 删除旧文件并把 .tmp 重命名为正式文件，保证正式目录中只有完整文件
         */
        private boolean replaceTargetFile() {
            if (targetFile.exists() && !targetFile.delete()) {
                return false;
            }
            if (tmpFile.renameTo(targetFile)) {
                return true;
            }
            // 个别机型 renameTo 失败时退化为复制
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(tmpFile);
                outputStream = new FileOutputStream(targetFile);
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.getFD().sync();
                return tmpFile.delete();
            } catch (IOException e) {
                return false;
            } finally {
                closeQuietly(inputStream);
                closeQuietly(outputStream);
            }
        }

        /**
         * 进度节流：单任务 200ms 一次，并发时全局 80ms 一次；完成前强制补一次 100
         */
        private void notifyProgress(long currentBytes, long totalBytes, boolean finalProgress) {
            int percent;
            if (finalProgress) {
                percent = 100;
            } else if (totalBytes > 0) {
                percent = (int) (currentBytes * 100 / totalBytes);
                if (percent > 100) {
                    percent = 100;
                }
            } else {
                percent = -1;
            }
            long now = SystemClock.elapsedRealtime();
            if (!finalProgress) {
                if (lastProgressTime != 0 && now - lastProgressTime < PROGRESS_INTERVAL) {
                    return;
                }
                long lastGlobal = lastGlobalProgressTime.get();
                if (now - lastGlobal < GLOBAL_PROGRESS_INTERVAL) {
                    return;
                }
                lastGlobalProgressTime.set(now);
                lastProgressTime = now;
            }
            if (!hasNotificationRequester()) {
                return;
            }
            if (finalProgress || now - lastNotificationTime >= NOTIFICATION_INTERVAL) {
                lastNotificationTime = now;
                notificationHelper.showProgress(getPrimaryFileName(), percent);
            }
            Set<String> deliveredNames = new HashSet<>();
            for (Requester requester : snapshotRequesters()) {
                if (requester.needNotification && deliveredNames.add(requester.fileName)) {
                    requester.callback.onProgress(requester.fileName, percent, currentBytes, totalBytes);
                }
            }
        }

        /**
         * 任务结束：保证只结束一次，并逐个发起方回调
         * 下载终态同一 fileName 只回调一次；openFile 的发起方每次都回调
         */
        private void finish(boolean success, String msg) {
            synchronized (taskLock) {
                if (finished) {
                    return;
                }
                finished = true;
                taskMap.remove(key);
            }
            if (!success) {
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                notificationHelper.cancel(getPrimaryFileName());
            } else if (hasNotificationRequester()) {
                notificationHelper.showCompleted(getPrimaryFileName(), targetFile);
            }
            String savedPath = success ? targetFile.getAbsolutePath() : "";
            Set<String> deliveredDownloadNames = new HashSet<>();
            for (Requester requester : snapshotRequesters()) {
                if (requester.needNotification) {
                    if (deliveredDownloadNames.add(requester.fileName)) {
                        requester.callback.onFinished(success, savedPath, msg);
                    }
                } else {
                    requester.callback.onFinished(success, savedPath, msg);
                }
            }
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}