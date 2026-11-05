package com.gsoft.inventory.service.scanner;

/**
 * 扫码结果载体：原始文本 + 原始字节（部分SDK带字节，用于特殊编码场景）+ 厂家标识
 *
 * @author 10904
 */
public class ScanResult {

    /** 扫码原始文本（未清洗，页面用BarcodeResultUtils统一解析） */
    public final String rawValue;
    /** 扫码原始字节（无则null） */
    public final byte[] rawBytes;
    /** 识别厂家名称（诊断用） */
    public final String vendor;

    public ScanResult(String rawValue, byte[] rawBytes, String vendor) {
        this.rawValue = rawValue;
        this.rawBytes = rawBytes;
        this.vendor = vendor;
    }
}