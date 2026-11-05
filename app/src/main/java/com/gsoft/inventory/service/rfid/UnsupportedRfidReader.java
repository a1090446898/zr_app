package com.gsoft.inventory.service.rfid;

import android.content.Context;

/**
 * 兜底适配器：当前设备不支持RFID，所有操作均为空实现
 */
public class UnsupportedRfidReader implements IRfidReader {

    @Override
    public String getVendorName() {
        return "未知设备";
    }

    @Override
    public boolean isSupported(Context context) {
        return false;
    }

    @Override
    public boolean requiresStoragePermission() {
        return false;
    }

    @Override
    public boolean requiresLocationPermission() {
        return false;
    }

    @Override
    public boolean supportsTid() {
        return false;
    }

    @Override
    public int[] getPowerRange() {
        // 无硬件可设：与默认功率33对齐，避免设置对话框默认值超出校验范围
        return new int[]{0, 33};
    }

    @Override
    public int getDefaultPower() {
        // 非优博讯设备：默认功率33dBm
        return 33;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void connect(Context context, RfidCallbacks callbacks) {
        if (callbacks != null) {
            callbacks.onConnectResult(RfidError.DEVICE_NOT_SUPPORTED);
        }
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void startScan() {
    }

    @Override
    public void stopScan() {
    }

    @Override
    public void applyConfig(RfidConfig config) {
    }

    @Override
    public void onPageResume() {
    }

    @Override
    public void onPagePause() {
    }

    @Override
    public boolean isTriggerKey(int keyCode, int scanCode) {
        return false;
    }
}