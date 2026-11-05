package com.gsoft.inventory.utils.print_router;

import java.util.HashMap;
import java.util.Map;

/**
 * 打印效果（打印类型）→ 打印机品牌 路由注册表
 * <p>
 * 背景：登录/设置页选择的"打印效果"决定标签版式，同时通过本注册表决定由哪台打印机打印。
 * 原有12种打印效果全部走老打印机（得实/LPAPI），未在表中登记即默认按老打印机处理，存量类型零改动。
 * 宝威RFID标签登记为BRAND_BW；得实新型号RFID标签登记为BRAND_DS（当前暂无标签，标签选择页"得实"Tab为空态）。
 * <p>
 * 后续新增打印类型的步骤：
 * 1. strings.xml 的 printe_effect 数组新增一项（标签选择页自动出现，无需改Java）；
 * 2. 在下方 EFFECT_BRAND_MAP 登记品牌；
 * 3. 宝威：新建版式模板（继承BwLabelTemplate）并在 BwLabelRenderer.TEMPLATE_MAP 注册；
 *    得实新型号：适配得实SDK的打印页/批量执行器，并在 PrintCompatActivity 品牌分发处加 BRAND_DS 分支；
 *    老打印机：在 PrintCompatActivity.printAssets 分发方法里加一个 else-if 分支。
 */
public class PrintEffectRouter {

    /** 宝威打印机打印类型名（RFID标签，60×25版式见BwWebQrDoubleTemplate，打印时写入EPC），必须与strings.xml中数组项文字完全一致 */
    public static final String EFFECT_BW_RFID_WEB_QR = "RFID二维码60*25";
    /** 宝威打印机打印类型名（RFID标签，70×25版式见BwRfidQr7025Template），必须与strings.xml中数组项文字完全一致 */
    public static final String EFFECT_BW_RFID_QR_70_25 = "RFID二维码70*25";

    /** 得实RFID打印机打印类型名（60×25版式见DsRfidQr6025Template），必须与strings.xml中数组项文字完全一致 */
    public static final String EFFECT_DS_RFID_QR_60_25 = "得实二维码60*25";
    /** 得实RFID打印机打印类型名（70×25版式见DsRfidQr7025Template），必须与strings.xml中数组项文字完全一致 */
    public static final String EFFECT_DS_RFID_QR_70_25 = "得实二维码70*25";


    /** 打印机品牌：老打印机（得实/LPAPI） */
    public static final String BRAND_DT = "DT";
    /** 打印机品牌：宝威（威手持，支持蓝牙/WiFi） */
    public static final String BRAND_BW = "BW";
    /** 打印机品牌：得实新型号（RFID打印，SDK与老LPAPI不同，待适配） */
    public static final String BRAND_DS = "DS";

    /** 打印效果名 → 打印机品牌；未登记的效果一律按老打印机（DT）处理 */
    private static final Map<String, String> EFFECT_BRAND_MAP = new HashMap<>();

    static {
        // 新增宝威打印类型在此登记
        EFFECT_BRAND_MAP.put(EFFECT_BW_RFID_WEB_QR, BRAND_BW);
        EFFECT_BRAND_MAP.put(EFFECT_BW_RFID_QR_70_25, BRAND_BW);
        // 得实RFID打印类型
        EFFECT_BRAND_MAP.put(EFFECT_DS_RFID_QR_60_25, BRAND_DS);
        EFFECT_BRAND_MAP.put(EFFECT_DS_RFID_QR_70_25, BRAND_DS);
    }

    private PrintEffectRouter() {
    }

    /** 查询打印效果对应的打印机品牌，未登记返回默认品牌（老得实/精臣） */
    public static String getBrand(String effectName) {
        String brand = EFFECT_BRAND_MAP.get(effectName);
        return brand == null ? BRAND_DT : brand;
    }

    /** 是否为宝威打印机类型 */
    public static boolean isBwEffect(String effectName) {
        return BRAND_BW.equals(EFFECT_BRAND_MAP.get(effectName));
    }

    /** 是否为得实RFID打印机类型 */
    public static boolean isDsEffect(String effectName) {
        return BRAND_DS.equals(EFFECT_BRAND_MAP.get(effectName));
    }
}