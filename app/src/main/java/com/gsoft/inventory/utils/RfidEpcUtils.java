package com.gsoft.inventory.utils;

/**
 * RFID EPC 编解码工具
 * <p>
 * 与标签打印侧 buildEpcHex 严格互逆：EPC十六进制 → ASCII条码。
 * RFID盘点页与H5扫码页共用同一套判断口径，避免两处实现逐渐不一致。
 */
public final class RfidEpcUtils {

    private RfidEpcUtils() {
    }

    /**
     * 判断EPC编码并转码为条码(BARCODEID)：十六进制 → ASCII
     * <p>
     * 按字节转码后若全部为可打印ASCII字符(0x20-0x7E)，判定为ASCII编码并返回转码结果
     * （读回的EPC可能带PC字头，前4位hex仅当总长超32位时去除，转码后去除尾部零填充）；
     * 不是ASCII编码(二进制EPC、非法hex、全零等)时返回null，由调用方用原始EPC直接查询
     */
    public static String epcHexToBarcode(String epcHex) {
        try {
            if (StringUtils.isNullOrEmpty(epcHex)) {
                return null;
            }
            String hex = epcHex.trim();
            if (hex.length() % 2 != 0 || hex.length() < 2) {
                return null;
            }
            if (hex.length() > 32) {
                hex = hex.substring(4);
            }
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            int len = bytes.length;
            while (len > 0 && bytes[len - 1] == 0) {
                len--;
            }
            if (len == 0) {
                return null;
            }
            String barCode = new String(bytes, 0, len).trim();
            return barCode.matches("[\\x20-\\x7E]+") ? barCode : null;
        } catch (Exception e) {
            return null;
        }
    }
}