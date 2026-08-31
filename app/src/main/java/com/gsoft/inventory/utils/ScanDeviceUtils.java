package com.gsoft.inventory.utils;


import android.os.Build;

/**
 * 扫码设备类型检测
 * @author 10904
 */
public class ScanDeviceUtils {

    /**
     * 判断是否为新型设备（采用广播扫码方案）
     */
    public static boolean isNewDevice() {
        String model = Build.MODEL;
        return "CRUISE GeP".equalsIgnoreCase(model);
    }

    /**
     * 判断是否为旧型设备（采用RSC-JA硬件扫码方案）
     */
    public static boolean isOldDevice() {
        return !isNewDevice();
    }
}
