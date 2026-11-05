package com.gsoft.inventory.service.scanner;

import android.content.Context;
import android.util.Log;

import com.gsoft.inventory.service.scanner.broadcast.SystemBroadcastScannerReader;
import com.gsoft.inventory.service.scanner.rsc.RscScannerReader;
import com.gsoft.inventory.service.scanner.urovo.UrovoScannerReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫码适配器注册表工厂：按注册顺序探测设备，返回第一个支持当前设备的适配器
 * <p>
 * 顺序说明：
 * 1. 优博讯：厂商名优先，扫描服务参数可读作为贴牌机兜底；
 * 2. 东集新机：CRUISE GeP等白名单，系统扫描广播方案；
 * 3. 东集旧机/未知设备：RSC-JA解码器（与旧版兜底逻辑一致）；
 * 4. 均不匹配时由UnsupportedScannerReader兜底
 * <p>
 * 注册表保存Class对象、实例化延迟到create()逐厂家容错：
 * 厂家SDK缺失或探测异常时该厂家自动跳过，不拖垮整个工厂与扫描页
 *
 * @author 10904
 */
public final class ScannerFactory {

    private static final String TAG = "ScannerFactory";

    /** 厂家适配器注册表（保存Class对象，工厂类初始化不触发厂家SDK类加载） */
    private static final List<Class<? extends IScannerReader>> REGISTRY = new ArrayList<>();

    static {
        // 类字面量只加载类定义、不执行类初始化；单厂家注册失败不影响其它厂家
//        try {
//            REGISTRY.add(UrovoScannerReader.class);
//        } catch (Throwable e) {
//            Log.w(TAG, "[DIAG] 优博讯适配器类不可用，跳过注册", e);
//        }
        try {
            REGISTRY.add(SystemBroadcastScannerReader.class);
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 系统广播适配器类不可用，跳过注册", e);
        }
        try {
            REGISTRY.add(RscScannerReader.class);
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 东集旧机适配器类不可用，跳过注册", e);
        }
    }

    private ScannerFactory() {
    }

    /** 探测并创建当前设备对应的扫码读取器（新增厂家适配器时只需在此注册一行） */
    public static IScannerReader create(Context context) {
        for (Class<? extends IScannerReader> clazz : REGISTRY) {
            try {
                IScannerReader reader = clazz.getDeclaredConstructor().newInstance();
                boolean supported = reader.isSupported(context);
                Log.i(TAG, "[DIAG] 探测 " + reader.getVendorName() + " isSupported=" + supported);
                if (supported) {
                    return reader;
                }
            } catch (Throwable e) {
                Log.w(TAG, "[DIAG] 适配器不可用，已跳过：" + clazz.getSimpleName(), e);
            }
        }
        Log.w(TAG, "[DIAG] 未匹配任何厂家，使用不支持兜底");
        return new UnsupportedScannerReader();
    }
}