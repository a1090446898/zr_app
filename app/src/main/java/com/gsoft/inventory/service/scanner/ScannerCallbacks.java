package com.gsoft.inventory.service.scanner;

/**
 * 统一扫码回调：适配器保证全部回调投递到主线程
 *
 * @author 10904
 */
public interface ScannerCallbacks {

    /** 扫到条码（原始值，页面统一经BarcodeResultUtils清洗后使用） */
    void onScanResult(ScanResult result);

    /** 扫描状态变化（物理键触发/超时/停止时由适配器通知，页面同步按钮与状态文案） */
    void onScanStateChanged(boolean scanning, String message);

    /** 初始化/运行期错误（适配器已转换为统一语义与文案） */
    void onScannerError(ScannerError error);
}