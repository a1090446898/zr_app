package com.gsoft.inventory.service.rfid.urovo;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.utils.CommonUtils;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ReadTag;
import com.ubx.usdk.bean.enums.CountryCode;
import com.ubx.usdk.bean.enums.FrequencyRegion;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.bean.enums.QueryMemBank;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.KeyEventListener;
import com.ubx.usdk.listener.DataCallback;
import com.ubx.usdk.listener.InitListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 优博讯(Urovo)一体机适配器：RFID模块集成于手持机内部，SDK包com.ubx.usdk（URFIDLibrary aar打包进APK）
 * <p>
 * 1. 一体机走init()初始化：InitListener.onStatus(true)即就绪，无BLE连接/MAC缓存/定位权限依赖
 * 2. SDK接口阻塞主线程且顺序执行：统一走CommonUtils单线程池串行下发，回调统一切主线程投递
 * 3. 参数下发顺序：先切CUSTOM_MODE场景(否则Session/存储区等锁死报260)，功率最后设置
 *    （切换场景模式会重置功率）；EPC/EPC+TID对应setQueryMemoryBank
 * 4. 一体机物理扫描键(515/523)走页面onKeyDown/onKeyUp经isTriggerKey路由；为兼容分体外设/
 *    枪柄形态，非INTEGRATED设备时额外注册GripDeviceManager按键监听，经RfidCallbacks的
 *    onTriggerKeyDown/onTriggerKeyUp路由给页面（页面持有scanning状态，禁止绕过）
 * 5. 释放统一RFIDSDKManager.release()（v2.5.1230已移除rfidManager.release()）
 */
public class UrovoRfidReader implements IRfidReader {

    private static final String TAG = "UrovoRfidReader";

    /** 默认功率上限兜底（getSupportMaxOutputPower失败时） */
    private static final int DEFAULT_MAX_POWER = 33;

    /** 优博讯机型名单兜底（一体机机型），新机型按需补充 */
    private static final String[] UROVO_MODELS = {"DT610", "DT50"};

    private Context mContext;
    private RfidCallbacks callbacks;
    /** 是否连接就绪（可开始扫描） */
    private volatile boolean ready = false;
    /** 是否正在连接 */
    private volatile boolean connecting = false;
    /** 页面销毁后不再投递任何回调 */
    private volatile boolean attached = false;
    /** 页面激活标志：外设按键仅页面可见时派发 */
    private volatile boolean pageActive = false;
    /** 连接成功后缓存的模块最大功率（UI范围与下发clamp用） */
    private volatile int maxOutputPower = 0;
    /** 最近一次配置（缓存供内部重放） */
    private RfidConfig lastConfig = new RfidConfig();

    @Override
    public String getVendorName() {
        return "优博讯";
    }

    @Override
    public boolean isSupported(Context context) {
        // 厂商串优先；兜底按机型名单前缀匹配（探测无IO开销）
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("urovo")) {
            return true;
        }
        String brand = Build.BRAND == null ? "" : Build.BRAND.toLowerCase();
        if (brand.contains("urovo")) {
            return true;
        }
        String model = Build.MODEL == null ? "" : Build.MODEL.toUpperCase();
        for (String m : UROVO_MODELS) {
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
        // 一体机init初始化不依赖定位权限（定位权限为BLE枪柄首次搜索所需）
        return false;
    }

    @Override
    public boolean supportsTid() {
        // setQueryMemoryBank支持EPC_TID混合读取
        return true;
    }

    @Override
    public int[] getPowerRange() {
        int max = maxOutputPower > 0 ? maxOutputPower : DEFAULT_MAX_POWER;
        return new int[]{0, max};
    }

    @Override
    public int getDefaultPower() {
        // 文档：一体机默认按设备支持的最大功率输出；连接成功后为实测值，连接前用兜底上限
        return maxOutputPower > 0 ? maxOutputPower : DEFAULT_MAX_POWER;
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
        if (context == null) {
            return;
        }
        this.mContext = context.getApplicationContext();
        this.callbacks = callbacks;
        this.attached = true;
        this.connecting = true;
        // SDK接口阻塞主线程：init放单线程池，结果经InitListener回调
        CommonUtils.getExecutor().execute(this::doInit);
    }

    /** 提交SDK初始化（子线程）：onStatus(true)后进入就绪流程 */
    private void doInit() {
        try {
            RFIDSDKManager.getInstance().init(mContext, new InitListener() {
                @Override
                public void onStatus(boolean status) {
                    Log.i(TAG, "init onStatus=" + status);
                    if (!attached) {
                        return;
                    }
                    if (status) {
                        CommonUtils.getExecutor().execute(UrovoRfidReader.this::finishConnectSetup);
                    } else {
                        failConnect(RfidError.CONNECT_FAILED);
                    }
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "RFID init异常", e);
            failConnect(RfidError.CONNECT_FAILED);
        }
    }

    /** 连接失败统一出口：复位状态并投递错误（自动切主线程） */
    private void failConnect(RfidError error) {
        connecting = false;
        ready = false;
        postConnectResult(error);
    }

    /** 初始化成功后的就绪流程（单线程池）：缓存最大功率、注册标签回调与外设按键监听 */
    private void finishConnectSetup() {
        try {
            if (RFIDSDKManager.getInstance().getRfidManager() == null) {
                Log.e(TAG, "getRfidManager返回null");
                failConnect(RfidError.CONNECT_FAILED);
                return;
            }
            int max = RFIDSDKManager.getInstance().getRfidManager().getSupportMaxOutputPower();
            maxOutputPower = max > 0 ? max : DEFAULT_MAX_POWER;
            RFIDSDKManager.getInstance().getRfidManager().addDataCallback(dataCallback);
            RFIDSDKManager.getInstance().getRfidManager().setRssiInDbm(true);
            // 外设按键监听：仅非一体机(INTEGRATED)注册（一体机按键走系统KeyEvent，由页面处理）
            CommonUtils.getHandler().post(this::registerGripKeyListener);
            ready = true;
            connecting = false;
            Log.i(TAG, "一体机RFID就绪 maxPower=" + maxOutputPower);
            postConnectResult(RfidError.SUCCESS);
        } catch (Throwable e) {
            Log.e(TAG, "RFID初始化后处理异常", e);
            failConnect(RfidError.CONNECT_FAILED);
        }
    }

    /** 注册外设按键监听（主线程）：非一体机（分体/枪柄外设）才有效，一体机自动跳过 */
    private void registerGripKeyListener() {
        try {
            if (RFIDSDKManager.getInstance().getRfidManager() == null) {
                return;
            }
            if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType()
                    != ReaderDeviceType.INTEGRATED) {
                GripDeviceManager.getInstance().setKeyEventListener(gripKeyListener);
                Log.i(TAG, "已注册外设按键监听");
            }
        } catch (Throwable e) {
            Log.e(TAG, "注册外设按键监听失败", e);
        }
    }

    /** 外设按键回调（任意线程）：BT_SCAN按下/抬起分别路由到页面 */
    private final KeyEventListener gripKeyListener = new KeyEventListener() {
        @Override
        public void event(int keyCode, boolean isDown) {
            if (!attached || !pageActive) {
                return;
            }
            if (keyCode != BTKeyEvent.BT_SCAN) {
                return;
            }
            Log.i(TAG, "外设按键 isDown=" + isDown);
            if (isDown) {
                postTriggerKeyDown();
            } else {
                postTriggerKeyUp();
            }
        }
    };

    @Override
    public void disconnect() {
        attached = false;
        pageActive = false;
        ready = false;
        connecting = false;
        CommonUtils.getExecutor().execute(() -> {
            try {
                // v2.5.1230：释放统一走RFIDSDKManager（rfidManager.release已移除）
                RFIDSDKManager.getInstance().release();
                Log.i(TAG, "优博讯RFID已释放");
            } catch (Throwable e) {
                Log.e(TAG, "释放优博讯RFID失败", e);
            }
        });
    }

    @Override
    public void startScan() {
        if (!ready) {
            return;
        }
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (RFIDSDKManager.getInstance().getRfidManager() == null
                        || !RFIDSDKManager.getInstance().getRfidManager().isConnected()) {
                    Log.e(TAG, "模块已断开，无法启动盘点");
                    postHardwareError(RfidError.START_FAILED);
                    return;
                }
                int result = RFIDSDKManager.getInstance().getRfidManager().startInventory();
                Log.i(TAG, "开始盘点 result=" + result);
                if (result != 0) {
                    postHardwareError(RfidError.START_FAILED);
                }
            } catch (Throwable e) {
                Log.e(TAG, "启动盘点异常", e);
                postHardwareError(RfidError.START_FAILED);
            }
        });
    }

    @Override
    public void stopScan() {
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                    RFIDSDKManager.getInstance().getRfidManager().stopInventory();
                    Log.i(TAG, "停止盘点");
                }
            } catch (Throwable e) {
                Log.e(TAG, "停止盘点异常", e);
            }
        });
    }

    @Override
    public void applyConfig(RfidConfig config) {
        lastConfig = config == null ? new RfidConfig() : config;
        applyConfigInternal();
    }

    /** 应用参数（单线程池）：顺序=CUSTOM_MODE→存储区→会话→频段→功率(最后) */
    private void applyConfigInternal() {
        if (!ready) {
            return;
        }
        final RfidConfig config = lastConfig;
        CommonUtils.getExecutor().execute(() -> {
            try {
                // 1. 切自定义模式：非CUSTOM_MODE时Session/存储区等参数被锁定(错误码260)
                RFIDSDKManager.getInstance().getRfidManager()
                        .setInventorySceneMode(InventorySceneMode.CUSTOM_MODE);
                // 2. 盘点存储区：EPC_TID混读TID前6字；EPC纯读
                if (config.mode == RfidConfig.MODE_EPC_TID) {
                    RFIDSDKManager.getInstance().getRfidManager()
                            .setQueryMemoryBank(QueryMemBank.EPC_TID, 0, 6);
                } else {
                    RFIDSDKManager.getInstance().getRfidManager()
                            .setQueryMemoryBank(QueryMemBank.EPC, 0, 0);
                }
                // 3. 会话S0/S1（文档§2.5.5：0=S0、1=S1；需CUSTOM_MODE，返回非0为错误码）
                int sessionResult = RFIDSDKManager.getInstance().getRfidManager()
                        .setInventoryWithSession(config.session == 0 ? 0 : 1);
                if (sessionResult != 0) {
                    Log.w(TAG, "设置会话失败 code=" + sessionResult + " session=" + config.session);
                }
                // 4. 频段：从支持列表按名称匹配
                applyFrequencyRegion(config.region);
                // 5. 功率必须最后设置（文档§2.3.1：切换场景模式会重置功率）
                int max = maxOutputPower > 0 ? maxOutputPower : DEFAULT_MAX_POWER;
                int powerResult = RFIDSDKManager.getInstance().getRfidManager()
                        .setOutputPower(clamp(config.power, 0, max));
                if (powerResult != 0) {
                    Log.w(TAG, "设置功率失败 code=" + powerResult + " power=" + config.power);
                }
                Log.i(TAG, "配置已应用 mode=" + config.mode + " session=" + config.session
                        + " region=" + config.region + " power=" + config.power);
            } catch (Throwable e) {
                Log.e(TAG, "应用配置失败", e);
            }
        });
    }

    /**
     * 统一频率区域 → 支持频段列表按名称匹配后整段下发（多候选自动重试）
     * <p>
     * 文档：频段集合由getSupportFrequencyBandList()获取，setFrequencyRegion(region, startIndex, endIndex)
     * 指定起止信道索引并返回错误码；信道索引口径与官方Demo(SettingActivity.handleSetFrequency)一致：
     * 起=0、止=channelCount-1整段下发（列表元素的min/maxChannelIndex是"回读当前选中范围"语义，不可直接下发）
     * <p>
     * 中国区在SDK列表里可能同时存在China1/China2两个频段（920~925与840~845，各家命名顺序不一）：
     * 起始频率900~940MHz的候选优先尝试，失败自动换下一候选；全部候选失败或未匹配时，
     * 按国家码setWorkRegion兜底（文档2.7.1）；不做列表首位兜底（列表含多国频段，误切会影响读距与合规）
     */
    private void applyFrequencyRegion(int region) {
        try {
            List<FrequencyRegion> bands = RFIDSDKManager.getInstance().getRfidManager()
                    .getSupportFrequencyBandList();
            if (bands == null || bands.isEmpty()) {
                Log.w(TAG, "支持频段列表为空，跳过频段设置");
                return;
            }
            Log.i(TAG, "支持频段列表=" + describeBands(bands));
            List<FrequencyRegion> candidates = matchBands(region, bands);
            if (candidates.isEmpty()) {
                Log.w(TAG, "频段未匹配region=" + region + "，尝试国家码兜底");
            }
            for (FrequencyRegion band : candidates) {
                if (trySetBand(band)) {
                    return;
                }
            }
            fallbackByCountryCode(region);
        } catch (Throwable e) {
            Log.w(TAG, "设置频段失败（不阻断）", e);
        }
    }

    /** 按统一区域收集候选频段：中国区优先920~925MHz频段（起始频率900~940MHz），其余按列表顺序 */
    private static List<FrequencyRegion> matchBands(int region, List<FrequencyRegion> bands) {
        List<FrequencyRegion> primary = new ArrayList<>();
        List<FrequencyRegion> secondary = new ArrayList<>();
        for (FrequencyRegion band : bands) {
            if (band == null) {
                continue;
            }
            String name = band.getRegionName();
            String upper = name == null ? "" : name.toUpperCase();
            boolean hit;
            if (region == RfidConfig.REGION_EU) {
                hit = upper.contains("ETSI") || upper.contains("EU");
            } else if (region == RfidConfig.REGION_USA) {
                hit = upper.contains("FCC") || upper.contains("USA");
            } else {
                hit = upper.contains("CHN") || upper.contains("CHINA") || upper.contains("中国")
                        || "CN".equals(upper);
            }
            if (!hit) {
                continue;
            }
            float start = safeStartFreq(band);
            if (region == RfidConfig.REGION_CHN && !(start >= 900f && start <= 940f)) {
                secondary.add(band);
            } else {
                primary.add(band);
            }
        }
        primary.addAll(secondary);
        return primary;
    }

    /** 尝试下发单个频段整段信道（0~channelCount-1）并回读校验，成功返回true */
    private boolean trySetBand(FrequencyRegion band) {
        try {
            int count = band.getChannelCount();
            if (count <= 0) {
                Log.w(TAG, "频段信道数为0，跳过 band=" + band.getRegionName());
                return false;
            }
            int result = RFIDSDKManager.getInstance().getRfidManager()
                    .setFrequencyRegion(band, 0, count - 1);
            if (result != 0) {
                Log.w(TAG, "设置频段失败 code=" + result + " band=" + band.getRegionName()
                        + " 信道=0~" + (count - 1));
                return false;
            }
            // 回读校验：确认模块当前频段与目标一致（枚举实例可直接比较，名称比对兜底）
            FrequencyRegion current = RFIDSDKManager.getInstance().getRfidManager().getFrequencyRegion();
            boolean applied = current == band || (current != null
                    && current.getRegionName() != null
                    && current.getRegionName().equals(band.getRegionName()));
            if (applied) {
                Log.i(TAG, "频段已设置并回读确认 band=" + band.getRegionName() + " 信道=0~" + (count - 1));
                return true;
            }
            Log.w(TAG, "频段回读与目标不一致 current="
                    + (current == null ? "null" : current.getRegionName())
                    + " target=" + band.getRegionName() + "，尝试下一候选");
            return false;
        } catch (Throwable e) {
            Log.w(TAG, "设置频段异常 band=" + band.getRegionName(), e);
            return false;
        }
    }

    /**
     * 国家码兜底（文档2.7.1 setWorkRegion）：候选频段均失败或未匹配时按国家码整段设置；
     * 仅中国(CHN)/美国(USA)有明确对应国家码，欧洲无单一国家码不做兜底
     */
    private void fallbackByCountryCode(int region) {
        String code = region == RfidConfig.REGION_CHN ? "CHN"
                : region == RfidConfig.REGION_USA ? "USA" : null;
        if (code == null) {
            Log.w(TAG, "频段设置未成功且无国家码兜底 region=" + region);
            return;
        }
        try {
            CountryCode countryCode = CountryCode.valueOf(code);
            int result = RFIDSDKManager.getInstance().getRfidManager().setWorkRegion(countryCode);
            CountryCode current = RFIDSDKManager.getInstance().getRfidManager().getWorkRegion();
            Log.i(TAG, "国家码兜底设置频段 code=" + code + " result=" + result
                    + " 回读=" + (current == null ? "null" : current.name()));
        } catch (Throwable e) {
            Log.w(TAG, "国家码兜底设置频段失败 code=" + code, e);
        }
    }

    /** 支持频段列表摘要（日志排查用：名称+信道数+起始频率） */
    private static String describeBands(List<FrequencyRegion> bands) {
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < bands.size(); i++) {
                FrequencyRegion band = bands.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(band == null ? "null"
                        : band.getRegionName() + "(" + band.getChannelCount() + "信道,"
                        + safeStartFreq(band) + "MHz起)");
            }
            return sb.append("]").toString();
        } catch (Throwable e) {
            return "读取频段列表描述失败";
        }
    }

    /** 频段起始频率(MHz)，读取失败返回0（用于中国区920优先排序与日志） */
    private static float safeStartFreq(FrequencyRegion band) {
        try {
            return (float) band.getStartFreqMHz();
        } catch (Throwable e) {
            return 0f;
        }
    }

    @Override
    public void onPageResume() {
        pageActive = true;
        if (!attached || !ready) {
            return;
        }
        // 页面恢复时检测模块断开（休眠等），断开则重新init
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (RFIDSDKManager.getInstance().getRfidManager() == null
                        || !RFIDSDKManager.getInstance().getRfidManager().isConnected()) {
                    Log.w(TAG, "RFID模块连接已断开，尝试重新初始化");
                    ready = false;
                    connecting = true;
                    doInit();
                }
            } catch (Throwable e) {
                Log.e(TAG, "页面恢复检测失败", e);
            }
        });
    }

    @Override
    public void onPagePause() {
        pageActive = false;
        if (!ready) {
            return;
        }
        CommonUtils.getExecutor().execute(() -> {
            try {
                if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                    RFIDSDKManager.getInstance().getRfidManager().stopInventory();
                }
            } catch (Throwable e) {
                Log.e(TAG, "页面暂停停止盘点失败", e);
            }
        });
    }

    @Override
    public boolean isTriggerKey(int keyCode, int scanCode) {
        // 优博讯Demo按键集合：515/523
        return keyCode == 515 || keyCode == 523;
    }

    /** 盘点数据回调（SDK线程）：归一化为统一模型后投递主线程 */
    private final DataCallback dataCallback = new DataCallback() {
        @Override
        public void onInventoryTag(ReadTag readTag) {
            if (readTag == null || TextUtils.isEmpty(readTag.epcId)) {
                return;
            }
            final RfidTagData tag = new RfidTagData();
            tag.epc = readTag.epcId.toUpperCase();
            tag.rssi = readTag.rssi;
            // memId为混读存储区数据（EPC_TID模式为TID），仅非空时携带
            tag.tid = TextUtils.isEmpty(readTag.memId) ? null : readTag.memId.toUpperCase();
            CommonUtils.getHandler().post(() -> {
                if (attached && callbacks != null) {
                    callbacks.onTagScanned(tag);
                }
            });
        }

        @Override
        public void onInventoryTagEnd() {
            Log.i(TAG, "盘点结束");
        }
    };

    private void postConnectResult(RfidError error) {
        CommonUtils.getHandler().post(() -> {
            if (attached && callbacks != null) {
                callbacks.onConnectResult(error);
            }
        });
    }

    private void postHardwareError(RfidError error) {
        CommonUtils.getHandler().post(() -> {
            if (attached && callbacks != null) {
                callbacks.onHardwareError(error);
            }
        });
    }

    private void postTriggerKeyDown() {
        CommonUtils.getHandler().post(() -> {
            if (attached && callbacks != null) {
                callbacks.onTriggerKeyDown();
            }
        });
    }

    private void postTriggerKeyUp() {
        CommonUtils.getHandler().post(() -> {
            if (attached && callbacks != null) {
                callbacks.onTriggerKeyUp();
            }
        });
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}