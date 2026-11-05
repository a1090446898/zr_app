package com.gsoft.inventory.utils.ds_print;

import android.content.Context;
import android.graphics.Bitmap;

import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.print_router.PrintEffectRouter;

import java.util.HashMap;
import java.util.Map;

/**
 * 得实标签渲染入口（标签类型 → 版式模板 的路由注册表）
 * <p>
 * 独立于宝威的BwLabelRenderer：模板注册表、EPC生成、条码校验都在本类内闭环，
 * 后续调整得实标签不影响宝威，反之亦然。
 * <p>
 * EPC生成规则（与RfidReadActivity.epcHexToBarcode严格互逆）：
 * BARCODEID → ASCII字节 → 大写十六进制字符串。
 * 条码固定14位 → 28个hex字符，正好是4的倍数，满足得实SDK对HEX长度必须为4的倍数的校验，
 * 因此不需要补零。
 *
 * @author 10904
 */
public class DsLabelRenderer {

    /** 条码编号固定长度：14位（业务约定，打印与写EPC都以此为前提） */
    public static final int BARCODE_LENGTH = 14;

    /** 标签类型（打印效果名，与strings.xml的printe_effect一致） → 版式模板 */
    private static final Map<String, DsLabelTemplate> TEMPLATE_MAP = new HashMap<>();

    static {
        TEMPLATE_MAP.put(PrintEffectRouter.EFFECT_DS_RFID_QR_60_25, new DsRfidQr6025Template());
        TEMPLATE_MAP.put(PrintEffectRouter.EFFECT_DS_RFID_QR_70_25, new DsRfidQr7025Template());
    }

    private DsLabelRenderer() {
    }

    /** 是否存在该标签类型的得实模板 */
    public static boolean hasTemplate(String labelType) {
        return TEMPLATE_MAP.containsKey(labelType);
    }

    /**
     * 绘制标签位图：按标签类型（打印效果）选择模板渲染，预览与发送打印的为同一张
     *
     * @param labelType 标签类型（打印效果名）
     * @param assets    资产（构建二维码内容、读取打印字段值）
     * @param title     打印抬头
     * @param position  打印位置设置："居中"居中，其他值按居左
     */
    public static Bitmap renderLabel(Context context, String labelType, Assets assets,
                                     String title, String position) {
        DsLabelTemplate template = TEMPLATE_MAP.get(labelType);
        if (template == null) {
            return null;
        }
        return template.render(context, assets, title, position);
    }

    /** 条码是否为合法的14位可打印ASCII字符（EPC要求每个字符1字节） */
    public static boolean isValidBarcode(String barCodeId) {
        if (StringUtils.isNullOrEmpty(barCodeId)) {
            return false;
        }
        String value = barCodeId.trim();
        if (value.length() != BARCODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /** 条码长度校验失败的提示文案（含当前位数，便于用户直接定位问题） */
    public static String buildBarcodeError(String barCodeId) {
        int length = StringUtils.isNullOrEmpty(barCodeId) ? 0 : barCodeId.trim().length();
        return "条码编号长度不正确（当前" + length + "位，应为" + BARCODE_LENGTH + "位），请修改后重试";
    }

    /**
     * 生成待写入标签的EPC（大写十六进制字符串）
     * 条码为空返回null；非ASCII字符会导致长度不符，由isValidBarcode提前拦截
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