package com.gsoft.inventory.utils;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.gsoft.inventory.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.POST_NOTIFICATIONS;


public class NotificationUtils {

    private final Context context;
    private final NotificationManager notificationManager;
    private String channelId;
    private String channelName;
    // 默认支持声音震动
    private int importance = NotificationManager.IMPORTANCE_DEFAULT;

    public NotificationUtils(Context context, String channelId, String channelName) {
        this.context = context;
        this.channelId = channelId;
        this.channelName = channelName;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    // 创建通知渠道（Android O+）
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.enableVibration(false);
            channel.setShowBadge(true);
//            channel.setVibrationPattern(new long[]{100, 200, 300});
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 构建基础通知
    public NotificationCompat.Builder buildNotification(String title, String content, PendingIntent pendingIntent, int badgeCount) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getSmallIcon())
                .setContentTitle(title)
                .setContentText(content)
                .setNumber(badgeCount)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);
    }

    // 发送通知
    public void showNotification(int notificationId, Notification notification) {
        notificationManager.notify(notificationId, notification);

    }

    // 获取应用图标（避免资源找不到问题）
    private int getSmallIcon() {
        return ContextCompat.getDrawable(context, R.mipmap.logo) != null
                ? R.mipmap.logo
                : android.R.drawable.ic_notification_overlay;
    }

    // 设置渠道重要性（针对不同厂商调整）
    public void setImportance(int importance) {
        this.importance = importance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel != null) {
                channel.setImportance(importance);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 检查通知权限（已有方法）
     */
    public static boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return notificationManager.areNotificationsEnabled();
            } else {
                try {
                    Method method = notificationManager.getClass().getMethod("isNotificationEnabled");
                    return (boolean) method.invoke(notificationManager);
                } catch (Exception e) {
                    // 旧系统默认返回true
                    return true;
                }
            }
        }
    }


    /**
     * 检查小米设备角标权限
     */
    private static boolean checkMiuiBadgePermission(Context context) {
        try {
            // MIUI 角标权限检查（通过反射）
            Class<?> miuiNotificationClass = Class.forName("android.app.MiuiNotification");
            Object miuiNotification = miuiNotificationClass.newInstance();
            Field field = miuiNotificationClass.getField("messageCount");
            field.setAccessible(true);
            field.setInt(miuiNotification, 1);

            // 检查通知设置中的角标开关
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = info.metaData;
            return bundle == null || bundle.getBoolean("miui_badge_permission", true);
        } catch (Exception e) {
            // 备选方案：检查通知渠道设置
            return checkNotificationPermission(context);
        }
    }

    /**
     * 检查华为/荣耀设备角标权限
     */
    private static boolean checkHuaweiBadgePermission(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Method method = manager.getClass().getMethod("getNotificationPolicy");
            Object policy = method.invoke(manager);

            // 反射获取华为角标权限状态
            Class<?> policyClass = Class.forName("android.app.NotificationManager$NotificationPolicy");
            Field badgeField = policyClass.getField("allowBadge");
            return badgeField.getBoolean(policy);
        } catch (Exception e) {
            // 华为旧系统兼容
            return checkNotificationPermission(context);
        }
    }

    /**
     * 检查vivo设备角标权限
     */
    private static boolean checkVivoBadgePermission(Context context) {
        try {
            // Vivo专用权限检查Intent
            Intent intent = new Intent("launcher.action.CHECK_BADGE_PERMISSION");
            intent.putExtra("packageName", context.getPackageName());
            context.sendBroadcast(intent);

            // 检查vivo系统版本
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", String.class);
            String version = (String) method.invoke(null, "ro.vivo.os.version");

            // 新版本vivo系统直接检查通知权限
            return !TextUtils.isEmpty(version) && checkNotificationPermission(context);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查OPPO/Realme设备角标权限
     */
    private static boolean checkOppoBadgePermission(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(context.getPackageName(), 0);

            // OPPO反射检查角标权限
            Method method = pm.getClass().getMethod("hasBadgePermission", int.class);
            return (boolean) method.invoke(pm, info.uid);
        } catch (Exception e) {
            // OPPO旧系统兼容方案
            return checkNotificationPermission(context);
        }
    }

    /**
     * Android O及以上通用角标权限检查
     */
    private static boolean checkAndroidOBadgePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return manager.areNotificationsEnabled();
        }
        return true;
    }

    /**
     * 检查是否为MIUI系统（已有方法）
     */
    public boolean isMiuiSystem() {
        return Build.MANUFACTURER.toLowerCase().contains("xiaomi");
    }

    /**
     * 检查是否为VIVO系统（已有方法）
     */
    public boolean isVivoSystem() {
        return Build.MANUFACTURER.toLowerCase().contains("vivo");
    }

    /**
     * 检查是否拥有桌面角标权限（主方法）
     */
    public static boolean hasBadgePermission(Context context) {
        if (context == null) return false;

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        try {
            if (manufacturer.contains("xiaomi")) {
                return checkMiuiBadgePermission(context);
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                return checkHuaweiBadgePermission(context);
            } else if (manufacturer.contains("vivo")) {
                return checkVivoBadgePermission(context);
            } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
                return checkOppoBadgePermission(context);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return checkAndroidOBadgePermission(context);
            } else {
                // 其他设备默认返回true（或根据需求调整）
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
