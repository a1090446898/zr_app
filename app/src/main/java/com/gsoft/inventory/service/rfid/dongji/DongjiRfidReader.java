package com.gsoft.inventory.service.rfid.dongji;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.utils.CommonUtils;
import com.seuic.uhf.EPC;
import com.seuic.uhf.IReadTagsListener;
import com.seuic.uhf.UHFService;

import java.util.List;

/**
 * 东集(SEUIC) UHF适配器：系统包com.seuic.uhf（Manifest uses-library required=false，仅编译期依赖uhf.jar）
 * <p>
 * 1. API为同步boolean调用（Binder系统服务），无阻塞串口，单线程Executor串行下发指令即可
 * 2. 回调模式registerReadTags批量返回List(EPC)（SDK已排重），逐条归一化后投递主线程
 * 3. 系统休眠UHF模块断电：页面暂停时close，恢复时重新open并重放配置
 * 4. 不支持EPC+TID盘点区域（supportsTid=false，设置页隐藏该项）；功率5~33dBm
 */
public class DongjiRfidReader implements IRfidReader {

    private static final String TAG = "DongjiRfidReader";

    /** 东集UHF设备机型名单（SDK文档：D500/D501H/D502H/R109），新机型按需补充 */
    private static final String[] SEUIC_MODELS = {"D500", "D501H", "D502H", "R109"};

    private Context mContext;
    private RfidCallbacks callbacks;
    private volatile UHFService mService;
    private volatile boolean ready = false;
    private volatile boolean attached = false;
    private RfidConfig lastConfig = new RfidConfig();

    @Override
    public String getVendorName() {
        return "东集";
    }

    @Override
    public boolean isSupported(Context context) {
        // 厂商串优先；兜底按机型名单前缀匹配（R109先于得实R10判断，工厂中本适配器排在前位）
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("seuic")) {
            return true;
        }
        String model = Build.MODEL == null ? "" : Build.MODEL.toUpperCase();
        for (String m : SEUIC_MODELS) {
            if (model.startsWith(m.toUpperCase())) {
                return true;
            }
        }
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
        return new int[]{5, 33};
    }

    @Override
    public int getDefaultPower() {
        // 非优博讯设备：默认功率33dBm（取值上限）
        return 33;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void connect(Context context, RfidCallbacks callbacks) {
        this.mContext = context;
        this.callbacks = callbacks;
        this.attached = true;
        // 复用项目单线程池（CommonUtils.getExecutor），保证硬件指令串行有序
        CommonUtils.getExecutor().execute(() -> {
            try {
                UHFService service = UHFService.getInstance();
                boolean ok = service.open();
                if (!attached) {
                    service.close();
                    return;
                }
                if (ok) {
                    mService = service;
                    ready = true;
                    service.registerReadTags(mReadTagsListener);
                    Log.i(TAG, "东集UHF open成功");
                    postConnectResult(RfidError.SUCCESS);
                } else {
                    ready = false;
                    Log.e(TAG, "东集UHF open失败");
                    postConnectResult(RfidError.OPEN_FAILED);
                }
            } catch (Throwable e) {
                ready = false;
                Log.e(TAG, "东集UHF open异常", e);
                postConnectResult(RfidError.OPEN_FAILED);
            }
        });
    }

    @Override
    public void disconnect() {
        attached = false;
        ready = false;
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (mService != null) {
                    mService.inventoryStop();
                    mService.unregisterReadTags(mReadTagsListener);
                    mService.close();
                    Log.i(TAG, "东集UHF已关闭");
                }
            } catch (Throwable e) {
                Log.e(TAG, "关闭东集UHF失败", e);
            }
        });
    }

    @Override
    public void startScan() {
        if (!ready || mService == null) {
            return;
        }
        CommonUtils.getExecutor().execute(() -> {
            try {
                // 开始前清空EPC列表，保证每次盘点互不残留
                mService.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST, 1);
                boolean ok = mService.inventoryStart();
                Log.i(TAG, "东集开始盘点 result=" + ok);
                if (!ok) {
                    postHardwareError(RfidError.START_FAILED);
                }
            } catch (Throwable e) {
                Log.e(TAG, "东集启动盘点异常", e);
                postHardwareError(RfidError.START_FAILED);
            }
        });
    }

    @Override
    public void stopScan() {
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (mService != null) {
                    mService.inventoryStop();
                    Log.i(TAG, "东集停止盘点");
                }
            } catch (Throwable e) {
                Log.e(TAG, "东集停止盘点失败", e);
            }
        });
    }

    @Override
    public void applyConfig(RfidConfig config) {
        lastConfig = config == null ? new RfidConfig() : config;
        applyConfigInternal();
    }

    private void applyConfigInternal() {
        if (!ready || mService == null) {
            return;
        }
        final RfidConfig config = lastConfig;
        CommonUtils.getExecutor().execute(() -> {
            try {
                // 功率5~33dBm（文档§7：setPower范围5~33，返回false为失败）
                if (!mService.setPower(clamp(config.power, 5, 33))) {
                    Log.w(TAG, "设置功率失败 power=" + config.power);
                }
                // 会话S0/S1（文档§22：setParameters返回false为失败；东集取值0~3，业务统一0/1为子集）
                if (!mService.setParameters(UHFService.PARAMETER_INVENTORY_SESSION, config.session)) {
                    Log.w(TAG, "设置会话失败 session=" + config.session);
                }
                // 频段：文档setRegion取值仅支持"FCC"/"ETSI"；中国区域不可设，保持模块内置频段(China1，920~925MHz)
                String region = mapRegion(config.region);
                if (region != null) {
                    boolean regionOk = mService.setRegion(region);
                    if (!regionOk) {
                        Log.w(TAG, "设置频段失败 region=" + region);
                    }
                } else {
                    Log.i(TAG, "中国频率区域使用模块内置频段，跳过setRegion");
                }
                // 输出隐藏PC号，与得实侧业务解码(超32位去PC字头)口径一致
                mService.setParameters(UHFService.PARAMETER_HIDE_PC, 1);
                // 开始寻卡自动清空之前EPC列表
                mService.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST_WHEN_START_INVENTORY, 1);
                Log.i(TAG, "东集设置已应用：power=" + config.power + " session=" + config.session + " region=" + config.region);
            } catch (Throwable e) {
                Log.e(TAG, "应用东集设置失败", e);
            }
        });
    }

    /** 统一频率区域 → 东集setRegion字符串取值（文档：setRegion仅支持"FCC"/"ETSI"，中国区域不可设返回null） */
    private static String mapRegion(int region) {
        switch (region) {
            case RfidConfig.REGION_EU:
                return "ETSI";
            case RfidConfig.REGION_USA:
                return "FCC";
            default:
                return null;
        }
    }

    @Override
    public void onPageResume() {
        // 系统休眠UHF模块断电：恢复时若模块已关则重新open并重放配置
        if (!ready || mService == null) {
            return;
        }
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (!mService.isOpen()) {
                    boolean ok = mService.open();
                    Log.i(TAG, "休眠唤醒后重新open result=" + ok);
                    if (!ok) {
                        ready = false;
                        postConnectResult(RfidError.OPEN_FAILED);
                        return;
                    }
                    applyConfigInternal();
                }
            } catch (Throwable e) {
                ready = false;
                Log.e(TAG, "休眠唤醒后重新open异常", e);
                postConnectResult(RfidError.OPEN_FAILED);
            }
        });
    }

    @Override
    public void onPagePause() {
        // 系统休眠前必须close（SDK文档要求），唤醒后由onPageResume重新open
        if (!ready || mService == null) {
            return;
        }
        CommonUtils.getExecutor().execute(() -> {
            try {
                mService.inventoryStop();
                mService.close();
                Log.i(TAG, "东集UHF已close（页面暂停，防系统休眠断电）");
            } catch (Throwable e) {
                Log.e(TAG, "东集close失败", e);
            }
        });
    }

    @Override
    public boolean isTriggerKey(int keyCode, int scanCode) {
        // 东集机型按键集合待真机验证（onKeyDown日志打印实际值后补充）
        return scanCode == 261 || scanCode == 563 || scanCode == 564
                || scanCode == 250 || scanCode == 313 || scanCode == 523
                || keyCode == 139 || keyCode == 140 || keyCode == 141 || keyCode == 142
                || keyCode == 292 || keyCode == 293;
    }

    /** 东集回调：批量返回最新盘存到的标签（SDK线程，逐条归一化后投递主线程） */
    private final IReadTagsListener mReadTagsListener = new IReadTagsListener() {
        @Override
        public void tagsRead(List<EPC> epcList) {
            if (!ready || epcList == null || epcList.isEmpty()) {
                return;
            }
            for (EPC epc : epcList) {
                if (epc == null) {
                    continue;
                }
                String hex = epc.getId();
                if (hex == null || hex.isEmpty()) {
                    continue;
                }
                final RfidTagData tag = new RfidTagData();
                tag.epc = hex.toUpperCase();
                tag.rssi = epc.rssi;
                tag.tid = null;
                CommonUtils.getHandler().post(() -> {
                    if (attached && callbacks != null) {
                        callbacks.onTagScanned(tag);
                    }
                });
            }
        }
    };

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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}