package com.gsoft.inventory.service.scanner.urovo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
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
 * 优博讯(Urovo)红外扫码适配器：基于设备内置扫描服务(android.device.ScanManager)
 * <p>
 * 1. 输出模式固定为广播模式(switchOutputMode(0))，条码结果经广播回传页面；
 * 2. 广播action/key不同固件可能不同：优先用getParameterString动态读取，
 *    读取失败回退文档默认值（android.intent.ACTION_DECODE_DATA / barcode_string）；
 * 3. 支持软触发startDecode/stopDecode，与设备扫描键等效；物理按键由系统扫描服务直接处理；
 * 4. 不修改触发模式、提示音/震动等全局设置，避免影响设备上其它应用。
 *
 * @author 10904
 */
public class UrovoScannerReader implements IScannerReader {

    private static final String TAG = "UrovoScanner";

    /** 文档默认广播action（PropertyID.WEDGE_INTENT_ACTION_NAME默认值） */
    private static final String DEFAULT_ACTION = "android.intent.ACTION_DECODE_DATA";
    /** 文档默认广播数据key（PropertyID.WEDGE_INTENT_DATA_STRING_TAG默认值） */
    private static final String DEFAULT_KEY = "barcode_string";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ScanManager scanManager;
    private ScannerCallbacks callbacks;
    private Activity activity;
    private BroadcastReceiver scanReceiver;
    private boolean receiverRegistered = false;
    /** 条码广播action（初始化时从设备动态读取） */
    private String action = DEFAULT_ACTION;
    /** 条码广播数据key（初始化时从设备动态读取） */
    private String key = DEFAULT_KEY;

    @Override
    public String getVendorName() {
        return "优博讯";
    }

    @Override
    public boolean isSupported(Context context) {
        if (ScanDeviceUtils.isUrovoDevice()) {
            return true;
        }
        // 贴牌机厂商名可能为alps等：排除东集机型后，再探测扫描服务参数是否可读
        if (ScanDeviceUtils.isNewDevice() || ScanDeviceUtils.isSeuicDevice()) {
            return false;
        }
        return canReadWedgeParams();
    }

    @Override
    public boolean supportsSoftwareTrigger() {
        return true;
    }

    @Override
    public void init(Activity activity, ScannerCallbacks callbacks) {
        this.activity = activity;
        this.callbacks = callbacks;
        try {
            scanManager = new ScanManager();
            // 0=广播模式(Intent输出)，保证条码结果经广播回传
            boolean broadcastMode = scanManager.switchOutputMode(0);
            readWedgeParams();
            scanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !action.equals(intent.getAction())) {
                        return;
                    }
                    String code = intent.getStringExtra(key);
                    if (StringUtils.isNullOrEmpty(code)) {
                        return;
                    }
                    postResult(code);
                }
            };
            Log.i(TAG, "[DIAG] 初始化完成 广播模式=" + broadcastMode + " action=" + action + " key=" + key);
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 初始化失败", e);
            notifyError(ScannerError.INIT_FAILED);
        }
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
        try {
            if (scanManager != null) {
                scanManager.stopDecode();
            }
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 停止扫描失败", e);
        }
        unregisterReceiver();
        scanManager = null;
        scanReceiver = null;
        activity = null;
        callbacks = null;
    }

    @Override
    public void startScan() {
        if (scanManager == null) {
            notifyError(ScannerError.INIT_FAILED);
            return;
        }
        try {
            boolean ok = scanManager.startDecode();
            Log.i(TAG, "[DIAG] startDecode=" + ok);
            if (!ok) {
                notifyError(ScannerError.START_FAILED);
            }
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 启动扫描失败", e);
            notifyError(ScannerError.START_FAILED);
        }
    }

    @Override
    public void stopScan() {
        if (scanManager == null) {
            return;
        }
        try {
            scanManager.stopDecode();
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 停止扫描失败", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 优博讯扫描键由系统扫描服务直接出光，按键通常不会下发到应用，无需拦截
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /** 读取当前固件的条码广播action/key（不同固件可配置，不能硬编码） */
    private void readWedgeParams() {
        try {
            String[] params = scanManager.getParameterString(new int[]{
                    PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG});
            if (params != null && params.length >= 2) {
                if (!StringUtils.isNullOrEmpty(params[0])) {
                    action = params[0];
                }
                if (!StringUtils.isNullOrEmpty(params[1])) {
                    key = params[1];
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "[DIAG] 读取广播参数失败，使用默认值", e);
        }
    }

    /** 扫描服务参数是否可读：贴牌优博讯机型（厂商名非UROVO）的兜底探测 */
    private boolean canReadWedgeParams() {
        try {
            String[] params = new ScanManager().getParameterString(new int[]{
                    PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG});
            return params != null && params.length > 0 && !StringUtils.isNullOrEmpty(params[0]);
        } catch (Throwable e) {
            return false;
        }
    }

    /** 注册条码广播 */
    private void registerReceiver() {
        if (activity == null || scanReceiver == null || receiverRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter(action);
            activity.registerReceiver(scanReceiver, filter);
            receiverRegistered = true;
        } catch (Throwable e) {
            Log.e(TAG, "[DIAG] 注册扫码广播失败", e);
        }
    }

    /** 注销条码广播 */
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

    /** 错误切回主线程回调页面 */
    private void notifyError(final ScannerError error) {
        mainHandler.post(() -> {
            if (callbacks != null) {
                callbacks.onScannerError(error);
            }
        });
    }
}