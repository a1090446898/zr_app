package com.gsoft.inventory.service.scanner;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;

/**
 * 兜底适配器：当前设备不支持红外扫码，初始化时直接回调不支持错误
 *
 * @author 10904
 */
public class UnsupportedScannerReader implements IScannerReader {

    @Override
    public String getVendorName() {
        return "未知设备";
    }

    @Override
    public boolean isSupported(Context context) {
        return false;
    }

    @Override
    public boolean supportsSoftwareTrigger() {
        return false;
    }

    @Override
    public void init(Activity activity, ScannerCallbacks callbacks) {
        if (callbacks != null) {
            callbacks.onScannerError(ScannerError.NOT_SUPPORTED);
        }
    }

    @Override
    public void onPageResume() {
    }

    @Override
    public void onPagePause() {
    }

    @Override
    public void release() {
    }

    @Override
    public void startScan() {
    }

    @Override
    public void stopScan() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }
}