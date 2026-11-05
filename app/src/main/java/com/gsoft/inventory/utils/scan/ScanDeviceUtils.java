package com.gsoft.inventory.utils.scan;


import android.os.Build;

import java.util.Locale;

/**
 * 扫码设备类型检测
 * @author 10904
 */
public class ScanDeviceUtils {

    /** 优博讯厂商标识（MANUFACTURER/BRAND，统一转大写比较；中文标识转大写后保持不变） */
    private static final String[] UROVO_VENDOR_FLAGS = {"UROVO", "UBX", "优博讯"};
    /** 东集厂商标识（用于排除东集机型被误判为优博讯） */
    private static final String[] SEUIC_VENDOR_FLAGS = {"SEUIC", "RSC", "东集"};

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

    /**
     * 判断是否为优博讯(Urovo)设备：红外扫描走内置扫描服务(android.device.ScanManager)
     */
    public static boolean isUrovoDevice() {
        return matchesVendor(UROVO_VENDOR_FLAGS);
    }

    /**
     * 判断是否为东集(Seuic)设备：新机型走系统广播、旧机型走RSC-JA解码器
     */
    public static boolean isSeuicDevice() {
        return matchesVendor(SEUIC_VENDOR_FLAGS);
    }

    /** 厂商名/品牌名是否命中指定标识集合 */
    private static boolean matchesVendor(String[] flags) {
        String vendor = (String.valueOf(Build.MANUFACTURER) + " " + String.valueOf(Build.BRAND))
                .toUpperCase(Locale.ROOT);
        for (String flag : flags) {
            if (vendor.contains(flag)) {
                return true;
            }
        }
        return false;
    }
}