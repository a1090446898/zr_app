package com.gsoft.inventory.service.scanner.broadcast;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import com.gsoft.inventory.service.scanner.IScannerReader;
import com.gsoft.inventory.service.scanner.ScanResult;
import com.gsoft.inventory.service.scanner.ScannerCallbacks;
import com.gsoft.inventory.service.scanner.ScannerError;
import com.gsoft.inventory.utils.ScanDeviceUtils;
import com.gsoft.inventory.utils.StringUtils;

/**
 * 东集(Seuic)新机型红外扫码适配器：系统扫描服务广播方案
 * <p>
 * 1. 物理扫描键出光，条码结果经com.android.server.scannerservice.broadcast广播带回；
 * 2. 不支持程序软触发，页面隐藏"开始/停止扫描"按钮；
 * 3. 页面onResume注册、onPause注销，避免后台重复响应。
 *
 * @author 10904
 */
public class SystemBroadcastScannerReader implements IScannerReader {

    private static final String TAG = "BroadcastScanner";
    /** 系统扫描服务广播 */
    private static final String SCAN_ACTION = "com.android.server.scannerservice.broadcast";
    /** 广播携带的扫码数据字段 */
    private static final String SCAN_DATA_KEY = "scannerdata";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ScannerCallbacks callbacks;
    private Activity activity;
    private BroadcastReceiver scanReceiver;
    private boolean receiverRegistered = false;

    @Override
    public String getVendorName() {
        return "东集(广播)";
    }

    @Override
    public boolean isSupported(Context context) {
        return ScanDeviceUtils.isNewDevice();
    }

    @Override
    public boolean supportsSoftwareTrigger() {
        return false;
    }

    @Override
    public void init(Activity activity, ScannerCallbacks callbacks) {
        this.activity = activity;
        this.callbacks = callbacks;
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !SCAN_ACTION.equals(intent.getAction())) {
                    return;
                }
                String code = intent.getStringExtra(SCAN_DATA_KEY);
                if (StringUtils.isNullOrEmpty(code)) {
                    return;
                }
                postResult(code);
            }
        };
        Log.i(TAG, "[DIAG] 初始化完成（系统广播方案）");
    }

    @Override
    public void onPageResume() {
        registerReceiver();
    }

    @Override
    public void onPagePause() {
        unregisterReceiver();
    }

    @Override
    public void release() {
        unregisterReceiver();
        scanReceiver = null;
        activity = null;
        callbacks = null;
    }

    @Override
    public void startScan() {
        // 广播方案不支持程序触发：由物理扫描键触发出光
    }

    @Override
    public void stopScan() {
        // 广播方案不支持程序停止：松开扫描键由系统自动停止
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /** 注册扫码广播（提高优先级，保证优先收到扫码结果） */
    private void registerReceiver() {
        if (activity == null || scanReceiver == null || receiverRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter(SCAN_ACTION);
            filter.setPriority(Integer.MAX_VALUE);
            activity.registerReceiver(scanReceiver, filter);
            receiverRegistered = true;
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 注册扫码广播失败", e);
        }
    }

    /** 注销扫码广播（未注册时忽略） */
    private void unregisterReceiver() {
        if (activity == null || scanReceiver == null || !receiverRegistered) {
            return;
        }
        try {
            activity.unregisterReceiver(scanReceiver);
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 注销扫码广播失败", e);
        }
        receiverRegistered = false;
    }

    /** 结果切回主线程回调页面 */
    private void postResult(final String rawValue) {
        mainHandler.post(() -> {
            if (callbacks != null) {
                callbacks.onScanResult(new ScanResult(rawValue, null, getVendorName()));
            }
        });
    }
}
