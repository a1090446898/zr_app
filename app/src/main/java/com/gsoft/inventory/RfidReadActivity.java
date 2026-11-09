package com.gsoft.inventory;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.gsoft.inventory.abs.InventoryScanBaseActivity;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidReaderFactory;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.DownloadZrFileManager;
import com.gsoft.inventory.utils.RfidEpcUtils;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.view.rfidfind.FindAssetsCriteria;
import com.gsoft.inventory.view.rfidfind.FindAssetsSettingActivity;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

/**
 * RFID盘点扫描页（按存放地点盘点）
 * <p>
 * 本页继承盘点扫描公共基类 InventoryScanBaseActivity，
 * 公共业务（盘点范围统计、标签卡片列表、一键变更、修改跳转等）在基类实现，
 * 本类只负责RFID识别源与专属UI：
 * <p>
 * 1. 硬件访问经统一接口IRfidReader由厂家适配器实现（优博讯com.ubx.usdk / 东集com.seuic.uhf），
 *    本页保留设备按键触发盘点、功率/区域/会话/频率设置等业务交互
 * 2. EPC编码处理：判断EPC是否为ASCII编码（是则转码为条码，不是则用EPC原值），随后交由基类公共命中处理
 * 3. 顶部"设置"菜单：找资产/功率/盘点区域(仅支持TID的机型)/会话/频率区域/提示音/重置当前地点盘点状态
 * 4. 找资产：开启后只认查找目标，非目标标签静默忽略（不提示、不标记、不加入列表）
 * 5. 扫描开停：按钮与设备RFID硬件按键均可触发，适配器负责会话启停
 * @author 10904
 */
public class RfidReadActivity extends InventoryScanBaseActivity {

    private static final String TAG = "RfidReadActivity";

    /** 盘点区域 */
    public static final int MODE_EPC = RfidConfig.MODE_EPC;
    public static final int MODE_EPC_TID = RfidConfig.MODE_EPC_TID;
    /** 频率区域 */
    public static final int REGION_CHN = RfidConfig.REGION_CHN;           // 中国920~925MHz
    public static final int REGION_EU = RfidConfig.REGION_EU;             // 欧洲865~868MHz
    public static final int REGION_USA = RfidConfig.REGION_USA;           // 美国902~928MHz

    /** 设置菜单项标识：用 tag 分发，避免增删条目后行号错位 */
    private static final String SETTING_FIND = "find";
    private static final String SETTING_POWER = "power";
    private static final String SETTING_MODE = "mode";
    private static final String SETTING_SESSION = "session";
    private static final String SETTING_REGION = "region";
    private static final String SETTING_SOUND = "sound";
    private static final String SETTING_RESET = "reset";

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 开始/停止扫描按钮 */
    @BindView(R.id.buttonScan)
    QMUIButton buttonScan;
    /** 找资产状态行：开启查找后展示查找方式与已找到/目标数量 */
    @BindView(R.id.tvFindState)
    TextView tvFindState;

    /** RFID读取器统一接口（厂家适配器由工厂按设备型号创建，本页不感知具体SDK） */
    private IRfidReader rfidReader;
    /** 本轮盘点已读标签EPC集合（去重，线程安全）：每轮开始扫描/重置盘点状态/开启自动标记时清空 */
    private final Set<String> readEpcSet = Collections.synchronizedSet(new HashSet<String>());
    /** 是否正在盘点 */
    private volatile boolean scanning = false;
    /** 连接中用户点击了扫描：连接成功后自动补扫 */
    private volatile boolean pendingScan = false;

    /** 存储权限请求码 */
    private static final int REQ_RFID_STORAGE = 9001;
    /** 定位权限请求码（优博讯BLE枪柄首次搜索需要） */
    private static final int REQ_RFID_LOCATION = 9002;
    /** 找资产设置页请求码 */
    private static final int REQ_FIND_ASSETS = 9003;

    /** 当前找资产条件：null表示未开启，扫描按普通盘点处理 */
    private FindAssetsCriteria findCriteria;
    /** 查找目标总数（人员方式按姓名count查询，指定资产方式取选择时统计值） */
    private int findTargetTotal = 0;
    /** 目标总数是否已统计完成：未完成时状态行先不展示目标数 */
    private boolean findTargetTotalReady = false;

    /** UHF功率(dBm)（从未手动设置过时为适配器默认值：优博讯=设备最大功率，其他厂家33） */
    private int uhfPower;
    /** 用户是否手动设置过功率：从未设置过时连接成功后按适配器默认值同步 */
    private boolean powerUserSet;
    /** 盘点区域：EPC / EPC+TID */
    private int uhfMode;
    /** 盘点会话：S0 / S1 */
    private int uhfSession;
    /** 频率区域：中国 / 欧洲 / 美国 */
    private int uhfRegion;

    /**
     * 页面创建：绑定视图 → 初始化UI → 检查存储权限并连接RFID模块
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_read);
        ButterKnife.bind(this);
        initializeView();
        initRfidReader();
    }

    /**
     * 页面恢复：基类刷新统计与卡片；本页补充适配器恢复（得实连接失败自动重连、东集休眠唤醒重新open由适配器处理）
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (rfidReader != null) {
            rfidReader.onPageResume();
        }
    }

    /**
     * 页面暂停：停止盘点并交由适配器做休眠保护（东集模块休眠断电需close）
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (scanning) {
            stopScan();
        }
        if (rfidReader != null) {
            rfidReader.onPagePause();
        }
    }

    /**
     * 存储权限申请回调（部分机型跳系统设置页授权后经此返回）
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_RFID_STORAGE) {
            onStoragePermissionResult();
        } else if (requestCode == REQ_FIND_ASSETS && resultCode == RESULT_OK) {
            applyFindCriteria(FindAssetsSettingActivity.readResult(data));
        }
    }

    /**
     * 存储/定位权限申请结果回调（Android 6.0+ 运行时授权）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RFID_STORAGE) {
            onStoragePermissionResult();
        } else if (requestCode == REQ_RFID_LOCATION) {
            onLocationPermissionResult();
        }
    }

    /**
     * 存储权限结果处理：授权成功继续走初始化串接（若适配器还需定位权限则接着申请），否则给出提示
     */
    private void onStoragePermissionResult() {
        if (DownloadZrFileManager.hasWritePermission(mContext)) {
            initRfidReader();
        } else {
            showShortText("未授予存储权限，RFID模块无法连接");
        }
    }

    /**
     * 定位权限结果处理：无论是否授予都继续连接——已缓存枪柄MAC时BLE直连不依赖定位权限；
     * 未授予且无缓存MAC时由适配器返回LOCATION_REQUIRED统一提示
     */
    private void onLocationPermissionResult() {
        if (!hasLocationPermission()) {
        }
        connectRfid();
    }

    /** 是否已授予定位权限（targetSdk=30下BLE扫描枪柄依赖） */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 初始化UI：标题栏(含设置入口)、扫描按钮、RFID模块参数；
     * 公共部分（盘点范围参数、顶部统计、标签卡片列表、扫描开关）由基类initScanCommonView完成
     */
    @Override
    public void initializeView() {
        topBar.setTitle("资产盘点");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("设置", R.id.topbar_right_query_button).setOnClickListener(v -> showSettingSheet());

        // 工厂按设备型号创建对应厂家适配器（东集/优博讯/不支持兜底）
        rfidReader = RfidReaderFactory.create(mContext);

        // 读取UHF本地参数（旧版本保存的0x02/0x04/0x08需映射为新SDK取值）
        // 功率：手动设置过（含0，优博讯下限为0）用保存值；从未设置过取适配器默认——
        // 优博讯按文档为设备支持的最大功率（连接成功后再同步实测值），其他厂家33
        int savedPower = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_POWER, -1);
        powerUserSet = savedPower >= 0;
        uhfPower = powerUserSet ? savedPower : rfidReader.getDefaultPower();
        uhfMode = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_MODE, MODE_EPC);
        uhfSession = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_SESSION, 1);
        uhfRegion = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_REGION, REGION_CHN);
        if (uhfRegion > 3) {
            uhfRegion = uhfRegion == 0x04 ? REGION_EU : uhfRegion == 0x08 ? REGION_USA : REGION_CHN;
        }

        // 扫描按钮：开始/停止盘点切换
        buttonScan.setOnClickListener(v -> {
            if (scanning) {
                stopScan();
            } else {
                startScan();
            }
        });

        // 公共业务初始化：盘点范围参数/顶部统计/标签卡片列表/盘点扫描开关
        initScanCommonView();
        updateScanUi();
    }

    /**
     * 盘点扫描开关开启：清空EPC去重集合，本轮已展示但未标记的标签下次上报时补标记盘实
     */
    @Override
    protected void onMarkStatusEnabled() {
        readEpcSet.clear();
    }

    // ==================== 找资产 ====================

    /**
     * 是否处于找资产模式：基类据此静默忽略未入库标签
     */
    @Override
    protected boolean isScanTargetFilterActive() {
        return findCriteria != null && findCriteria.isEnabled();
    }

    /**
     * 扫描目标过滤：找资产模式下只放行匹配查找条件的资产，
     * 非目标返回null由基类静默忽略（不提示、不标记、不加入列表）；
     * 未开启找资产时保持基类原有逻辑
     */
    @Override
    protected Assets selectScanTarget(List<Assets> candidates) {
        if (!isScanTargetFilterActive()) {
            return super.selectScanTarget(candidates);
        }
        for (Assets assets : candidates) {
            if (findCriteria.matches(assets)) {
                return assets;
            }
        }
        return null;
    }

    /** 列表变化（扫描命中/清空）时刷新找资产状态行 */
    @Override
    protected void onScanListChanged() {
        updateFindStateUi();
    }

    /** 打开找资产设置页：携带当前条件，便于回显与修改 */
    private void openFindAssetsSetting() {
        FindAssetsSettingActivity.open(this, REQ_FIND_ASSETS, findCriteria);
    }

    /**
     * 应用找资产设置页回传的条件。
     * <p>
     * 条件发生变化时先停止扫描，再清空卡片列表与两处去重集合：
     * 否则旧目标会继续显示，且新条件的目标可能因EPC/条码已扫过而被去重。
     * 条件未变化时保留当前列表，仅刷新状态行。
     */
    private void applyFindCriteria(FindAssetsCriteria criteria) {
        boolean newEnabled = criteria != null && criteria.isEnabled();
        String oldKey = findCriteria == null ? "" : findCriteria.configKey();
        String newKey = newEnabled ? criteria.configKey() : "";
        findCriteria = newEnabled ? criteria : null;
        if (oldKey.equals(newKey)) {
            refreshFindTargetTotal();
            showShortText(newEnabled ? "查找条件未变化" : "未开启找资产");
            return;
        }
        if (scanning) {
            stopScan();
        }
        readEpcSet.clear();
        clearScannedBarcodeSet();
        tagAdapter.setDataList(new ArrayList<RfidTagAdapter.RfidTagItem>());
        updateScanUi();
        showShortText(newEnabled ? "已开启找资产：" + findCriteria.getModeName() : "已关闭找资产");
        refreshFindTargetTotal();
    }

    /**
     * 刷新查找目标总数：人员方式在后台按姓名count查询，
     * 指定资产方式直接取选择时统计的有条码数量，避免每次开启都查库
     */
    private void refreshFindTargetTotal() {
        final FindAssetsCriteria criteria = findCriteria;
        if (criteria == null) {
            findTargetTotal = 0;
            findTargetTotalReady = true;
            updateFindStateUi();
            return;
        }
        if (criteria.getMode() == FindAssetsCriteria.MODE_TARGET) {
            findTargetTotal = criteria.getScannableTargetCount();
            findTargetTotalReady = true;
            updateFindStateUi();
            return;
        }
        findTargetTotal = 0;
        findTargetTotalReady = false;
        updateFindStateUi();
        AsyncManager.getInstance().submitWithCallback(criteria::countTargets,
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || findCriteria != criteria) {
                        return;
                    }
                    findTargetTotalReady = true;
                    findTargetTotal = result.isSuccess() && result.getData() != null
                            ? result.getData().intValue() : 0;
                    updateFindStateUi();
                }));
    }

    /**
     * 刷新找资产状态行与空列表提示：
     * 开启查找时展示查找方式与已找到/目标数量，列表为空时提示对准目标扫描
     */
    private void updateFindStateUi() {
        if (tvFindState == null) {
            return;
        }
        if (!isScanTargetFilterActive()) {
            tvFindState.setVisibility(View.GONE);
            textEmpty.setText("按下设备RFID按键或点击下方按钮开始扫描\n扫描到的标签将显示在卡片列表中");
            return;
        }
        int found = tagAdapter.getDataList().size();
        String totalText = findTargetTotalReady ? " / 目标 " + findTargetTotal + " 项" : "";
        tvFindState.setText("找资产：" + findCriteria.getSummary() + "\u3000已找到 " + found + totalText);
        tvFindState.setVisibility(View.VISIBLE);
        textEmpty.setText("尚未扫描到目标资产\n请对准目标资产标签扫描");
    }

    /** 设置菜单“找资产”项文案 */
    private String findAssetsSettingText() {
        return "找资产：" + (findCriteria == null ? "未开启" : findCriteria.getSummary());
    }

    /**
     * RFID模块初始化：依次检查存储权限（得实SDK需读校验文件）与定位权限（优博讯BLE枪柄首次搜索），
     * 均满足后连接；权限回调入口重新进入本方法逐级串接
     */
    private void initRfidReader() {

        if (rfidReader.requiresStoragePermission() && !DownloadZrFileManager.hasWritePermission(mContext)) {
            DownloadZrFileManager.requestWritePermission(this, REQ_RFID_STORAGE);
            return;
        }
        if (rfidReader.requiresLocationPermission() && !hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_RFID_LOCATION);
            return;
        }
        connectRfid();
    }

    /** 连接RFID模块：结果经回调onConnectResult异步返回（主线程） */
    private void connectRfid() {
        rfidReader.connect(mContext, rfidCallbacks);
    }

    /** 统一RFID回调（适配器保证主线程投递） */
    private final RfidCallbacks rfidCallbacks = new RfidCallbacks() {
        @Override
        public void onConnectResult(RfidError error) {
            if (error == RfidError.SUCCESS) {
                // 用户从未手动设置过功率：连接成功后按适配器默认值同步（优博讯此时可拿到设备最大功率）
                if (!powerUserSet) {
                    int defaultPower = rfidReader.getDefaultPower();
                    if (defaultPower != uhfPower) {
                        uhfPower = defaultPower;
                    }
                }
                applyRfidConfig();
                if (pendingScan) {
                    pendingScan = false;
                    startScan();
                }
            } else {
                pendingScan = false;
                showShortText(error.getMessage());
            }
        }

        @Override
        public void onTagScanned(RfidTagData tag) {
            handleRfidTag(tag);
        }

        @Override
        public void onHardwareError(RfidError error) {
            if (error == RfidError.START_FAILED) {
                scanning = false;
                updateScanUi();
            }
            showShortText(error.getMessage());
        }

        @Override
        public void onTriggerKeyDown() {
            // 枪柄按键按下=开始扫描（复用startScan门控：连接中暂存pendingScan/未就绪提示）
            if (!scanning) {
                startScan();
            }
        }

        @Override
        public void onTriggerKeyUp() {
            // 枪柄按键抬起=停止扫描
            pendingScan = false;
            if (scanning) {
                stopScan();
            }
        }
    };

    /**
     * 开始盘点：连接中则暂存请求待就绪后补扫；就绪后交由适配器清空标签并启动扫描
     */
    private void startScan() {
        if (!rfidReader.isSupported(mContext)) {
            showShortText("当前设备不支持RFID模块");
            return;
        }
        if (rfidReader.isConnecting()) {
            pendingScan = true;
            showShortText("RFID模块正在初始化，请稍后……");
            return;
        }
        if (!rfidReader.isReady()) {
            showShortText("RFID模块未就绪，请稍后……");
            return;
        }
        if (scanning) {
            return;
        }
        scanning = true;
        // 新一轮扫描：清空EPC去重集合，本轮上报的标签全部重新走展示/标记流程
        // （去重仅用于压制同一次连续盘点内SDK对同一标签的重复上报）
        readEpcSet.clear();
        updateScanUi();
        rfidReader.startScan();
    }

    /**
     * 停止盘点：立即复位状态刷新UI，适配器异步下发停止指令
     */
    private void stopScan() {
        scanning = false;
        pendingScan = false;
        updateScanUi();
        rfidReader.stopScan();
    }

    /**
     * 盘点回调（主线程）：EPC去重 → 判断EPC是否为ASCII编码（是则先转码为条码，不是则直接用EPC原值）
     * → 交由基类公共命中处理（查资产→按开关标记→加入卡片列表→提示音）
     */
    private void handleRfidTag(RfidTagData tag) {
        if (!scanning || tag == null || StringUtils.isNullOrEmpty(tag.epc)) {
            return;
        }
        String epc = tag.epc;
        if (readEpcSet.contains(epc)) {
            return;
        }
        readEpcSet.add(epc);
        // 判断EPC是否为ASCII编码：是则先转码(十六进制→ASCII)为条码再查询；不是则跳过转码，直接用原始EPC值查询
        String barCode = epcHexToBarcode(epc);
        if (StringUtils.isNullOrEmpty(barCode)) {
            barCode = epc.trim();
        }
        handleBarcodeHit(barCode, epc, String.valueOf(tag.rssi));
    }

    /**
     * 判断EPC编码并转码为条码(BARCODEID)：十六进制 → ASCII
     * <p>
     * 具体规则统一维护，
     * 保证RFID盘点页与H5扫码页口径一致。
     */
    private String epcHexToBarcode(String epcHex) {
        return RfidEpcUtils.epcHexToBarcode(epcHex);
    }

    /**
     * 刷新扫描UI：按钮文案 / 扫描状态 / 列表数量与空视图（后两项在基类updateListUi）
     */
    private void updateScanUi() {
        buttonScan.setText(scanning ? "停止扫描" : "开始扫描");
        textScanState.setText(scanning ? "正在扫描周围RFID标签..." : "扫描已停止");
        updateListUi();
    }

    /**
     * 判断是否为设备RFID硬件触发键（按键集合由各厂家适配器维护）
     */
    private boolean isUhfTriggerKey(int keyCode, KeyEvent event) {
        return rfidReader.isTriggerKey(keyCode, event.getScanCode());
    }

    /**
     * 按下设备RFID硬件按键：开始盘点（忽略长按重复事件）
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isUhfTriggerKey(keyCode, event)) {
            if (event.getRepeatCount() == 0) {
                startScan();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 松开设备RFID硬件按键：停止盘点
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isUhfTriggerKey(keyCode, event)) {
            stopScan();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 页面销毁：停止盘点状态复位并断开模块连接（提示音资源由基类释放）
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanning = false;
        pendingScan = false;
        if (rfidReader != null) {
            rfidReader.disconnect();
        }
    }

    // ==================== 设置 ====================

    /**
     * 弹出UHF设置底部菜单：找资产 / 功率 / 盘点区域(仅支持TID的机型) / 会话 / 频率区域 / 提示音 / 重置当前地点盘点状态
     */
    private void showSettingSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("扫描设置")
                .addItem(findAssetsSettingText(), SETTING_FIND)
                .addItem("功率：" + uhfPower + "dBm", SETTING_POWER);
        // 不支持EPC+TID的机型（东集）隐藏盘点区域设置项
        if (rfidReader.supportsTid()) {
            builder.addItem("盘点区域：" + modeName(), SETTING_MODE);
        }
        builder.addItem("会话：" + (uhfSession == 0 ? "S0" : "S1"), SETTING_SESSION)
                .addItem("频率区域：" + regionName(), SETTING_REGION)
                .addItem(scanSoundSettingText(), SETTING_SOUND);
        // 空白地点（临时）模式扫描范围不限，隐藏"重置当前地点盘点状态"避免误重置全量数据
        if (!backupMode) {
            builder.addItem("重置当前地点盘点状态", SETTING_RESET);
        }
        builder.setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
            dialog.dismiss();
            if (SETTING_FIND.equals(tag)) {
                openFindAssetsSetting();
            } else if (SETTING_RESET.equals(tag)) {
                confirmResetScopeStatus();
            } else if (SETTING_POWER.equals(tag)) {
                showPowerDialog();
            } else if (SETTING_MODE.equals(tag)) {
                showModeSheet();
            } else if (SETTING_SESSION.equals(tag)) {
                showSessionSheet();
            } else if (SETTING_REGION.equals(tag)) {
                showRegionSheet();
            } else if (SETTING_SOUND.equals(tag)) {
                toggleScanSound();
            }
        });
        builder.build().show();
    }

    /**
     * 功率设置对话框：输入范围由适配器能力决定（东集5-33dBm，优博讯0~设备最大功率），保存到本地并下发到模块
     */
    private void showPowerDialog() {
        final int[] range = rfidReader.getPowerRange();
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("设置UHF功率(dBm)")
                .setPlaceholder(range[0] + "-" + range[1])
                .setDefaultText(String.valueOf(uhfPower))
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builder.getEditText().getText().toString().trim();
                    dialog.dismiss();
                    if (TextUtils.isEmpty(text)) {
                        return;
                    }
                    try {
                        int power = Integer.parseInt(text);
                        if (power < range[0] || power > range[1]) {
                            showShortText("功率范围" + range[0] + "-" + range[1]);
                            return;
                        }
                        uhfPower = power;
                        powerUserSet = true;
                        SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_POWER, power);
                        applyRfidConfig();
                    } catch (NumberFormatException e) {
                        showShortText("请输入数字");
                    }
                })
                .create().show();
    }

    /**
     * 盘点区域设置：EPC(默认) / EPC+TID，保存并下发
     */
    private void showModeSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择盘点区域")
                .addItem("EPC")
                .addItem("EPC+TID")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    uhfMode = position == 1 ? MODE_EPC_TID : MODE_EPC;
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_MODE, uhfMode);
                    applyRfidConfig();
                })
                .build().show();
    }

    /**
     * 会话设置：S0 / S1，保存并下发
     */
    private void showSessionSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择会话")
                .addItem("S0")
                .addItem("S1")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    uhfSession = position == 1 ? 1 : 0;
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_SESSION, uhfSession);
                    applyRfidConfig();
                })
                .build().show();
    }

    /**
     * 频率区域设置：中国 / 欧洲 / 美国，保存并下发
     */
    private void showRegionSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择频率区域")
                .addItem("中国(920-925MHz)")
                .addItem("欧洲(865-868MHz)")
                .addItem("美国(902-928MHz)")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    switch (position) {
                        case 1:
                            uhfRegion = REGION_EU;
                            break;
                        case 2:
                            uhfRegion = REGION_USA;
                            break;
                        default:
                            uhfRegion = REGION_CHN;
                            break;
                    }
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_REGION, uhfRegion);
                    applyRfidConfig();
                })
                .build().show();
    }

    // ==================== 重置盘点状态 ====================

    /**
     * 重置当前地点盘点状态（设置菜单底部入口）：弹窗确认，防止误触清空整片范围的盘点成果
     */
    private void confirmResetScopeStatus() {
        String deptShow = StringUtils.isNullOrEmpty(deptName) ? "全部部门" : deptName;
        String locationShow = StringUtils.isNullOrEmpty(locationName) ? "全部地点" : locationName;
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("重置盘点状态")
                .setMessage("是否将当前盘点范围（" + deptShow + " / " + locationShow + "）内全部资产的盘点状态重置为未盘点？\n"
                        + "重置数据将置为未同步，待联网上传。")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    resetScopeInventoryStatus();
                })
                .show();
    }

    /**
     * 重置当前范围盘点状态：将范围内(管理部门+存放地点，与顶部统计同一口径)全部资产的
     * 盘点情况(PDZT)与盘点状态(ZT)置为01(待盘点/未盘)，并置ISUPLOAD=02(未同步)
     * <p>
     * 批量更新在AsyncManager后台线程执行并显示转圈提示（数据量可能较大，避免阻塞主线程），
     * 完成后切回主线程：按资产ID以重置后的最新实例替换卡片旧实例再重绘，并刷新顶部统计
     */
    private void resetScopeInventoryStatus() {
        showProgressDialog("重置盘点状态", "正在重置当前地点盘点状态……");
        AsyncManager.getInstance().submitWithCallback(() -> {
            List<Assets> scopeAssets = queryScopeAssets();
            for (Assets assets : scopeAssets) {
                assets.setPDZT("01");
                assets.setZT("01");
                assets.setISUPLOAD(Const.NOT_UPLOAD);
                assets.save();
            }
            return scopeAssets;
        }, result -> mainHandler.post(() -> {
            hideProgressDialog();
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!result.isSuccess()) {
                showShortText("重置失败，请重试");
                return;
            }
            // 重置后清空EPC去重集合：已扫过的标签再次上报时重新标记盘实（否则被去重直接忽略）
            readEpcSet.clear();
            List<Assets> updatedList = result.getData();
            if (updatedList == null || updatedList.isEmpty()) {
                showShortText("当前地点范围内没有资产");
                return;
            }
            // 卡片持有扫描时的旧实例：按资产ID替换为重置后的最新实例再重绘，状态文字与标题颜色联动
            Map<Long, Assets> updatedMap = new HashMap<>();
            for (Assets assets : updatedList) {
                if (assets.getId() != null) {
                    updatedMap.put(assets.getId(), assets);
                }
            }
            for (RfidTagAdapter.RfidTagItem item : tagAdapter.getDataList()) {
                if (item.assets == null || item.assets.getId() == null) {
                    continue;
                }
                Assets fresh = updatedMap.get(item.assets.getId());
                if (fresh != null) {
                    tagAdapter.refreshAssets(fresh);
                }
            }
            // 重置后顶部已盘/未盘/全部数量联动刷新
            refreshScopeStats();
            showShortText("已重置 " + updatedList.size() + " 条资产的盘点状态");
        }));
    }

    /**
     * 应用RFID设置到模块：组装统一配置交给适配器映射下发（未就绪时适配器内部缓存，连接成功/休眠唤醒后重放）
     */
    private void applyRfidConfig() {
        if (rfidReader == null) {
            return;
        }
        RfidConfig config = new RfidConfig();
        config.power = uhfPower;
        config.mode = uhfMode;
        config.session = uhfSession;
        config.region = uhfRegion;
        rfidReader.applyConfig(config);
    }

    /** 盘点区域显示名 */
    private String modeName() {
        return uhfMode == MODE_EPC_TID ? "EPC+TID" : "EPC";
    }

    /** 频率区域显示名 */
    private String regionName() {
        switch (uhfRegion) {
            case REGION_EU:
                return "欧洲(865-868MHz)";
            case REGION_USA:
                return "美国(902-928MHz)";
            default:
                return "中国(920-925MHz)";
        }
    }
}