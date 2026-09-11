package com.gsoft.inventory.service.rfid;

import android.content.Context;

import com.gsoft.inventory.service.rfid.desai.DesaiRfidReader;
import com.gsoft.inventory.service.rfid.dongji.DongjiRfidReader;

import java.util.ArrayList;
import java.util.List;

/**
 * RFID适配器注册表工厂：按注册顺序探测设备，返回第一个支持当前设备的适配器
 * <p>
 * 东集判断依赖Build.MANUFACTURER/Build.MODEL且SDK为系统包，探测最安全，放在最前；
 * 得实SDK被打进APK，其探测接口在陌生设备上也可能返回非空，作为次选
 */
public final class RfidReaderFactory {

    private static final List<IRfidReader> REGISTRY = new ArrayList<>();

    static {
        REGISTRY.add(new DongjiRfidReader());
        REGISTRY.add(new DesaiRfidReader());
    }

    private RfidReaderFactory() {
    }

    /** 探测并创建当前设备对应的RFID读取器（新增厂家适配器时只需在此注册一行） */
    public static IRfidReader create(Context context) {
        for (IRfidReader reader : REGISTRY) {
            if (reader.isSupported(context)) {
                return reader;
            }
        }
        return new UnsupportedRfidReader();
    }
}