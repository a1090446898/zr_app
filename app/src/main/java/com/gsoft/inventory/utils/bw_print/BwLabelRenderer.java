package com.gsoft.inventory.utils.bw_print;

import android.content.Context;
import android.graphics.Bitmap;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.print_router.PrintEffectRouter;

import java.util.HashMap;
import java.util.Map;

/**
 * 宝威标签渲染入口（标签类型 → 版式模板 的路由注册表）
 * <p>
 * 与PrintEffectRouter同思路：登录/设置页选择的"打印效果(打印类型)"字符串即标签类型，
 * 在这里映射到对应版式模板（BwLabelTemplate子类）；BwPrintActivity只调用本类，不感知具体版式。
 * <p>
 * 后续新增宝威标签类型的步骤：
 * 1. strings.xml的printe_effect数组新增一项（标签选择页"宝威"Tab自动出现，无需改Java）；
 * 2. 在PrintEffectRouter.EFFECT_BRAND_MAP登记为BRAND_BW；
 * 3. 新建模板类（继承BwLabelTemplate），并在下方TEMPLATE_MAP注册。
 * <p>
 * EPC生成规则（与RfidReadActivity.epcHexToBarcode严格互逆，保证打印出的标签能被现有RFID盘点逻辑读回转成条码）：
 * BARCODEID → ASCII字节 → 大写十六进制字符串（例："1211103000308" → "31323131313033303030333038"）。
 */
public class BwLabelRenderer {

    /** 标签类型（打印效果名，与strings.xml的printe_effect一致） → 版式模板；新增标签类型在此登记 */
    private static final Map<String, BwLabelTemplate> TEMPLATE_MAP = new HashMap<>();

    static {
        // "RFID移动端网页二维码"：网页二维码-二字段版式（60×25mm）
        TEMPLATE_MAP.put(PrintEffectRouter.EFFECT_BW_RFID_WEB_QR, new BwRfidQr6025Template());
        // "RFID二维码70*25"：沿用二字段版式，纸张加宽到70×25mm
        TEMPLATE_MAP.put(PrintEffectRouter.EFFECT_BW_RFID_QR_70_25, new BwRfidQr7025Template());
    }

    private BwLabelRenderer() {
    }

    /** 是否存在该标签类型的模板（打印页用于提示"打印类型暂未适配"） */
    public static boolean hasTemplate(String labelType) {
        return TEMPLATE_MAP.containsKey(labelType);
    }

    /**
     * 绘制标签位图：按标签类型（打印效果）选择模板渲染，预览与发送打印的为同一张位图
     *
     * @param labelType 标签类型（打印效果名，与strings.xml的printe_effect一致）
     * @param assets    资产（构建二维码内容、读取打印字段值）
     * @param title     打印抬头（调用方已按"打印抬头"设置计算好）
     * @param position  打印位置设置："居中"居中，其他值（含默认"打印位置"）按居左
     */
    public static Bitmap renderLabel(Context context, String labelType, Assets assets, String title, String position) {
        BwLabelTemplate template = TEMPLATE_MAP.get(labelType);
        if (template == null) {
            return null;
        }
        return template.render(context, assets, title, position);
    }

    /**
     * 生成待写入标签的EPC（十六进制字符串；SDK要求0-9/A-F，位数不足4的倍数时自动补0）
     * 与小写/大写无关，这里统一大写；条码为空时返回null
     */
    public static String buildEpcHex(String barCodeId) {
        if (StringUtils.isNullOrEmpty(barCodeId)) {
            return null;
        }
        try {
            byte[] bytes = barCodeId.trim().getBytes("US-ASCII");
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02X", b & 0xFF));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}