package com.gsoft.inventory.service.download;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.down.DownloadedFileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 附件下载通知：进度通知与完成通知（点击完成通知可打开文件）
 *
 * @author 10904
 */
public class DownloadNotificationHelper {

    private static final String CHANNEL_PROGRESS = "zr_file_download_progress";
    private static final String CHANNEL_PROGRESS_NAME = "附件下载进度";
    private static final String CHANNEL_COMPLETE = "zr_file_download_complete";
    private static final String CHANNEL_COMPLETE_NAME = "附件下载完成";

    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_FILE_MIME = "extra_file_mime";
    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private final Context context;
    private final NotificationManager notificationManager;
    private final Map<String, Integer> notificationIds = new HashMap<>();
    private int nextNotificationId = 20100;

    public DownloadNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel(CHANNEL_PROGRESS,
                    CHANNEL_PROGRESS_NAME, NotificationManager.IMPORTANCE_LOW);
            progressChannel.enableVibration(false);
            progressChannel.setShowBadge(false);
            NotificationChannel completeChannel = new NotificationChannel(CHANNEL_COMPLETE,
                    CHANNEL_COMPLETE_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            completeChannel.enableVibration(true);
            completeChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(progressChannel);
            notificationManager.createNotificationChannel(completeChannel);
        }
    }

    /**
     * 展示下载进度，percent 为 -1 时使用不确定进度
     */
    public void showProgress(String fileName, int percent) {
        int notificationId = getNotificationId(fileName);
        boolean indeterminate = percent < 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PROGRESS)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(DownloadedFileUtils.getDisplayName(fileName))
                .setContentText(indeterminate ? "正在下载..." : "正在下载 " + percent + "%")
                .setProgress(100, Math.max(percent, 0), indeterminate)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * 展示下载完成通知，点击后调用外部应用打开
     */
    public void showCompleted(String fileName, File file) {
        int notificationId = getNotificationId(fileName);
        Intent intent = new Intent(context, OpenDownloadedFileReceiver.class);
        intent.setAction(OpenDownloadedFileReceiver.ACTION_OPEN_DOWNLOADED_FILE);
        intent.putExtra(EXTRA_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(EXTRA_FILE_MIME, DownloadedFileUtils.getMimeType(fileName));
        intent.putExtra(EXTRA_FILE_NAME, DownloadedFileUtils.getDisplayName(fileName));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_COMPLETE)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(DownloadedFileUtils.getDisplayName(fileName))
                .setContentText("下载完成，点击打开")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(notificationId, builder.build());
    }

    public void cancel(String fileName) {
        notificationManager.cancel(getNotificationId(fileName));
    }

    private synchronized int getNotificationId(String fileName) {
        Integer notificationId = notificationIds.get(fileName);
        if (notificationId == null) {
            notificationId = nextNotificationId++;
            notificationIds.put(fileName, notificationId);
        }
        return notificationId;
    }
}