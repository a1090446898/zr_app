package com.gsoft.inventory;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidReaderFactory;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.CountValueHelper;
import com.gsoft.inventory.utils.DownloadZrFileManager;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RFID盘点扫描页（按存放地点盘点）
 * <p>
 * 功能说明：
 * 1. 由盘点统计页点击存放地点进入，携带管理部门+存放地点两个过滤条件，避免多部门同地点数据串乱
 * 2. 进入页面先查询并渲染当前范围(管理部门+存放地点)的全部资产卡片；用户操作RFID设备盘点，
 *    扫描到存放地点不一致的资产时逐条新增卡片
 * 3. 顶部卡片展示当前盘点范围(管理部门/存放地点)与已盘数量(绿)/未盘数量(红)/全部资产数量(黑)，盘点后实时联动刷新
 * 4. 顶部"盘点扫描开关"默认关闭：关闭时扫描仅展示卡片、不修改任何状态；开启后扫描命中自动标记
 *    盘点情况=盘实(PDZT=02)、盘点状态=已盘(ZT=02)，并置ISUPLOAD=02(未同步)
 * 5. 标签卡片展示：资产名称/条码编号/管理部门/存放地点/盘点状态/盘点情况，底部为"修改"与"一键变更"按钮
 *    - 存放地点与当前盘点地点不一致：标题红色(即使已盘也红)并显示红色提示与"一键变更"按钮
 *    - 存放地点一致：已盘(ZT=02)标题绿色，未盘标题黑色
 * 6. "修改"按钮跳转资产变更页；"一键变更"按钮弹窗确认后将该资产存放地点改为当前盘点地点，
 *    并置ISUPLOAD=02(未同步)后同步刷新统计
 * 7. 所有变更(盘点状态/存放地点)均设置ISUPLOAD=02(未同步)，待联网上传
 * 8. 硬件访问经统一接口IRfidReader由厂家适配器实现（得实com.hardware.rfid / 东集com.seuic.uhf），
 *    本页保留设备按键触发盘点、功率/区域/会话/频率设置等业务交互
 */
public class RfidReadActivity extends BaseCompatActivity {

    private static final String TAG = "RfidReadActivity";

    /** 盘点区域 */
    public static final int MODE_EPC = RfidConfig.MODE_EPC;
    public static final int MODE_EPC_TID = RfidConfig.MODE_EPC_TID;
    /** 频率区域 */
    public static final int REGION_CHN = RfidConfig.REGION_CHN;           // 中国920~925MHz
    public static final int REGION_EU = RfidConfig.REGION_EU;             // 欧洲865~868MHz
    public static final int REGION_USA = RfidConfig.REGION_USA;           // 美国902~928MHz

    /** Intent参数：管理部门名称（与盘点统计页一致，来自GROUNDMANAGEDEPT） */
    public static final String EXTRA_DEPT_NAME = "extra_dept_name";
    /** Intent参数：存放地点名称（"未设置存放地点"表示地点为空） */
    public static final String EXTRA_LOCATION = "extra_location";

    // ==================== 【临时测试】模拟数字开关：验证已盘/未盘/全部显示（测试完成后删除本区域） ====================
    /** 【临时测试】true=顶部三列渲染模拟数字，false=恢复真实统计 */
    private static final boolean TEST_COUNT_DISPLAY = false;
    /** 【临时测试】模拟已盘数量（超过十） */
    private static final int TEST_COUNTED = 300;
    /** 【临时测试】模拟全部数量（未盘=全部-已盘，自动为42） */
    private static final int TEST_TOTAL = 400;

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 盘点范围提示：管理部门/存放地点 */
    @BindView(R.id.tvContext)
    TextView tvContext;
    /** 当前范围已盘数量（绿色显示，盘点后实时刷新） */
    @BindView(R.id.tvCounted)
    TextView tvCounted;
    /** 当前范围未盘数量（红色显示，=全部-已盘，盘点后实时刷新） */
    @BindView(R.id.tvUncounted)
    TextView tvUncounted;
    /** 当前范围全部资产数量（黑色显示） */
    @BindView(R.id.tvTotal)
    TextView tvTotal;
    /** 扫描状态文案 */
    @BindView(R.id.textScanState)
    TextView textScanState;
    /** 已识别标签数量 */
    @BindView(R.id.textTagCount)
    TextView textTagCount;
    /** 空视图提示 */
    @BindView(R.id.textEmpty)
    TextView textEmpty;
    /** 标签卡片列表 */
    @BindView(R.id.recyclerTags)
    RecyclerView recyclerTags;
    /** 开始/停止扫描按钮 */
    @BindView(R.id.buttonScan)
    QMUIButton buttonScan;
    /** 盘点扫描开关：默认关闭，开启后扫描命中才修改盘点状态(PDZT/ZT)并置未同步 */
    @BindView(R.id.switchMarkStatus)
    Switch switchMarkStatus;

    /** RFID读取器统一接口（厂家适配器由工厂按设备型号创建，本页不感知具体SDK） */
    private IRfidReader rfidReader;
    /** 标签列表适配器（仅主线程更新） */
    private final RfidTagAdapter tagAdapter = new RfidTagAdapter();
    /** 本次盘点已读标签EPC集合（去重用，线程安全） */
    private final Set<String> readEpcSet = Collections.synchronizedSet(new HashSet<String>());
    /** 是否正在盘点 */
    private volatile boolean scanning = false;
    /** 连接中用户点击了扫描：连接成功后自动补扫 */
    private volatile boolean pendingScan = false;

    /** 当前盘点管理部门名称（过滤条件，空表示不过滤部门） */
    private String deptName = "";
    /** 当前盘点存放地点名称（过滤条件与展示用，"未设置存放地点"表示地点为空） */
    private String locationName = "";

    /** 当前盘点部门的部门编号BMBH（进入页面时缓存，查询与卡片判断用） */
    private String deptCodeValue = "";
    /** 当前盘点部门的单位编号DWBH（进入页面时缓存，查询与卡片判断用） */
    private String unitCodeValue = "";

    /** 盘点扫描开关状态（主线程经监听器更新，扫描线程读取）：开启后扫描命中才修改盘点状态 */
    private volatile boolean markStatusOn = false;

    /** 扫到标签提示音（复用单个ToneGenerator，随页面销毁释放） */
    private ToneGenerator toneGenerator;
    /** 上次提示音播放时间戳，用于200ms节流 */
    private long lastBeepTime = 0;

    /** 存储权限请求码 */
    private static final int REQ_RFID_STORAGE = 9001;

    /** UHF功率(dBm) */
    private int uhfPower;
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
     * 页面恢复：刷新盘点统计与卡片状态；得实连接失败(1~3次)自动重连、东集休眠唤醒重新open由适配器处理
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 从资产修改页返回或扫描标记后，重新统计当前范围已盘/全部数量并刷新卡片颜色
        refreshScopeStats();
        tagAdapter.refreshAll();
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
        }
    }

    /**
     * 存储权限申请结果回调（Android 6.0+ 运行时授权）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RFID_STORAGE) {
            onStoragePermissionResult();
        }
    }

    /**
     * 存储权限结果处理：授权成功继续连接RFID模块，否则给出提示
     */
    private void onStoragePermissionResult() {
        if (DownloadZrFileManager.hasWritePermission(mContext)) {
            connectRfid();
        } else {
            showShortText("未授予存储权限，RFID模块无法连接");
        }
    }

    /**
     * 初始化UI：读取盘点范围参数、标题栏(含设置入口)、统计卡片、标签列表、扫描按钮事件
     */
    @Override
    public void initializeView() {
        // 读取盘点统计页传入的管理部门与存放地点（过滤条件）
        deptName = getIntent().getStringExtra(EXTRA_DEPT_NAME);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION);
        if (StringUtils.isNullOrEmpty(deptName)) {
            deptName = "";
        }
        if (StringUtils.isNullOrEmpty(locationName)) {
            locationName = "";
        }

        // 预计算部门编号（GROUNDMANAGEDEPT字段中编号与名称并存，查询与卡片判断需两者兼容，避免每次扫描都查表）
        if (!StringUtils.isNullOrEmpty(deptName) && !RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            String ztbh = application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH();
            if (!StringUtils.isNullOrEmpty(ztbh)) {
                deptCodeValue = AccountHelper.getDepartCode(ztbh, deptName);
                if (deptCodeValue == null) {
                    deptCodeValue = "";
                }
            }
            unitCodeValue = AccountHelper.getDanweiCode(deptName);
            if (unitCodeValue == null) {
                unitCodeValue = "";
            }
        }

        topBar.setTitle("RFID盘点");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("设置", R.id.topbar_right_query_button).setOnClickListener(v -> showSettingSheet());

        // 盘点范围展示：管理部门 / 存放地点
        String deptShow = StringUtils.isNullOrEmpty(deptName) ? "全部部门" : deptName;
        String locationShow = StringUtils.isNullOrEmpty(locationName) ? "全部地点" : locationName;
        tvContext.setText("盘点范围：" + deptShow + " / " + locationShow);

        // 盘点扫描开关默认关闭：关闭时扫描仅展示卡片，不修改盘点状态；开关状态经volatile字段供扫描线程读取
        switchMarkStatus.setChecked(false);
        switchMarkStatus.setOnCheckedChangeListener((buttonView, isChecked) -> markStatusOn = isChecked);

        // 读取UHF本地参数（旧版本保存的0x02/0x04/0x08需映射为新SDK取值）
        uhfPower = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_POWER, 26);
        uhfMode = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_MODE, MODE_EPC);
        uhfSession = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_SESSION, 1);
        uhfRegion = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_REGION, REGION_CHN);
        if (uhfRegion > 3) {
            uhfRegion = uhfRegion == 0x04 ? REGION_EU : uhfRegion == 0x08 ? REGION_USA : REGION_CHN;
        }

        // 工厂按设备型号创建对应厂家适配器（东集/得实/不支持兜底）
        rfidReader = RfidReaderFactory.create(mContext);

        // 标签卡片列表：设置当前盘点地点（供卡片判断地点是否一致）与部门取值（供卡片判断部门是否一致）
        recyclerTags.setLayoutManager(new LinearLayoutManager(mContext));
        tagAdapter.setCurrentLocation(locationName);
        tagAdapter.setCurrentDept(deptName, deptCodeValue, unitCodeValue);
        recyclerTags.setAdapter(tagAdapter);
        tagAdapter.setOnCardActionListener(new RfidTagAdapter.OnCardActionListener() {
            @Override
            public void onItemClick(RfidTagAdapter.RfidTagItem item) {
                // 点击卡片：已入库资产跳转资产详情，未入库标签仅提示
                if (item.assets == null) {
                    showShortText("该标签未在资产台账中登记");
                    return;
                }
                Intent intentView = new Intent(mContext, AssetsViewActivity.class);
                intentView.putExtra(SysDefine.IntentExtra_AssetsID, item.assets.getId());
                startActivity(intentView);
            }

            @Override
            public void onModifyClick(RfidTagAdapter.RfidTagItem item) {
                // 修改按钮：跳转资产变更页编辑该资产
                if (item.assets == null || item.assets.getId() == null) {
                    showShortText("资产信息异常，无法修改");
                    return;
                }
                // 强制转基本类型long传参（避免Long走Serializable重载导致目标页读不到ID）
                long assetsId = item.assets.getId();
                Log.i(TAG, "点击修改按钮，跳转资产变更页 assetsId=" + assetsId);
                Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assetsId);
                startActivity(intentEdit);
            }

            @Override
            public void onChangeLocationClick(RfidTagAdapter.RfidTagItem item) {
                // 一键变更按钮：将该资产存放地点改为当前盘点地点
                changeLocation(item);
            }
        });

        // 扫描按钮：开始/停止盘点切换
        buttonScan.setOnClickListener(v -> {
            if (scanning) {
                stopScan();
            } else {
                startScan();
            }
        });
        updateScanUi();
        // 进入页面先把当前范围(管理部门+存放地点)的资产全部渲染为卡片，并统计已盘/全部数量
        loadScopeCards();
    }

    /**
     * 一键变更：弹窗确认后将该资产存放地点修改为当前盘点地点，并置未同步(ISUPLOAD=02)
     * <p>
     * 变更后该资产归属当前盘点范围：刷新顶部已盘/全部统计，并重绘该资产卡片
     * （标题颜色、提示文字、"一键变更"按钮可见性随之联动）
     */
    private void changeLocation(RfidTagAdapter.RfidTagItem item) {
        if (item.assets == null) {
            return;
        }
        // 弹窗确认：确定后才修改存放地点，防止误触
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("变更存放地点")
                .setMessage("是否将该资产存放地点修改为当前存放地点：" + locationName + "？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    // 当前盘点地点为"未设置存放地点"时，数据库存空字符串
                    String storeLocation = RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName) ? "" : locationName;
                    item.assets.setASSETSLAYADD(storeLocation);
                    // 存放地点变更后置为未同步，待联网上传
                    item.assets.setISUPLOAD("02");
                    item.assets.save();
                    // 地点变更后资产归属当前范围，重新统计并刷新卡片显示
                    refreshScopeStats();
                    tagAdapter.refreshAssets(item.assets);
                    showShortText("已变更存放地点：" + locationName);
                })
                .show();
    }

    /**
     * 刷新当前范围盘点统计：已盘数量(绿)/未盘数量(红)/全部资产数量(黑)
     * <p>
     * 查询条件为管理部门(GROUNDMANAGEDEPT)+存放地点(ASSETSLAYADD)，
     * 与盘点统计页传入的两个过滤条件一致，避免多部门同地点时数据串乱
     */
    private void refreshScopeStats() {
        List<Assets> scopeAssets = queryScopeAssets();
        int counted = 0;
        for (Assets assets : scopeAssets) {
            if ("02".equals(assets.getZT())) {
                counted++;
            }
        }
        int total = scopeAssets.size();
        // 【临时测试】开启后用模拟数字覆盖真实统计，验证三列显示（测试完成后删除本if块）
        if (TEST_COUNT_DISPLAY) {
            counted = TEST_COUNTED;
            total = TEST_TOTAL;
        }
        // 千分位显示并按最大位数统一字号
        int uncounted = total - counted;
        int maxDigits = CountValueHelper.maxDigitsOf(counted, uncounted, total);
        CountValueHelper.showTopCardCount(tvCounted, counted, maxDigits);
        CountValueHelper.showTopCardCount(tvUncounted, uncounted, maxDigits);
        CountValueHelper.showTopCardCount(tvTotal, total, maxDigits);
    }

    /**
     * 查询当前盘点范围的资产
     * <p>
     * 组合管理部门与存放地点条件：
     * "未分配部门"/"未设置存放地点"为占位名，需匹配数据库中的空值(NULL或空串)
     *
     * @return 当前范围的资产列表
     */
    private List<Assets> queryScopeAssets() {
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        // 管理部门条件（字段中编号与中文名称并存，两种历史取值都兼容；TRIM去除首尾空格避免漏查）
        if (!StringUtils.isNullOrEmpty(deptName)) {
            if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
                // 未分配部门：匹配数据库空值
                whereParts.add("(GROUNDMANAGEDEPT IS NULL OR TRIM(GROUNDMANAGEDEPT) = '')");
            } else {
                List<String> orParts = new ArrayList<>();
                orParts.add("TRIM(GROUNDMANAGEDEPT) = ?");
                args.add(deptName);
                if (!StringUtils.isNullOrEmpty(deptCodeValue) && !deptCodeValue.equals(deptName)) {
                    orParts.add("TRIM(GROUNDMANAGEDEPT) = ?");
                    args.add(deptCodeValue);
                }
                if (!StringUtils.isNullOrEmpty(unitCodeValue) && !unitCodeValue.equals(deptName)) {
                    orParts.add("TRIM(GROUNDMANAGEDEPT) = ?");
                    args.add(unitCodeValue);
                }
                whereParts.add("(" + TextUtils.join(" OR ", orParts) + ")");
            }
        }
        // 存放地点条件（地点为纯字符值，直接匹配；TRIM去除首尾空格，与统计页分组口径一致）
        if (!StringUtils.isNullOrEmpty(locationName)) {
            if (RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName)) {
                // 未设置存放地点：匹配数据库空值
                whereParts.add("(ASSETSLAYADD IS NULL OR TRIM(ASSETSLAYADD) = '')");
            } else {
                whereParts.add("TRIM(ASSETSLAYADD) = ?");
                args.add(locationName);
            }
        }
        // 未携带任何条件时查询全部（兼容直接进入页面的情况）
        if (whereParts.isEmpty()) {
            return Assets.listAll(Assets.class);
        }
        String where = TextUtils.join(" AND ", whereParts);
        return Assets.find(Assets.class, where, args.toArray(new String[0]));
    }

    /**
     * RFID模块初始化：connect前先检查存储权限（得实SDK需读校验文件，未授权时错误码-3；东集无需）
     */
    private void initRfidReader() {
        if (rfidReader.requiresStoragePermission() && !DownloadZrFileManager.hasWritePermission(mContext)) {
            Log.w(TAG, "存储权限未授予，先申请权限再连接RFID模块");
            DownloadZrFileManager.requestWritePermission(this, REQ_RFID_STORAGE);
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
            Log.e(TAG, "RFID硬件错误:" + error);
            if (error == RfidError.START_FAILED) {
                scanning = false;
                updateScanUi();
            }
            showShortText(error.getMessage());
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
     * 盘点回调（主线程）：EPC去重 → 十六进制转条码(BARCODEID) → 查资产 → 开关开启时自动标记盘实/已盘 → 加入卡片列表
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
        String barCode = epcHexToBarcode(epc);
        if (StringUtils.isNullOrEmpty(barCode)) {
            Log.w(TAG, "EPC解码条码失败，已忽略 epc=" + epc);
            return;
        }
        List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=?", barCode);
        if (assetsList.isEmpty()) {
            Log.i(TAG, "BARCODEID未在资产台账中登记，已忽略 barCode=" + barCode);
            return;
        }
        Assets hitAssets = assetsList.get(0);
        // 盘点扫描开关开启时才自动标记：盘点情况=盘实、盘点状态=已盘，并置未同步；关闭时仅展示卡片不改状态
        if (markStatusOn) {
            hitAssets.setPDZT("02");
            hitAssets.setZT("02");
            hitAssets.setISUPLOAD("02");
            hitAssets.save();
            Log.i(TAG, "已标记盘实/已盘并置未同步 barCode=" + barCode);
        }
        RfidTagAdapter.RfidTagItem item = new RfidTagAdapter.RfidTagItem();
        item.epc = epc;
        item.barcode = barCode;
        item.rssi = String.valueOf(tag.rssi);
        item.assets = hitAssets;
        // 回调已在主线程：顶部已盘数量与卡片颜色同步刷新
        refreshScopeStats();
        tagAdapter.refreshAssets(hitAssets);
        // 该资产尚未在列表中（即扫描到存放地点不一致的资产）时才新增卡片，避免重复
        if (!tagAdapter.containsAssets(hitAssets)) {
            tagAdapter.addItem(item);
        }
        updateScanUi();
        playScanBeep();
    }

    /**
     * 扫到标签提示音：短哔声，200ms节流避免连续标签刷成噪声
     */
    private void playScanBeep() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBeepTime < 200) {
            return;
        }
        lastBeepTime = now;
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        } catch (Exception e) {
            Log.e(TAG, "播放提示音失败", e);
        }
    }

    /**
     * EPC十六进制 → ASCII条码(BARCODEID)
     * 盘点读回的EPC可能带PC字头(前4位hex，仅当总长超32位时去除)，按字节转ASCII并去除尾部零填充
     */
    private String epcHexToBarcode(String epcHex) {
        try {
            String hex = epcHex.trim();
            if (hex.length() % 2 != 0 || hex.length() < 2) {
                return null;
            }
            if (hex.length() > 32) {
                hex = hex.substring(4);
            }
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            int len = bytes.length;
            while (len > 0 && bytes[len - 1] == 0) {
                len--;
            }
            if (len == 0) {
                return null;
            }
            String barCode = new String(bytes, 0, len).trim();
            return barCode.matches("[\\x20-\\x7E]+") ? barCode : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 进入页面时先把当前范围(管理部门+存放地点)的资产全部查询出来渲染为卡片，
     * 并同步统计顶部已盘/全部数量；随后用户盘点扫描到地点不一致的资产时再逐条新增卡片
     */
    private void loadScopeCards() {
        List<Assets> scopeAssets = queryScopeAssets();
        List<RfidTagAdapter.RfidTagItem> items = new ArrayList<>();
        for (Assets assets : scopeAssets) {
            RfidTagAdapter.RfidTagItem item = new RfidTagAdapter.RfidTagItem();
            item.assets = assets;
            items.add(item);
        }
        tagAdapter.setDataList(items);
        updateScanUi();
        refreshScopeStats();
    }

    /**
     * 刷新扫描UI：按钮文案 / 扫描状态 / 列表数量 / 空视图
     */
    private void updateScanUi() {
        buttonScan.setText(scanning ? "停止扫描" : "开始扫描");
        textScanState.setText(scanning ? "正在扫描周围RFID标签..." : "扫描已停止");
        textTagCount.setText("列表共 " + tagAdapter.getDataList().size() + " 条资产");
        textEmpty.setVisibility(tagAdapter.getDataList().isEmpty() ? View.VISIBLE : View.GONE);
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
        Log.i(TAG, "onKeyDown keyCode=" + keyCode + " scanCode=" + event.getScanCode());
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
     * 页面销毁：释放提示音资源，并交由适配器停止盘点、断开模块连接，避免串口资源泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanning = false;
        pendingScan = false;
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (rfidReader != null) {
            rfidReader.disconnect();
        }
    }

    // ==================== 设置 ====================

    /**
     * 弹出UHF设置底部菜单：功率 / 盘点区域(仅支持TID的机型) / 会话 / 频率区域
     */
    private void showSettingSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("UHF扫描设置")
                .addItem("功率：" + uhfPower + "dBm");
        // 不支持EPC+TID的机型（东集）隐藏盘点区域设置项
        if (rfidReader.supportsTid()) {
            builder.addItem("盘点区域：" + modeName());
        }
        builder.addItem("会话：" + (uhfSession == 0 ? "S0" : "S1"))
                .addItem("频率区域：" + regionName());
        builder.setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
            dialog.dismiss();
            if (position == 0) {
                showPowerDialog();
            } else if (!rfidReader.supportsTid()) {
                if (position == 1) {
                    showSessionSheet();
                } else {
                    showRegionSheet();
                }
            } else {
                if (position == 1) {
                    showModeSheet();
                } else if (position == 2) {
                    showSessionSheet();
                } else {
                    showRegionSheet();
                }
            }
        });
        builder.build().show();
    }

    /**
     * 功率设置对话框：输入范围由适配器能力决定（得实0-30dBm，东集5-33dBm），保存到本地并下发到模块
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