package com.gsoft.inventory.utils;

import android.app.*;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.gsoft.inventory.NetChooseActivity;
import com.gsoft.inventory.const_data.ReqPageCodeConst;
import com.gsoft.inventory.entities.BadgeNotificationPojo;
import com.gsoft.inventory.service.login.SyncGetBadgeAndNotification;
import java.util.List;
import me.leolin.shortcutbadger.ShortcutBadger;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.POST_NOTIFICATIONS;

/**
 * @author 10904
 */
public class BadgeNotificationManager {
    private Context mContext;

    private Handler handler;
    public BadgeNotificationManager(Context context) {
        this.mContext = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    // 申请通知栏权限以及桌面角标权限
    public void requestNotificationAndBadgePermissions() {
        // 1. 申请通知栏权限（Android 13及以上）
        if (Build.VERSION.SDK_INT >= 33) {
            if (!NotificationUtils.checkNotificationPermission(mContext)) {
                ActivityCompat.requestPermissions(
                        (Activity) mContext,
                        new String[]{POST_NOTIFICATIONS},
                        ReqPageCodeConst.NOTICE_BADGE_CHOOSER_REQUEST_CODE
                );
            }
        }

        // 2. 根据厂商引导开启桌面角标权限
        handleManufacturerBadgePermission();
    }


    /**
     * 获取徽章和通知信息
     */
    public void getBadgeAndNotification() {
        SyncGetBadgeAndNotification syncTask = new SyncGetBadgeAndNotification(mContext);
        AsyncManager.getInstance().submitWithCallback(syncTask, result -> {
            this.handler.post(() -> {
                if (result.isSuccess()) {
                    List<BadgeNotificationPojo> data = result.getData();
                    if (data != null && !data.isEmpty()) {
                        BadgeNotificationPojo badgeNotificationPojo = data.get(data.size()-1);
                        String badgeCount = badgeNotificationPojo.getBadgeCount();
                        if(StringUtils.isNullOrEmpty(badgeCount)){
                            badgeCount = "0";
                        }
                        String noticeContent = badgeNotificationPojo.getNoticeContent();
                        sendNotice(noticeContent, Integer.parseInt(badgeCount), "from_app");
                    }
                }
            });
        });
    }

    /**
     * 根据不同厂商处理桌面角标权限引导
     */
    private void handleManufacturerBadgePermission() {
        // 检查是否已拥有角标权限，已拥有则不显示引导
        if (NotificationUtils.hasBadgePermission(mContext)) {
            return;
        }

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String[] guideInfo = getBadgePermissionGuideInfo(manufacturer);
        if (guideInfo != null) {
            showPermissionGuideDialog(guideInfo[0], guideInfo[1]);
        }
    }

    /**
     * 获取不同厂商的角标权限引导信息
     */
    private String[] getBadgePermissionGuideInfo(String manufacturer) {
        if (manufacturer.contains("xiaomi")) {
            return new String[]{"小米设备角标权限", "为了正常显示桌面角标，请前往设置 > 应用管理 > 本应用 > 通知 > 开启「显示桌面图标角标」"};
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return new String[]{"华为/荣耀设备角标权限", "为了正常显示桌面角标，请前往设置 > 应用 > 应用管理 > 本应用 > 通知 > 开启「桌面图标角标」"};
        } else if (manufacturer.contains("vivo")) {
            return new String[]{"vivo设备角标权限", "为了正常显示桌面角标，请前往设置 > 通知与状态栏 > 应用通知管理 > 本应用 > 开启「桌面角标」"};
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            return new String[]{"OPPO/Realme设备角标权限", "为了正常显示桌面角标，请前往设置 > 通知与状态栏 > 应用通知管理 > 本应用 > 开启「桌面角标」"};
        }
        return null;
    }

    /**
     * 显示权限引导对话框
     */
    private void showPermissionGuideDialog(String title, String message) {
        new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("去设置", (dialog, which) -> startAppSettingsActivity())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 启动应用设置页面
     */
    private void startAppSettingsActivity() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 发送通知
     */
    public void sendNotice(String noticeContent, int badgeCount,  String source) {
        if(badgeCount == 0){
            return;
        }

        // 启动角标通知
        String channelId = "com.gsoft.inventory";

        // 1. 权限检查（使用工具类）
        if (Build.VERSION.SDK_INT >= 33 && !NotificationUtils.checkNotificationPermission(mContext)) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{POST_NOTIFICATIONS}, 1001);
            return;
        }

        // 2. 创建通知工具类实例（针对小米设置重要性）
        NotificationUtils notificationHelper = new NotificationUtils(mContext, channelId, "角标通知");
        if (notificationHelper.isMiuiSystem()) {
            // 小米系统特殊处理
            notificationHelper.setImportance(NotificationManager.IMPORTANCE_HIGH);
        }

        // 3. 构建通知点击意图
        Intent intent = new Intent(mContext, NetChooseActivity.class);
        intent.putExtra("notification_source", source); // 标记来源
        intent.putExtra("notice_content", noticeContent);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        // 4. 构建并发送通知
        NotificationCompat.Builder builder = notificationHelper.buildNotification(
                "待办通知",
                noticeContent,
                pendingIntent,
                badgeCount
        );
        // 设置点击后自动清除通知
        builder.setAutoCancel(true);
        // 小米系统角标特殊处理：设置通知的badgeCount字段
        Notification notice = builder.build();
        notificationHelper.showNotification(1, notice);
        // 小米系统特殊处理
        ShortcutBadger.applyNotification(mContext, notice, badgeCount);
        // 其它系统设置角标
        ShortcutBadger.applyCount(mContext, badgeCount);
        // Vivo系统角标特殊处理
        if (notificationHelper.isVivoSystem()) {
            try {
                Intent intentVivo = new Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
                intentVivo.putExtra("packageName", mContext.getPackageName());
                String className = mContext.getClass().getName();
                intentVivo.putExtra("className", className);
                intentVivo.putExtra("notificationNum", badgeCount);
                mContext.sendBroadcast(intentVivo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 更新桌面角标数量，不发送通知
     * @param badgeCount 角标数量
     */
    public void updateBadgeCount(int badgeCount) {
        // 权限检查
        if (Build.VERSION.SDK_INT >= 33 && !NotificationUtils.checkNotificationPermission(mContext)) {
            // 如果没有通知权限，则不尝试更新角标
            return;
        }

        // 使用 ShortcutBadger 库更新角标
        ShortcutBadger.applyCount(mContext, badgeCount);

        // 小米系统特殊处理
        if (new NotificationUtils(mContext, "dummy", "dummy").isMiuiSystem()) {
            try {
                // 小米需要通过发送一个空通知来更新角标
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new NotificationCompat.Builder(mContext, "dummy")
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // 使用系统默认图标
                        .setAutoCancel(true)
                        .build();
                ShortcutBadger.applyNotification(mContext, notification, badgeCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Vivo系统角标特殊处理
        if (new NotificationUtils(mContext, "dummy", "dummy").isVivoSystem()) {
            try {
                Intent intentVivo = new Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
                intentVivo.putExtra("packageName", mContext.getPackageName());
                String className = mContext.getClass().getName();
                intentVivo.putExtra("className", className);
                intentVivo.putExtra("notificationNum", badgeCount);
                mContext.sendBroadcast(intentVivo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
