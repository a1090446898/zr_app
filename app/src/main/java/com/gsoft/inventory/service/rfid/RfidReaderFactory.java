package com.gsoft.inventory.service.rfid;

import android.content.Context;
import android.util.Log;

import com.gsoft.inventory.service.rfid.dongji.DongjiRfidReader;
import com.gsoft.inventory.service.rfid.urovo.UrovoRfidReader;

import java.util.ArrayList;
import java.util.List;

/**
 * RFID适配器注册表工厂：按注册顺序探测设备，返回第一个支持当前设备的适配器
 * <p>
 * 东集判断依赖Build.MANUFACTURER/Build.MODEL且SDK为系统包，探测最安全，放在最前；
 * 优博讯按厂商串/机型名单探测；均不匹配时由UnsupportedRfidReader兜底
 * （得实适配已移除：其RFIDModuleS005内嵌URFIDLibrary与v2.5.1230重复类冲突）
 * <p>
 * 注册表保存Class对象、实例化延迟到create()逐厂家容错：厂家SDK缺失（如非东集设备
 * 无系统包com.seuic.uhf）时该厂家自动跳过，不再拖垮整个工厂与扫描页
 */
public final class RfidReaderFactory {

    private static final String TAG = "RfidReaderFactory";

    /** 厂家适配器注册表（保存Class对象，工厂类初始化不触发厂家SDK类加载） */
    private static final List<Class<? extends IRfidReader>> REGISTRY = new ArrayList<>();

    static {
        // 类字面量只加载类定义、不执行类初始化；单厂家注册失败不影响其它厂家
        try {
            REGISTRY.add(DongjiRfidReader.class);
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 东集适配器类不可用，跳过注册", e);
        }
        try {
            REGISTRY.add(UrovoRfidReader.class);
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 优博讯适配器类不可用，跳过注册", e);
        }
    }

    private RfidReaderFactory() {
    }

    /** 探测并创建当前设备对应的RFID读取器（新增厂家适配器时只需在此注册一行） */
    public static IRfidReader create(Context context) {
        for (Class<? extends IRfidReader> clazz : REGISTRY) {
            try {
                IRfidReader reader = clazz.getDeclaredConstructor().newInstance();
                boolean supported = reader.isSupported(context);
                Log.i(TAG, "[DIAG] 探测 " + reader.getVendorName() + " isSupported=" + supported);
                if (supported) {
                    return reader;
                }
            } catch (Throwable e) {
                // 厂家SDK缺失（如非东集设备无系统包com.seuic.uhf）或实例化/探测异常：跳过该厂家，不阻断其它适配器
                Log.w(TAG, "[DIAG] 厂家适配器不可用，已跳过：" + clazz.getSimpleName(), e);
            }
        }
        Log.w(TAG, "[DIAG] 未匹配任何厂家，使用不支持兜底");
        return new UnsupportedRfidReader();
    }
}