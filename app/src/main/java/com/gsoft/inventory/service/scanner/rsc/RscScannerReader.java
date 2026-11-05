package com.gsoft.inventory.service.scanner.rsc;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import com.gsoft.inventory.service.scanner.IScannerReader;
import com.gsoft.inventory.service.scanner.ScanResult;
import com.gsoft.inventory.service.scanner.ScannerCallbacks;
import com.gsoft.inventory.service.scanner.ScannerError;
import com.gsoft.inventory.utils.ScanDeviceUtils;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;

/**
 * 东集(Seuic)旧机型红外扫码适配器：RSC-JA硬件解码器方案
 * <p>
 * 1. 页面初始化时打开解码器并注册解码回调；
 * 2. 设备扫描键键码固定139：按下开始、松开停止，同时支持页面按钮软触发；
 * 3. 其余未知机型沿用旧版兜底走本适配器，保证既有设备行为不变；
 * 4. 不修改设备提示音/连续扫描间隔等全局设置，提示音统一由页面播放。
 *
 * @author 10904
 */
public class RscScannerReader implements IScannerReader {

    private static final String TAG = "RscScanner";

    /** 东集旧机型扫描键键码 */
    private static final int TRIGGER_KEY_CODE = 139;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BarcodeDecoder barcodeDecoder;
    private ScannerCallbacks callbacks;
    /** 解码器是否已打开（保证release时安全关闭） */
    private boolean opened = false;

    @Override
    public String getVendorName() {
        return "东集(RSC)";
    }

    @Override
    public boolean isSupported(Context context) {
        // 东集新机走系统广播、优博讯走内置扫描服务；其余未知机型沿用旧版兜底走RSC
        return !ScanDeviceUtils.isNewDevice() && !ScanDeviceUtils.isUrovoDevice();
    }

    @Override
    public boolean supportsSoftwareTrigger() {
        return true;
    }

    @Override
    public void init(Activity activity, ScannerCallbacks callbacks) {
        this.callbacks = callbacks;
        try {
            barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();
            barcodeDecoder.open(activity);
            opened = true;
            // 解码回调：成功后将条码原文与原始字节切回主线程交给页面
            barcodeDecoder.setDecodeCallback(entity -> {
                if (entity == null || entity.getResultCode() != BarcodeDecoder.DECODE_SUCCESS) {
                    return;
                }
                final String data = entity.getBarcodeData();
                final byte[] bytes = entity.getBarcodeBytesData();
                mainHandler.post(() -> {
                    if (RscScannerReader.this.callbacks != null) {
                        RscScannerReader.this.callbacks.onScanResult(
                                new ScanResult(data, bytes, getVendorName()));
                    }
                });
            });
            Log.i(TAG, "[DIAG] 初始化完成（RSC-JA方案）");
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 初始化失败", e);
            notifyError(ScannerError.INIT_FAILED);
        }
    }

    @Override
    public void onPageResume() {
        // 解码器常开，页面恢复无需额外处理
    }

    @Override
    public void onPagePause() {
        // 页面暂停由页面先stopScan，解码器无需关闭
    }

    @Override
    public void release() {
        if (barcodeDecoder != null && opened) {
            try {
                barcodeDecoder.close();
            } catch (Throwable e) {
                Log.e(TAG, "[DIAG] 关闭解码器失败", e);
            }
        }
        opened = false;
        barcodeDecoder = null;
        callbacks = null;
    }

    @Override
    public void startScan() {
        if (barcodeDecoder == null) {
            notifyError(ScannerError.INIT_FAILED);
            return;
        }
        try {
            barcodeDecoder.startScan();
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 启动扫描失败", e);
            notifyError(ScannerError.START_FAILED);
        }
    }

    @Override
    public void stopScan() {
        if (barcodeDecoder == null) {
            return;
        }
        try {
            barcodeDecoder.stopScan();
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 停止扫描失败", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == TRIGGER_KEY_CODE && event.getRepeatCount() == 0) {
            startScan();
            notifyScanState(true, "正在扫描…（" + getVendorName() + "）");
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == TRIGGER_KEY_CODE) {
            stopScan();
            notifyScanState(false, null);
            return true;
        }
        return false;
    }

    /** 扫描状态切回主线程回调页面（物理键触发时同步按钮与状态文案） */
    private void notifyScanState(final boolean scanning, final String message) {
        mainHandler.post(() -> {
            if (callbacks != null) {
                callbacks.onScanStateChanged(scanning, message);
            }
        });
    }

    /** 错误切回主线程回调页面 */
    private void notifyError(final ScannerError error) {
        mainHandler.post(() -> {
            if (callbacks != null) {
                callbacks.onScannerError(error);
            }
        });
    }
}