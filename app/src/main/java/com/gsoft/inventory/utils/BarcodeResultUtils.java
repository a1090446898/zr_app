package com.gsoft.inventory.utils;

/**
 * 扫码结果解析工具：解析扫码得到的条码原文（红外/摄像头等识别源通用）
 * <p>
 * 与项目原扫码页解析口径一致：
 * 1. HTTP链接二维码：截取"BARCODE"/"BID"标记后的内容
 * 2. 含"条码编号："的长文本二维码：取该行冒号后的条码
 * 3. 最终统一去除换行符与首尾空格
 */
public final class BarcodeResultUtils {

    private BarcodeResultUtils() {
    }

    /**
     * 解析扫码原文为条码值（无法解析时返回空串）
     */
    public static String parse(String raw) {
        if (StringUtils.isNullOrEmpty(raw)) {
            return "";
        }
        String barCode = raw.trim();
        try {
            if (barCode.toUpperCase().startsWith("HTTP")) {
                if (barCode.toUpperCase().contains("BARCODE")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BARCODE") + 8);
                } else if (barCode.toUpperCase().contains("BID")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BID") + 4);
                }
            } else if (barCode.length() > 25 && barCode.contains("条码编号：")) {
                barCode = barCode.substring(5, barCode.indexOf("\n"));
            }
        } catch (Exception e) {
            return "";
        }
        return barCode.replace("\n", "").replace("\r", "").trim();
    }
}