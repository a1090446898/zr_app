package com.gsoft.inventory.service.rfid.desai;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.utils.CommonUtils;
import com.hardware.rfid.Config;
import com.hardware.rfid.Connect;
import com.hardware.rfid.DataListener;
import com.hardware.rfid.Error;
import com.hardware.rfid.PhoneModuleController;
import com.hardware.rfid.RFIDModuleController;
import com.hardware.rfid.TagScanData;

/**
 * 得实(DSPDA) UHF适配器：SDK包com.hardware.rfid（AAR打包进APK）
 * <p>
 * 1. connect为阻塞串口I/O且SDK内部基于调用线程创建Handler，必须放在带Looper的专用线程
 * 2. 连接结果经DataListener.onConnect异步回调，需校验存储权限（错误码-3）
 * 3. 支持EPC+TID盘点区域，功率0~30dBm；连接失败1~3次内页面恢复时延迟自动重连
 */
public class DesaiRfidReader implements IRfidReader {

    private static final String TAG = "DesaiRfidReader";

    /** 得实RFID机型名单（R10 scanCode=261，D6500为563/564），新机型按需补充 */
    private static final String[] DESAI_MODELS = {"R10", "D6500"};

    /** UHF专用线程：SDK要求调用线程有Looper且connect为阻塞串口I/O */
    private static HandlerThread sUhfThread;
    private static Handler sUhfHandler;

    private Context mContext;
    private RfidCallbacks callbacks;
    /** 模块控制器（UHF线程创建并调用，主线程只读引用） */
    private volatile RFIDModuleController rfidController;
    /** 是否连接就绪 */
    private volatile boolean ready = false;
    /** 是否正在连接 */
    private volatile boolean connecting = false;
    /** 页面销毁后不再投递回调 */
    private volatile boolean attached = false;
    /** 连接失败累计次数（1~3次内页面恢复时自动重连） */
    private int connectFailCount = 0;
    /** 最近一次配置（缓存供内部重放） */
    private RfidConfig lastConfig = new RfidConfig();

    @Override
    public String getVendorName() {
        return "得实";
    }

    @Override
    public boolean isSupported(Context context) {
        // 机型名单优先（无IO开销）；名单未命中时尝试SDK自带的设备型号探测
        String model = Build.MODEL == null ? "" : Build.MODEL.toUpperCase();
        for (String m : DESAI_MODELS) {
            if (model.startsWith(m.toUpperCase())) {
                return true;
            }
        }
        try {
            return PhoneModuleController.getPDAModule() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean requiresStoragePermission() {
        return true;
    }

    @Override
    public boolean supportsTid() {
        return true;
    }

    @Override
    public int[] getPowerRange() {
        return new int[]{0, 30};
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isConnecting() {
        return connecting;
    }

    @Override
    public void connect(Context context, RfidCallbacks callbacks) {
        this.mContext = context;
        this.callbacks = callbacks;
        this.attached = true;
        connectInternal();
    }

    /**
     * 执行连接（UHF线程）：connect为阻塞串口I/O，结果经DataListener.onConnect异步回调
     */
    private void connectInternal() {
        if (connecting) {
            return;
        }
        connecting = true;
        getUhfHandler().post(() -> {
            try {
                String model = PhoneModuleController.getPDAModule();
                Log.i(TAG, "设备型号=" + model);
                rfidController = RFIDModuleController.connect(mContext, dataListener);
                if (rfidController == null) {
                    connecting = false;
                    Log.e(TAG, "connect返回null，当前设备无RFID模块");
                    postConnectResult(RfidError.DEVICE_NOT_SUPPORTED);
                } else {
                    Log.i(TAG, "RFID connect已提交");
                }
            } catch (Throwable e) {
                connecting = false;
                Log.e(TAG, "RFID模块连接异常", e);
                postConnectResult(RfidError.CONNECT_FAILED);
            }
        });
    }

    @Override
    public void disconnect() {
        attached = false;
        ready = false;
        final RFIDModuleController controller = rfidController;
        rfidController = null;
        if (controller != null) {
            getUhfHandler().post(() -> {
                try {
                    controller.stopScanTag();
                    controller.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "释放RFID模块失败", e);
                }
            });
        }
    }

    @Override
    public void startScan() {
        if (rfidController == null || !ready) {
            return;
        }
        getUhfHandler().post(() -> {
            try {
                rfidController.clearTag();
                rfidController.startScanTag();
                Log.i(TAG, "开始盘点");
            } catch (Exception e) {
                Log.e(TAG, "启动盘点失败", e);
                postHardwareError(RfidError.START_FAILED);
            }
        });
    }

    @Override
    public void stopScan() {
        if (rfidController != null && ready) {
            getUhfHandler().post(() -> {
                try {
                    rfidController.stopScanTag();
                    Log.i(TAG, "停止盘点");
                } catch (Exception e) {
                    Log.e(TAG, "停止盘点失败", e);
                }
            });
        }
    }

    @Override
    public void applyConfig(RfidConfig config) {
        lastConfig = config == null ? new RfidConfig() : config;
        applyConfigInternal();
    }

    /** 应用参数到模块（UHF线程）：功率/盘点区域(TID)/会话/频率区域 */
    private void applyConfigInternal() {
        if (rfidController == null || !ready) {
            return;
        }
        final RfidConfig config = lastConfig;
        getUhfHandler().post(() -> {
            try {
                rfidController.setRFIDConfig(Config.PARA_POWER, config.power);
                rfidController.setRFIDConfig(Config.PARA_ENABLE_TID, config.mode == RfidConfig.MODE_EPC_TID ? Config.TID_YES : Config.TID_NO);
                rfidController.setRFIDConfig(Config.PARA_SESSION, config.session);
                rfidController.setRFIDConfig(Config.PARA_REGION, mapRegion(config.region));
                Log.i(TAG, "RFID设置已应用：power=" + config.power + " mode=" + config.mode
                        + " session=" + config.session + " region=" + config.region);
            } catch (Exception e) {
                Log.e(TAG, "应用RFID设置失败", e);
            }
        });
    }

    /** 统一频率区域 → 得实SDK取值 */
    private static int mapRegion(int region) {
        switch (region) {
            case RfidConfig.REGION_EU:
                return Config.EUROPE;
            case RfidConfig.REGION_USA:
                return Config.NORTH_AMERICAN;
            default:
                return Config.CHINA1;
        }
    }

    @Override
    public void onPageResume() {
        // 连接失败1~3次内且当前未连接时，延迟800ms断开旧连接并自动重连
        if (mContext == null || ready || connecting || connectFailCount <= 0 || connectFailCount > 3 || !attached) {
            return;
        }
        CommonUtils.getHandler().postDelayed(() -> {
            if (ready || connecting || !attached) {
                return;
            }
            final RFIDModuleController controller = rfidController;
            getUhfHandler().post(() -> {
                try {
                    if (controller != null) {
                        controller.disconnect();
                        Log.i(TAG, "重连前已断开旧连接");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "重连前断开旧连接失败", e);
                }
            });
            connectInternal();
        }, 800);
    }

    @Override
    public void onPagePause() {
        // 得实模块无需休眠处理；停止扫描由业务层统一调用stopScan
    }

    @Override
    public boolean isTriggerKey(int keyCode, int scanCode) {
        // 得实R10 scanCode=261，D6500为563/564，其他机型为250/313/523
        // 保留旧机型keyCode 139/140/141/142/292/293 兼容
        return scanCode == 261 || scanCode == 563 || scanCode == 564
                || scanCode == 250 || scanCode == 313 || scanCode == 523
                || keyCode == 139 || keyCode == 140 || keyCode == 141 || keyCode == 142
                || keyCode == 292 || keyCode == 293;
    }

    /** RFID SDK回调监听：onConnect/onError切主线程投递，onScanTag在SDK线程归一化后切主线程 */
    private final DataListener dataListener = new DataListener() {
        @Override
        public void onConnect(Connect connect) {
            int status = connect == null ? -1 : connect.getErr_status();
            Log.i(TAG, "RFID连接回调 err_status=" + status);
            CommonUtils.getHandler().post(() -> {
                connecting = false;
                if (!attached) {
                    return;
                }
                if (status == 0) {
                    ready = true;
                    connectFailCount = 0;
                    postConnectResult(RfidError.SUCCESS);
                } else {
                    ready = false;
                    connectFailCount++;
                    postConnectResult(mapError(status));
                }
            });
        }

        @Override
        public void onScanTag(TagScanData data) {
            if (data == null || data.getEpc() == null) {
                return;
            }
            final RfidTagData tag = new RfidTagData();
            tag.epc = RFIDModuleController.byteToHexStr(data.getEpc()).toUpperCase();
            // 得实SDK返回float（小数位无显示意义），统一模型为int，显式取整
            tag.rssi = (int) data.getRssi();
            tag.tid = null;
            CommonUtils.getHandler().post(() -> {
                if (attached && callbacks != null) {
                    callbacks.onTagScanned(tag);
                }
            });
        }

        @Override
        public void onError(Error error) {
            Log.e(TAG, "RFID错误回调:" + (error == null ? "null" : error.getErr_status()));
        }
    };

    /** 得实错误码 → 统一错误枚举（SDK文档第4章错误码表） */
    private static RfidError mapError(int status) {
        switch (status) {
            case -1:
                return RfidError.SDK_VERIFY_FAILED;
            case -2:
                return RfidError.FILE_INVALID;
            case -3:
                return RfidError.STORAGE_PERMISSION;
            case -8:
                return RfidError.PORT_CONFLICT;
            case -9:
                return RfidError.NOT_IN_SUPPORT_LIST;
            case -10:
                return RfidError.MODEL_NOT_SUPPORTED;
            default:
                return RfidError.UNKNOWN;
        }
    }

    private void postConnectResult(RfidError error) {
        if (attached && callbacks != null) {
            callbacks.onConnectResult(error);
        }
    }

    private void postHardwareError(RfidError error) {
        if (attached && callbacks != null) {
            callbacks.onHardwareError(error);
        }
    }

    /** 获取UHF专用线程Handler（懒加载单例） */
    private static Handler getUhfHandler() {
        if (sUhfThread == null || !sUhfThread.isAlive()) {
            sUhfThread = new HandlerThread("uhf-sdk");
            sUhfThread.start();
            sUhfHandler = new Handler(sUhfThread.getLooper());
        }
        return sUhfHandler;
    }
}