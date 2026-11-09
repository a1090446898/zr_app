package com.gsoft.inventory.abs;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

import com.gsoft.inventory.AssetsViewActivity;
import com.gsoft.inventory.EditAssetsActivity;
import com.gsoft.inventory.InventoryStatsActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.CountValueHelper;
import com.gsoft.inventory.utils.ScanSoundPlayer;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

/**
 * 盘点扫描页公共基类（RFID扫描页/红外线扫描页共用）
 * <p>
 * 承载与识别方式无关的公共业务：
 * 1. 盘点范围参数（管理部门+存放地点）解析与部门编号预计算
 * 2. 顶部三列统计（已盘/未盘/全部）查询刷新与点击下钻明细页；
 *    存放地点层统计只按存放地点，不加入部门条件，
 *    并在统计数字下方以文字展示"非拥有产权资产"（管理部门与当前部门不一致）的已盘/未盘数量
 * 3. 标签卡片列表初始化与交互（点击详情/修改/一键变更）
 * 4. 一键变更（存放地点/跨部门归入当前盘点范围）与二次确认跳转
 * 5. 条码命中处理（查库→按开关标记盘实/已盘→刷新统计与卡片→提示音）——两种识别方式的公共出口
 * <p>
 * 统计性能：顶部数字走count查询，扫描命中标记成功时走内存增量更新，不重复查询数据库
 * <p>
 * 子类仅需实现识别源差异（RFID适配器/红外广播）与各自UI：
 * setContentView + ButterKnife.bind 后调用 initScanCommonView() 完成公共初始化
 */
public abstract class InventoryScanBaseActivity extends BaseCompatActivity {

    private static final String TAG = "InventoryScanBase";

    /**
     * 连续扫描判定间隔（毫秒）：相邻两次有效命中间隔小于该值视为"连续快扫"，
     * 首次命中的成功语音降级为短哔，避免扫描数量大时整句语音连续不断
     */
    private static final long DENSE_SCAN_GAP_MS = 1500L;
    /** 短哔最小间隔（毫秒）：连扫时避免哔声叠成噪声 */
    private static final long BEEP_MIN_INTERVAL_MS = 200L;

    /** Intent参数：管理部门名称（与盘点统计页一致，来自GROUNDMANAGEDEPT） */
    public static final String EXTRA_DEPT_NAME = "extra_dept_name";
    /** Intent参数：存放地点名称（"未设置存放地点"表示地点为空） */
    public static final String EXTRA_LOCATION = "extra_location";
    /** Intent参数：空白地点（临时）模式，true时扫描不限部门/存放地点，卡片支持手动变更地点 */
    public static final String EXTRA_BACKUP_MODE = "extra_backup_mode";

    /** 盘点范围提示：管理部门/存放地点 */
    @BindView(R.id.tvContext)
    public TextView tvContext;
    /** 当前范围已盘数量（绿色显示，盘点后实时刷新） */
    @BindView(R.id.tvCounted)
    public TextView tvCounted;
    /** 当前范围未盘数量（红色显示，=全部-已盘，盘点后实时刷新） */
    @BindView(R.id.tvUncounted)
    public TextView tvUncounted;
    /** 当前范围全部资产数量（黑色显示） */
    @BindView(R.id.tvTotal)
    public TextView tvTotal;
    /** 非拥有产权资产统计文字（仅存放地点层显示：管理部门与当前部门不一致的资产数量） */
    @BindView(R.id.tvNonOwnedAssets)
    public TextView tvNonOwnedAssets;
    /** 扫描状态文案 */
    @BindView(R.id.textScanState)
    public TextView textScanState;
    /** 已识别标签数量 */
    @BindView(R.id.textTagCount)
    public TextView textTagCount;
    /** 空视图提示 */
    @BindView(R.id.textEmpty)
    public TextView textEmpty;
    /** 标签卡片列表 */
    @BindView(R.id.recyclerTags)
    public RecyclerView recyclerTags;
    /** 盘点扫描开关：默认关闭，开启后扫描命中才修改盘点状态(PDZT/ZT)并置未同步 */
    @BindView(R.id.switchMarkStatus)
    public Switch switchMarkStatus;

    /** 标签列表适配器（仅主线程更新） */
    protected final RfidTagAdapter tagAdapter = new RfidTagAdapter();
    /** 盘点扫描开关状态（扫描线程读取）：开启后扫描命中才修改盘点状态 */
    protected volatile boolean markStatusOn = false;

    /** 当前统计范围缓存：全部资产数（count查询，扫描命中时增量维护） */
    private int scopeTotal = 0;
    /** 当前统计范围缓存：已盘数量 */
    private int scopeCounted = 0;
    /** 当前统计范围缓存：非拥有产权资产总数（仅存放地点层统计） */
    private int scopeNonOwnedTotal = 0;
    /** 当前统计范围缓存：非拥有产权资产已盘数量 */
    private int scopeNonOwnedCounted = 0;
    /** 统计缓存是否已初始化（未初始化时扫描命中走全量刷新） */
    private boolean scopeStatsReady = false;

    /** 当前盘点管理部门名称（过滤条件，空表示不过滤部门） */
    protected String deptName = "";
    /** 当前盘点存放地点名称（过滤条件与展示用，"未设置存放地点"表示地点为空） */
    protected String locationName = "";
    /** 是否空白地点（临时）模式：扫描范围不设限，隐藏统计并支持手动指定存放地点 */
    protected boolean backupMode = false;

    /** 当前盘点部门的部门编号BMBH（进入页面时缓存，查询与卡片判断用） */
    protected String deptCodeValue = "";
    /** 当前盘点部门的单位编号DWBH（进入页面时缓存，查询与卡片判断用） */
    protected String unitCodeValue = "";

    /** 扫到标签提示音（复用单个ToneGenerator，随页面销毁释放） */
    private ToneGenerator toneGenerator;
    /** 上次短哔播放时间戳，用于最小间隔节流 */
    private long lastBeepTime = 0;

    /** 扫描语音提示播放器：扫描成功(saomiaochenggong)/重复(chongfu)，随页面销毁释放 */
    private ScanSoundPlayer scanSoundPlayer;
    /** 扫描提示音开关（默认开启，三页共用，持久化到本地） */
    protected boolean scanSoundEnabled = true;

    /** 最近一次有效命中时间（含重复命中，SystemClock.elapsedRealtime），用于连扫降级判定 */
    private long lastHitElapsed = 0L;
    /** 本次页面会话已扫到的资产条码集合（重复语音判定口径：首次播成功音，同一会话再次扫到才播重复） */
    private final Set<String> scannedBarcodeSet = Collections.synchronizedSet(new HashSet<String>());

    /** 最近一次未登记标签提示时间（全局节流：不区分条码，避免快速扫描时提示常驻） */
    private long lastUnknownTipTime = 0L;
    /** 未登记标签提示Toast（复用并取消上一条，避免快速扫描时提示排队） */
    private Toast unknownTagToast;

    /**
     * 公共初始化：子类在 setContentView/ButterKnife.bind 与各自专属初始化完成后调用
     * <p>
     * 读取盘点范围参数 → 展示范围与统计卡片 → 初始化扫描开关 → 初始化标签列表
     * → 首轮统计当前范围（列表默认不显示资产数据，扫描到标签后再逐条新增卡片）
     */
    protected void initScanCommonView() {
        // 读取盘点统计页传入的管理部门与存放地点（过滤条件）
        deptName = getIntent().getStringExtra(EXTRA_DEPT_NAME);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION);
        backupMode = getIntent().getBooleanExtra(EXTRA_BACKUP_MODE, false);
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

        // 盘点范围展示：普通模式为管理部门/存放地点；
        // 空白地点（临时）模式扫描范围不设限，隐藏顶部三列统计并单独提示范围
        if (backupMode) {
            tvContext.setText("盘点范围：" + Const.BACKUP_LOCATION_NAME);
            hideScopeStatsColumns();
            // 空白地点模式不做部门口径统计，隐藏非拥有产权资产文字
            if (tvNonOwnedAssets != null) {
                tvNonOwnedAssets.setVisibility(View.GONE);
            }
        } else {
            String deptShow = StringUtils.isNullOrEmpty(deptName) ? "全部部门" : deptName;
            String locationShow = StringUtils.isNullOrEmpty(locationName) ? "全部地点" : locationName;
            tvContext.setText("盘点范围：" + deptShow + " / " + locationShow);
        }

        // 盘点扫描开关默认关闭：关闭时扫描仅展示卡片，不修改盘点状态；开启时子类可清空各自去重集合
        switchMarkStatus.setChecked(false);
        switchMarkStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            markStatusOn = isChecked;
            if (isChecked) {
                // 开启自动标记视为新一轮盘点：清空会话判重集合，
                // 之前仅展示未标记的资产再次扫到时应按首次命中处理（标记并播成功音），而非误判"重复"
                scannedBarcodeSet.clear();
                onMarkStatusEnabled();
            }
        });

        // 初始化扫描语音提示器（已盘/重复语音）：页面创建时预加载，随 onDestroy 释放
        // 注意：此前只在 onDestroy/设置开关里引用，缺少构造导致命中标签时 NPE 闪退
        scanSoundEnabled = SharedPreferencesUtils.getBoolean(
                mContext, SharedPreferencesUtils.KEY_SCAN_SOUND_ENABLED, true);
        if (scanSoundPlayer == null) {
            scanSoundPlayer = new ScanSoundPlayer(getApplicationContext());
        }
        scanSoundPlayer.setEnabled(scanSoundEnabled);

        // 顶部三列统计点击：下钻盘点明细页查看对应数据（存放地点层只带地点，部门层带部门）
        findViewById(R.id.columnCounted).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_COUNTED));
        findViewById(R.id.columnUncounted).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_UNCOUNTED));
        findViewById(R.id.columnAll).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_ALL));

        // 批量变更地点：位于标签列表下方，将本次扫描的标签统一变更存放地点
        // 注意：该按钮仅RFID/摄像头布局提供，红外页没有此入口，必须判空，
        // 否则红外页启动时findViewById返回null，抛出NPE导致Activity创建失败闪退
        View btnBatchChange = findViewById(R.id.btnBatchChangeLocation);
        if (btnBatchChange != null) {
            btnBatchChange.setOnClickListener(v -> batchChangeLocation());
        }

        // 标签卡片列表：设置当前盘点地点（供卡片判断地点是否一致）与部门取值（供卡片判断部门是否一致）
        recyclerTags.setLayoutManager(new LinearLayoutManager(mContext));
        tagAdapter.setCurrentLocation(locationName);
        tagAdapter.setCurrentDept(deptName, deptCodeValue, unitCodeValue);
        // 空白地点（临时）模式：卡片不判断地点/部门，隐藏一键变更并显示"变更地点"按钮
        tagAdapter.setBackupMode(backupMode);
        // 管理部门字段存单位编号时按账套转中文名称显示（与资产详情页/编辑页同一方案）
        tagAdapter.setAccountZtbh(application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH());
        recyclerTags.setAdapter(tagAdapter);
        tagAdapter.setOnCardActionListener(new RfidTagAdapter.OnCardActionListener() {
            @Override
            public void onItemClick(RfidTagAdapter.RfidTagItem item) {
                // 点击卡片：已入库资产跳转资产详情，未入库标签仅提示
                if (item.assets == null) {
                    showShortText("该标签未查询到");
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
                Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assetsId);
                startActivity(intentEdit);
            }

            @Override
            public void onChangeLocationClick(RfidTagAdapter.RfidTagItem item) {
                // 一键变更按钮：弹窗确认后将资产归入当前盘点范围，随后二次确认是否继续变更管理人/使用人
                changeLocation(item);
            }

            @Override
            public void onManualLocationClick(RfidTagAdapter.RfidTagItem item) {
                // 变更地点按钮（空白地点模式）：弹框确认后手动输入存放地点并保存（同步修改使用部门）
                changeLocationManually(item);
            }
        });

        // 列表默认不显示资产数据：进入页面仅统计当前范围（地点层只按地点）的已盘/全部数量
        refreshScopeStats();
    }

    /**
     * 盘点扫描开关开启回调：子类可覆写清空各自去重集合
     * （如RFID页清空EPC去重集合，让本轮已展示未标记的标签下次上报时补标记）
     */
    protected void onMarkStatusEnabled() {
    }

    /**
     * 页面恢复：从资产修改页返回或扫描标记后，重新统计当前范围已盘/全部数量并刷新卡片颜色
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshScopeStats();
        tagAdapter.refreshAll();
    }

    /**
     * 页面销毁：释放提示音资源（子类还需在各自onDestroy中释放识别源）
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanSoundPlayer != null) {
            scanSoundPlayer.release();
            scanSoundPlayer = null;
        }
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    /**
     * 顶部统计点击下钻：跳转盘点明细页查看对应数据
     * <p>
     * 存放地点层只带存放地点条件（不加入部门），与顶部统计口径一致；
     * 部门层带当前部门条件（"未分配部门"占位名照传，明细页按空值查询）；
     * 状态由点击的列决定（已盘/未盘/全部）
     */
    private void openInventoryStatsDetail(int filter) {
        Intent intent = new Intent(mContext, InventoryStatsActivity.class);
        intent.putExtra(InventoryStatsActivity.EXTRA_FILTER, filter);
        if (!StringUtils.isNullOrEmpty(locationName)) {
            // 存放地点层：统计只按地点，明细页同步不加入部门条件
            intent.putExtra(InventoryStatsActivity.EXTRA_LOCATION, locationName);
        } else if (!StringUtils.isNullOrEmpty(deptName)) {
            // 部门层：按管理部门查询
            intent.putExtra(InventoryStatsActivity.EXTRA_DEPT_NAME, deptName);
        }
        startActivity(intent);
    }

    /**
     * 条码命中处理（识别方式的公共出口，主线程调用）：查资产 → 目标过滤 → 开关开启时自动标记盘实/已盘 → 加入卡片列表 → 播放提示音
     * <p>
     * 1. 按条码(BARCODEID)查询资产，未登记时提示用户（同条码2秒内只提示一次）；
     *    找资产模式下非目标标签（含未入库标签）静默忽略，不提示、不标记、不展示
     * 2. 找资产模式可通过 {@link #selectScanTarget(List)} 只放行匹配目标的资产
     * 3. 重复判定口径为"本次页面会话是否已扫到过该条码"（与数据库盘点状态解耦）：
     *    首次命中播扫描成功音，同一会话再次命中才播"重复"
     * 4. 盘点扫描开关开启时：盘点情况=盘实(PDZT=02)、盘点状态=已盘(ZT=02)，并置ISUPLOAD=02(未同步)；关闭时仅展示卡片不改状态
     * 5. 刷新顶部统计与该资产卡片（颜色联动）：本次标记成功时走内存增量更新，避免连续扫描反复count查询；
     *    列表尚未包含时才新增卡片（去重）
     * 6. 按首次/重复及当前扫描节奏播放对应提示音（连扫时成功语音降级短哔）
     *
     * @param barCode 资产条码（BARCODEID）
     * @param epc     标签EPC（RFID扫描时有值，红外扫描传空）
     * @param rssi    信号强度（RFID扫描时有值，红外扫描传空）
     */
    protected void handleBarcodeHit(String barCode, String epc, String rssi) {
        if (StringUtils.isNullOrEmpty(barCode)) {
            return;
        }
        List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=?", barCode);
        if (assetsList.isEmpty()) {
            Log.i(TAG, "该资产未查询到 barCode=" + barCode);
            // 数据库查不到该条码：明确提示用户（原逻辑仅打印日志，用户无感知）；
            // 找资产模式下非目标标签一律静默，避免持续提示打断寻找
            if (!isScanTargetFilterActive()) {
                showUnknownTagTip(barCode);
            }
            return;
        }
        // 找资产模式：同条码多条记录时优先返回匹配目标的那条；非目标直接静默忽略
        Assets hitAssets = selectScanTarget(assetsList);
        if (hitAssets == null) {
            return;
        }
        // 重复语音判定：本次页面会话内该条码是否已命中过（首次=false，再次=true）；
        // 不再以数据库盘点状态判定，避免历史已盘资产首次扫描就播"重复"
        boolean duplicate = !scannedBarcodeSet.add(barCode);
        // 该资产在本轮扫描前是否已盘：仅决定是否需要写库标记，不作为语音重复口径
        boolean alreadyCounted = "02".equals(hitAssets.getZT());
        // 仅未盘且开启自动标记时才写库；已盘重复扫码不再反复置未同步，避免重复上传
        boolean markedNow = markStatusOn && !alreadyCounted;
        if (markedNow) {
            hitAssets.setPDZT("02");
            hitAssets.setZT("02");
            hitAssets.setISUPLOAD(Const.NOT_UPLOAD);
            hitAssets.save();
            Log.i(TAG, "已标记盘实/已盘并置未同步 barCode=" + barCode);
        }
        // 顶部已盘数量与卡片颜色同步刷新：标记成功且统计已初始化时走增量更新，
        // 连续扫描大数据量时不再每次命中都执行count查询
        if (!markedNow || !incrementScopeStatsOnMarked(hitAssets)) {
            refreshScopeStats();
        }
        tagAdapter.refreshAssets(hitAssets);
        // 该资产尚未在列表中时才新增卡片，避免重复
        if (!tagAdapter.containsAssets(hitAssets)) {
            RfidTagAdapter.RfidTagItem item = new RfidTagAdapter.RfidTagItem();
            item.epc = epc;
            item.barcode = barCode;
            item.rssi = rssi;
            item.assets = hitAssets;
            tagAdapter.addItem(item);
            // 新增卡片已插入首位：滚动到顶部，确保最新扫描记录立即可见
            recyclerTags.scrollToPosition(0);
        }
        updateListUi();
        playScanResultSound(duplicate);
    }

    /**
     * 是否处于扫描目标过滤状态（找资产模式）。
     * <p>
     * 默认false：红外/摄像头页保持原逻辑（未入库标签照常提示）；
     * RFID页开启找资产后覆写为true，非目标标签（含未入库标签）静默忽略。
     */
    protected boolean isScanTargetFilterActive() {
        return false;
    }

    /**
     * 从条码命中的候选资产中挑选本次要处理的资产。
     * <p>
     * 默认取第一条（普通盘点：扫到什么记什么）；
     * 找资产模式覆写此方法：只有匹配查找条件的资产才返回，非目标返回null静默忽略。
     *
     * @param candidates 同一条码查询到的资产列表（不会为空）
     * @return 本次处理的资产；返回null表示非目标，本次扫描不产生任何数据变化
     */
    protected Assets selectScanTarget(List<Assets> candidates) {
        return candidates == null || candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * 未登记标签提示：扫描到条码但资产台账中查不到时告知用户
     * <p>
     * 全局节流：2秒内最多提示一条（不区分条码），快速连续扫描时不会每条都弹；
     * 同时复用Toast并取消上一条，避免Toast队列排队导致提示一直存在
     *
     * @param barCode 未登记的条码原文
     */
    private void showUnknownTagTip(String barCode) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastUnknownTipTime < 2000) {
            return;
        }
        lastUnknownTipTime = now;
        if (unknownTagToast != null) {
            unknownTagToast.cancel();
        }
        unknownTagToast = Toast.makeText(this, "未找到资产：" + barCode, Toast.LENGTH_SHORT);
        unknownTagToast.show();
    }

    /**
     * 按命中结果播放提示音（含大批量连续扫描的防轰炸策略）：
     * 1. 本次会话再次扫到同一条码 → 重复(chongfu.mp3)，播放器内部1.5s节流
     * 2. 首次命中且为逐件慢扫（距上次命中≥DENSE_SCAN_GAP_MS）→ 扫描成功(saomiaochenggong.mp3)
     * 3. 首次命中但处于连续快扫节奏 → 降级为120ms短哔：
     *    扫描数量大时若每件都播整句语音会连成一片，短哔保留节奏反馈又不吵；
     *    短哔走ToneGenerator独立通道，不会打断正在播放的语音
     * 4. 成功语音未加载完成或被播放器内部节流 → 短哔兜底，保证始终有识别反馈
     *
     * @param duplicate 本次页面会话是否已扫到过该条码（重复判定唯一口径）
     */
    private void playScanResultSound(boolean duplicate) {
        try {
            if (scanSoundPlayer == null) {
                scanSoundPlayer = new ScanSoundPlayer(getApplicationContext());
                scanSoundPlayer.setEnabled(scanSoundEnabled);
            }
            long now = SystemClock.elapsedRealtime();
            // 连扫判定必须在更新时间戳之前：与上一次命中（含重复命中）比较操作节奏
            boolean denseScan = lastHitElapsed != 0 && now - lastHitElapsed < DENSE_SCAN_GAP_MS;
            lastHitElapsed = now;
            if (duplicate) {
                scanSoundPlayer.playDuplicate();
                return;
            }
            if (denseScan) {
                // 连续快扫阶段：不逐件播整句"扫描成功"，用短哔保持节奏反馈
                playScanBeep();
            } else if (!scanSoundPlayer.playScanSuccess()) {
                // 语音未就绪或被最小间隔节流：短哔兜底
                playScanBeep();
            }
        } catch (Throwable e) {
            // 提示音异常不得中断扫描业务（避免一条语音把整个盘点页打崩）
            Log.e(TAG, "播放扫描提示音失败", e);
        }
    }

    /** 提示音设置项文案：RFID设置菜单、红外/摄像头设置弹窗共用 */
    protected String scanSoundSettingText() {
        return "提示音：" + (scanSoundEnabled ? "开启" : "关闭");
    }

    /** 切换扫描提示音开关：立即生效并持久化，三种盘点方式共用 */
    protected void toggleScanSound() {
        scanSoundEnabled = !scanSoundEnabled;
        SharedPreferencesUtils.putBoolean(
                mContext, SharedPreferencesUtils.KEY_SCAN_SOUND_ENABLED, scanSoundEnabled);
        if (scanSoundPlayer != null) {
            scanSoundPlayer.setEnabled(scanSoundEnabled);
        }
        showShortText(scanSoundEnabled ? "扫描提示音已开启" : "扫描提示音已关闭");
    }

    /**
     * 红外/摄像头页的“设置”入口：目前只有扫描提示音开关
     * （RFID页将该开关合并在UHF设置菜单中）
     */
    protected void showScanSoundSettingSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("扫描设置")
                .addItem(scanSoundSettingText(), "sound")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if ("sound".equals(tag)) {
                        toggleScanSound();
                    }
                })
                .build().show();
    }

    /**
     * 刷新列表公共UI：列表数量 / 空视图
     */
    protected void updateListUi() {
        textTagCount.setText("列表共 " + tagAdapter.getDataList().size() + " 条资产");
        textEmpty.setVisibility(tagAdapter.getDataList().isEmpty() ? View.VISIBLE : View.GONE);
        onScanListChanged();
    }

    /**
     * 扫描列表变化回调：子类可同步刷新与列表相关的提示（如找资产已找到数量）
     */
    protected void onScanListChanged() {
    }

    /**
     * 清空会话判重集合：切换找资产条件时调用，让此前扫到的标签重新参与首次/重复判定
     */
    protected void clearScannedBarcodeSet() {
        scannedBarcodeSet.clear();
    }

    /**
     * 扫到标签短哔提示：120ms短音，200ms最小间隔节流避免连续标签刷成噪声
     */
    private void playScanBeep() {
        if (!scanSoundEnabled) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastBeepTime < BEEP_MIN_INTERVAL_MS) {
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
     * 批量变更地点入口（页面级按钮，位于标签列表下方）
     * <p>
     * 1. 非空白地点模式：确认后将列表中"存放地点≠当前盘点地点"的资产统一改为当前盘点地点（不改使用部门）；
     * 2. 空白地点（临时）模式：先确认（告知将同步把使用部门改为当前部门），
     *    再弹框输入存放地点，将列表中地点或使用部门不一致的资产统一修改；
     * 两种情况均置ISUPLOAD=02(未同步)，不改动盘点状态与管理部门
     */
    private void batchChangeLocation() {
        if (tagAdapter.getDataList().isEmpty()) {
            showShortText("请先扫描资产");
            return;
        }
        if (backupMode) {
            // 空白地点（临时）：先确认（告知将同步修改使用部门），再输入目标存放地点
            String deptShow = StringUtils.isNullOrEmpty(deptName) ? "未指定部门" : deptName;
            new QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle("批量变更地点")
                    .setMessage("是否批量变更扫描标签存放地点？\n变更的同时将把这些资产的使用部门修改为当前部门："
                            + deptShow + "（管理部门保持不变）。")
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        dialog.dismiss();
                        showBatchLocationInputDialog();
                    })
                    .show();
        } else {
            // 非空白地点：当前盘点地点必须已指定，避免把地点批量清空
            if (StringUtils.isNullOrEmpty(locationName)) {
                showShortText("当前未指定存放地点，无法批量变更");
                return;
            }
            String locationShow = RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName)
                    ? RfidTagAdapter.LOCATION_NONE_NAME : locationName;
            new QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle("批量变更地点")
                    .setMessage("是否变更当前扫描的标签的存放地点为当前地点（" + locationShow + "）？")
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        dialog.dismiss();
                        // "未设置存放地点"写空字符串，与现有一键变更口径一致
                        String storeLocation = RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName) ? "" : locationName;
                        executeBatchChangeLocation(storeLocation, false);
                    })
                    .show();
        }
    }

    /**
     * 空白地点（临时）模式：弹框输入目标存放地点（自动去除全部空格，空值拦截）
     */
    private void showBatchLocationInputDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("变更存放地点")
                .setPlaceholder("请输入存放地点")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String input = builder.getEditText().getText().toString();
                    dialog.dismiss();
                    // 自动消除全部空格（含全角空格）
                    String newLocation = input.replaceAll("[\\s\\u3000]+", "");
                    if (StringUtils.isNullOrEmpty(newLocation)) {
                        showShortText("存放地点不能为空");
                        return;
                    }
                    // 空白地点模式：同步把使用部门改为当前部门（只改使用部门，管理部门保持不变）
                    executeBatchChangeLocation(newLocation, true);
                })
                .create().show();
    }

    /**
     * 批量执行存放地点变更（后台线程保存，加载框转圈+遮罩拦截操作）
     * <p>
     * 列表中地点与使用部门均已一致的资产跳过；空白地点模式下同时把使用部门改为当前部门
     * （只改使用部门，管理部门保持不变），完成后整表重绘卡片并刷新顶部统计
     *
     * @param targetLocation 目标存放地点（空字符串表示清空地点）
     * @param updateUseDept  是否同步把使用部门改为当前部门（空白地点模式为true）
     */
    private void executeBatchChangeLocation(String targetLocation, boolean updateUseDept) {
        // 收集需要变更的资产：地点与使用部门均已一致的跳过，不产生无意义的上传记录
        final List<Assets> needChangeList = new ArrayList<>();
        for (RfidTagAdapter.RfidTagItem item : tagAdapter.getDataList()) {
            if (item.assets == null) {
                continue;
            }
            String oldLocation = StringUtils.trim(item.assets.getASSETSLAYADD());
            boolean locationSame = targetLocation.equals(oldLocation);
            boolean useDeptSame = !updateUseDept || isUseDeptMatched(item.assets.getASSETSDEPT());
            if (locationSame && useDeptSame) {
                continue;
            }
            needChangeList.add(item.assets);
        }
        if (needChangeList.isEmpty()) {
            showShortText("没有需要变更的资产");
            return;
        }
        // 加载框：转圈+遮罩，setCancelable(false)阻止操作与返回键
        showProgressDialog("批量变更地点", "正在变更存放地点，请稍候……");
        AsyncManager.getInstance().submitWithCallback(() -> {
            for (Assets assets : needChangeList) {
                assets.setASSETSLAYADD(targetLocation);
                if (updateUseDept) {
                    assets.setASSETSDEPT(resolveUseDeptStoreValue());
                }
                // 地点变更后置为未同步，待联网上传
                assets.setISUPLOAD(Const.NOT_UPLOAD);
                assets.save();
            }
            return needChangeList.size();
        }, result -> mainHandler.post(() -> {
            hideProgressDialog();
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!result.isSuccess()) {
                showShortText("变更失败，请重试");
                return;
            }
            // 卡片持有同一批资产实例：整表重绘显示最新存放地点
            tagAdapter.refreshAll();
            // 非空白模式：地点变更后资产归入当前范围，刷新顶部已盘/未盘/全部统计
            refreshScopeStats();
            showShortText(updateUseDept
                    ? "已变更 " + result.getData() + " 条资产的存放地点，使用部门已同步为当前部门，待上传"
                    : "已变更 " + result.getData() + " 条资产的存放地点，待上传");
        }));
    }

    /**
     * 一键变更：弹窗确认后将该资产归入当前盘点范围，并置未同步(ISUPLOAD=02)
     * <p>
     * 1. 存放地点：修改为当前盘点地点（"未设置存放地点"时存空字符串）
     * 2. 跨部门资产（管理部门与当前盘点部门不一致）：管理部门改为当前盘点部门对应的单位编号
     *    （与编辑页同一口径：单位名称→单位编号DWBH字典映射），使用部门改为当前盘点部门名称(使用部门字段存字符串)
     * 3. 变更完成后弹出二次确认，询问是否继续变更管理人、使用人；确定后跳转资产变更页并自动定位到"使用人"字段
     * <p>
     * 变更后该资产归属当前盘点范围：刷新顶部已盘/全部统计，并重绘该资产卡片
     * （标题颜色、提示文字、"一键变更"按钮可见性随之联动）
     */
    private void changeLocation(RfidTagAdapter.RfidTagItem item) {
        if (item.assets == null) {
            return;
        }
        // 管理部门是否已与当前盘点部门一致：一致仅变更存放地点；不一致(跨部门)时连同管理部门/使用部门一起变更
        boolean deptMatch = isAssetDeptMatched(item.assets);
        // 弹窗确认：确定后才修改，防止误触；跨部门时在弹窗中说明三项变更内容
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle(deptMatch ? "变更存放地点" : "变更资产归属确认")
                .setMessage(buildChangeLocationMessage(item.assets, deptMatch))
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    // 当前盘点地点为"未设置存放地点"时，数据库存空字符串
                    String storeLocation = RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName) ? "" : locationName;
                    item.assets.setASSETSLAYADD(storeLocation);
                    if (!deptMatch) {
                        // 管理部门存单位编号：与编辑页同一口径（单位名称→单位编号DWBH字典映射），查不到时兜底写名称(范围查询兼容名称)
                        item.assets.setGROUNDMANAGEDEPT(resolveDeptStoreValue());
                        // 使用部门存字符串：写当前盘点部门名称；"未分配部门"时写空字符串
                        item.assets.setASSETSDEPT(RfidTagAdapter.DEPT_NONE_NAME.equals(deptName) ? "" : deptName);
                    }
                    // 变更后置为未同步，待联网上传
                    item.assets.setISUPLOAD(Const.NOT_UPLOAD);
                    item.assets.save();
                    // 变更后资产归属当前范围，重新统计并刷新卡片显示
                    refreshScopeStats();
                    tagAdapter.refreshAssets(item.assets);
                    showShortText(deptMatch ? "已变更存放地点：" + locationName
                            : "已变更存放地点，并归入当前盘点部门：" + deptName);
                    // 二次确认：询问是否继续变更管理人、使用人
                    promptChangeCustodianUser(item);
                })
                .show();
    }

    /**
     * 一键变更完成后的二次确认：询问是否需要继续变更该资产的管理人、使用人；
     * 点击"确定"跳转资产变更页，并携带标记让编辑页自动滚动到"使用人"字段
     */
    private void promptChangeCustodianUser(RfidTagAdapter.RfidTagItem item) {
        if (item.assets == null || item.assets.getId() == null) {
            return;
        }
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("变更管理人/使用人")
                .setMessage("是否需要变更该资产的管理人、使用人？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    // 强制转基本类型long传参（避免Long走Serializable重载导致目标页读不到ID）
                    long assetsId = item.assets.getId();
                    Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                    intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assetsId);
                    // 携带标记：编辑页加载完成后自动滚动到"使用人"字段
                    intentEdit.putExtra(SysDefine.IntentExtra_ScrollToUser, true);
                    startActivity(intentEdit);
                })
                .show();
    }

    /**
     * 判断资产现管理部门是否与当前盘点部门一致（与卡片判断口径一致：名称/部门编号/单位编号三种历史取值）
     */
    private boolean isAssetDeptMatched(Assets assets) {
        if (StringUtils.isNullOrEmpty(deptName)) {
            return true;
        }
        String rawDept = StringUtils.trim(assets.getGROUNDMANAGEDEPT());
        if (deptName.equals(rawDept)) {
            return true;
        }
        if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName) && StringUtils.isNullOrEmpty(rawDept)) {
            return true;
        }
        if (!StringUtils.isNullOrEmpty(deptCodeValue) && deptCodeValue.equals(rawDept)) {
            return true;
        }
        return !StringUtils.isNullOrEmpty(unitCodeValue) && unitCodeValue.equals(rawDept);
    }

    /**
     * 一键变更弹窗文案：同部门仅说明存放地点变更；跨部门时列明三项变更（资产一并归入当前盘点部门）
     */
    private String buildChangeLocationMessage(Assets assets, boolean deptMatch) {
        if (deptMatch) {
            return "是否将该资产存放地点修改为当前存放地点：" + locationName + "？";
        }
        String rawDept = StringUtils.trim(assets.getGROUNDMANAGEDEPT());
        String oldDeptShow = StringUtils.isNullOrEmpty(rawDept) ? RfidTagAdapter.DEPT_NONE_NAME : deptDisplayName(rawDept);
        String deptShow = RfidTagAdapter.DEPT_NONE_NAME.equals(deptName) ? RfidTagAdapter.DEPT_NONE_NAME : deptDisplayName(deptName);
        return "该资产管理部门（" + oldDeptShow + "）与当前盘点部门（" + deptShow + "）不一致。"
                + "是否确认变更，变更内容如下：\n"
                + "1.存放地点变更为：" + locationName + "\n"
                + "2.管理部门变更为：" + deptShow + "\n"
                + "3.使用部门变更为：" + deptShow + "\n"
                + "变更数据将置为未同步，待联网上传。";
    }

    /**
     * 弹窗管理部门显示名：按业务口径优先按部门编号 BMBH 转部门名称 BMMC，
     * 再兼容历史单位编号 DWBH 转单位名称 DWMC；已是中文名称或无法识别时保留原文
     * （与卡片显示同一口径，仅用于展示，不影响匹配与写库逻辑）
     */
    private String deptDisplayName(String raw) {
        if (StringUtils.isNullOrEmpty(raw)) {
            return RfidTagAdapter.DEPT_NONE_NAME;
        }
        String name = AccountHelper.getDepartName(raw);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        String ztbh = application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH();
        name = AccountHelper.getDanweiName(ztbh, raw);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        return raw;
    }

    /**
     * 一键变更写入管理部门的取值：与新增/编辑页同一口径——存部门编号 BMBH，
     * "未分配部门"写空字符串；账套里查不到该部门编号时兜底写部门名称（展示层已兼容名称）
     */
    private String resolveDeptStoreValue() {
        if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            return "";
        }
        return StringUtils.isNullOrEmpty(deptCodeValue) ? deptName : deptCodeValue;
    }

    /**
     * 刷新当前范围盘点统计：已盘数量(绿)/未盘数量(红)/全部资产数量(黑)，并在地点层展示非拥有产权资产文字
     * <p>
     * 统计口径：
     * 1. 存放地点层：只按存放地点(ASSETSLAYADD)统计，不加入部门条件；
     * 2. 部门层（兼容直接进入页面）：按管理部门(GROUNDMANAGEDEPT)统计；
     * 3. 非拥有产权资产：仅地点层统计管理部门与当前部门不一致的资产，细分已盘/未盘数量。
     * <p>
     * 性能：全部/已盘/非拥有产权数量均走count查询，避免把范围内资产全量加载到内存遍历；
     * 扫描连续命中标记成功时由incrementScopeStatsOnMarked做内存增量，不再重复查询
     */
    protected void refreshScopeStats() {
        // 空白地点（临时）模式扫描范围不限且不展示统计，跳过查询（避免连续扫描时反复查询）
        if (backupMode) {
            return;
        }
        // 统计基础条件：存放地点层只按地点（不加入部门条件）；部门层按管理部门
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(locationName)) {
            appendLocationCondition(whereParts, args);
        } else if (!StringUtils.isNullOrEmpty(deptName)) {
            appendManageDeptCondition(whereParts, args);
        }
        String baseWhere = whereParts.isEmpty() ? "1=1" : TextUtils.join(" AND ", whereParts);
        String[] baseArgs = args.toArray(new String[0]);
        // 全部/已盘数量走count查询，避免把范围内资产全量加载到内存遍历
        scopeTotal = (int) Assets.count(Assets.class, baseWhere, baseArgs);
        List<String> countedWhereParts = new ArrayList<>(whereParts);
        List<String> countedArgs = new ArrayList<>(args);
        countedWhereParts.add("ZT = ?");
        countedArgs.add("02");
        scopeCounted = (int) Assets.count(Assets.class,
                TextUtils.join(" AND ", countedWhereParts), countedArgs.toArray(new String[0]));

        // 非拥有产权资产：仅存放地点层统计"管理部门与当前部门不一致"的资产
        if (!StringUtils.isNullOrEmpty(locationName) && !StringUtils.isNullOrEmpty(deptName)) {
            List<String> notOwnedWhereParts = new ArrayList<>(whereParts);
            List<String> notOwnedArgs = new ArrayList<>(args);
            appendNotOwnedDeptCondition(notOwnedWhereParts, notOwnedArgs);
            scopeNonOwnedTotal = (int) Assets.count(Assets.class,
                    TextUtils.join(" AND ", notOwnedWhereParts), notOwnedArgs.toArray(new String[0]));
            notOwnedWhereParts.add("ZT = ?");
            notOwnedArgs.add("02");
            scopeNonOwnedCounted = (int) Assets.count(Assets.class,
                    TextUtils.join(" AND ", notOwnedWhereParts), notOwnedArgs.toArray(new String[0]));
        } else {
            scopeNonOwnedTotal = 0;
            scopeNonOwnedCounted = 0;
        }
        scopeStatsReady = true;
        updateScopeStatsUi();
    }

    /**
     * 刷新顶部统计UI：已盘/未盘/全部三列 + 非拥有产权资产文字
     * （非拥有产权文字仅存放地点层显示，细分已盘/未盘数量）
     */
    private void updateScopeStatsUi() {
        int uncounted = scopeTotal - scopeCounted;
        int maxDigits = CountValueHelper.maxDigitsOf(scopeCounted, uncounted, scopeTotal);
        CountValueHelper.showTopCardCount(tvCounted, scopeCounted, maxDigits);
        CountValueHelper.showTopCardCount(tvUncounted, uncounted, maxDigits);
        CountValueHelper.showTopCardCount(tvTotal, scopeTotal, maxDigits);
        if (tvNonOwnedAssets == null) {
            return;
        }
        if (!StringUtils.isNullOrEmpty(locationName) && !StringUtils.isNullOrEmpty(deptName)) {
            int nonOwnedUncounted = scopeNonOwnedTotal - scopeNonOwnedCounted;
            tvNonOwnedAssets.setText("非拥有产权资产：共 " + scopeNonOwnedTotal + " 件（已盘 "
                    + scopeNonOwnedCounted + " 件 / 未盘 " + nonOwnedUncounted + " 件）");
            tvNonOwnedAssets.setVisibility(View.VISIBLE);
        } else {
            tvNonOwnedAssets.setVisibility(View.GONE);
        }
    }

    /**
     * 扫描标记成功后增量更新顶部统计（避免连续扫描时每次命中都重新count）：
     * 1. 统计缓存未初始化 → 返回false，由调用方全量刷新；
     * 2. 资产不在当前统计范围 → 数字不变，返回true；
     * 3. 在范围内 → 已盘数量+1，管理部门与当前部门不一致时非拥有产权已盘数+1
     */
    private boolean incrementScopeStatsOnMarked(Assets assets) {
        if (!scopeStatsReady) {
            return false;
        }
        if (!isAssetInScope(assets)) {
            return true;
        }
        scopeCounted++;
        if (!isAssetDeptMatched(assets)) {
            scopeNonOwnedCounted++;
        }
        updateScopeStatsUi();
        return true;
    }

    /** 资产是否在当前统计范围内：地点层按存放地点，部门层按管理部门 */
    private boolean isAssetInScope(Assets assets) {
        if (!StringUtils.isNullOrEmpty(locationName)) {
            return normalizeLocationName(assets.getASSETSLAYADD())
                    .equals(normalizeLocationName(locationName));
        }
        return isAssetDeptMatched(assets);
    }

    /** 存放地点归一化：去除首尾空格，空值统一为"未设置存放地点" */
    private static String normalizeLocationName(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.LOCATION_NONE_NAME : value;
    }

    /** 追加存放地点查询条件（"未设置存放地点"匹配数据库空值） */
    private void appendLocationCondition(List<String> whereParts, List<String> args) {
        if (RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName)) {
            whereParts.add("(ASSETSLAYADD IS NULL OR TRIM(ASSETSLAYADD) = '')");
        } else {
            whereParts.add("TRIM(ASSETSLAYADD) = ?");
            args.add(locationName);
        }
    }

    /** 追加管理部门查询条件（名称/部门编号/单位编号历史取值兼容，"未分配部门"匹配空值） */
    private void appendManageDeptCondition(List<String> whereParts, List<String> args) {
        if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            whereParts.add("(GROUNDMANAGEDEPT IS NULL OR TRIM(GROUNDMANAGEDEPT) = '')");
            return;
        }
        List<String> orParts = new ArrayList<>();
        for (String value : buildDeptMatchValues()) {
            orParts.add("TRIM(GROUNDMANAGEDEPT) = ?");
            args.add(value);
        }
        whereParts.add("(" + TextUtils.join(" OR ", orParts) + ")");
    }

    /**
     * 追加"非拥有产权资产"条件：管理部门不与当前部门匹配
     * （与卡片判断口径一致：名称/部门编号/单位编号任一命中视为一致；"未分配部门"时为空视为一致）
     */
    private void appendNotOwnedDeptCondition(List<String> whereParts, List<String> args) {
        if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            whereParts.add("NOT (GROUNDMANAGEDEPT IS NULL OR TRIM(GROUNDMANAGEDEPT) = '')");
            return;
        }
        List<String> placeholders = new ArrayList<>();
        for (String value : buildDeptMatchValues()) {
            placeholders.add("?");
            args.add(value);
        }
        whereParts.add("NOT (GROUNDMANAGEDEPT IS NOT NULL AND TRIM(GROUNDMANAGEDEPT) IN ("
                + TextUtils.join(", ", placeholders) + "))");
    }

    /** 当前部门的匹配值集合：名称/部门编号BMBH/单位编号DWBH（去重非空） */
    private List<String> buildDeptMatchValues() {
        List<String> values = new ArrayList<>();
        if (StringUtils.isNullOrEmpty(deptName) || RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            return values;
        }
        values.add(deptName);
        if (!StringUtils.isNullOrEmpty(deptCodeValue) && !values.contains(deptCodeValue)) {
            values.add(deptCodeValue);
        }
        if (!StringUtils.isNullOrEmpty(unitCodeValue) && !values.contains(unitCodeValue)) {
            values.add(unitCodeValue);
        }
        return values;
    }

    /**
     * 查询当前盘点范围的资产
     * <p>
     * 存放地点层只按存放地点查询（不加入部门条件），部门层按管理部门查询；
     * "未分配部门"/"未设置存放地点"为占位名，需匹配数据库中的空值(NULL或空串)
     *
     * @return 当前范围的资产列表
     */
    public List<Assets> queryScopeAssets() {
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(locationName)) {
            // 存放地点层：只按存放地点查询，不加入部门条件
            appendLocationCondition(whereParts, args);
        } else if (!StringUtils.isNullOrEmpty(deptName)) {
            // 部门层（兼容直接进入页面）：按管理部门查询
            appendManageDeptCondition(whereParts, args);
        }
        // 未携带任何条件时查询全部（兼容直接进入页面的情况）
        if (whereParts.isEmpty()) {
            return Assets.listAll(Assets.class);
        }
        String where = TextUtils.join(" AND ", whereParts);
        return Assets.find(Assets.class, where, args.toArray(new String[0]));
    }

    /**
     * 隐藏顶部三列统计容器（已盘/未盘/全部）：三页布局结构一致，
     * 通过第一列的父容器统一隐藏，范围文案与盘点扫描开关保留
     */
    private void hideScopeStatsColumns() {
        View columnCounted = findViewById(R.id.columnCounted);
        if (columnCounted != null && columnCounted.getParent() instanceof View) {
            ((View) columnCounted.getParent()).setVisibility(View.GONE);
        }
    }

    /**
     * 空白地点（临时）模式：手动输入存放地点并保存
     * <p>
     * 1. 操作前弹窗告知：变更存放地点的同时会把使用部门改为当前部门（只改使用部门，管理部门保持不变）；
     * 2. 弹框预填当前存放地点，输入自动去除全部空格（含全角空格），空值拦截；
     * 3. 地点与使用部门均未变化时不写库，避免无意义上传记录；
     * 4. 变更后写入ASSETSLAYADD/ASSETSDEPT并置ISUPLOAD=02(未同步)，刷新卡片供继续核对
     *
     * @param item 卡片数据（需已关联资产）
     */
    private void changeLocationManually(RfidTagAdapter.RfidTagItem item) {
        if (item.assets == null) {
            return;
        }
        final Assets assets = item.assets;
        final String deptShow = StringUtils.isNullOrEmpty(deptName) ? "未指定部门" : deptName;
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("变更存放地点")
                .setMessage("变更存放地点的同时，将把该资产的使用部门修改为当前部门："
                        + deptShow + "（管理部门保持不变）。\n是否继续？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("继续", (dialog, index) -> {
                    dialog.dismiss();
                    showManualLocationInputDialog(assets);
                })
                .show();
    }

    /**
     * 空白地点（临时）模式：弹框输入目标存放地点并保存（同时把使用部门改为当前部门）
     */
    private void showManualLocationInputDialog(final Assets assets) {
        final String oldLocation = StringUtils.trim(assets.getASSETSLAYADD());
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("变更存放地点")
                .setPlaceholder("请输入存放地点")
                .setDefaultText(oldLocation)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String input = builder.getEditText().getText().toString();
                    dialog.dismiss();
                    // 自动消除全部空格（含全角空格）
                    String newLocation = input.replaceAll("[\\s\\u3000]+", "");
                    if (StringUtils.isNullOrEmpty(newLocation)) {
                        showShortText("存放地点不能为空");
                        return;
                    }
                    boolean locationChanged = !newLocation.equals(oldLocation);
                    boolean useDeptMatched = isUseDeptMatched(assets.getASSETSDEPT());
                    if (!locationChanged && useDeptMatched) {
                        showShortText("存放地点未变化");
                        return;
                    }
                    assets.setASSETSLAYADD(newLocation);
                    // 空白地点模式：同步把使用部门改为当前部门（只改使用部门，管理部门保持不变）
                    assets.setASSETSDEPT(resolveUseDeptStoreValue());
                    // 变更后置为未同步，待联网上传
                    assets.setISUPLOAD(Const.NOT_UPLOAD);
                    assets.save();
                    // 卡片显示最新地点，供继续核对
                    tagAdapter.refreshAssets(assets);
                    showShortText(locationChanged
                            ? "已变更存放地点：" + newLocation + "，使用部门已同步为当前部门，待上传"
                            : "已同步使用部门为当前部门，待上传");
                })
                .create().show();
    }

    /** 资产使用部门是否已与当前部门一致（写入口径为部门名称，兼容历史数据存部门编号BMBH） */
    private boolean isUseDeptMatched(String rawUseDept) {
        String value = StringUtils.trim(rawUseDept);
        if (StringUtils.isNullOrEmpty(deptName) || RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            return StringUtils.isNullOrEmpty(value);
        }
        return deptName.equals(value)
                || (!StringUtils.isNullOrEmpty(deptCodeValue) && deptCodeValue.equals(value));
    }

    /** 空白地点模式写入使用部门的取值：当前部门名称；"未分配部门"或未指定部门时写空字符串 */
    private String resolveUseDeptStoreValue() {
        if (StringUtils.isNullOrEmpty(deptName) || RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            return "";
        }
        return deptName;
    }

}