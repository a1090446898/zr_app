package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gsoft.inventory.adapter.RfidDeptStatsAdapter;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.abs.InventoryScanBaseActivity;
import com.gsoft.inventory.service.rfid_stats.DeptStatsData;
import com.gsoft.inventory.service.rfid_stats.EqualRowHeightGridLayoutManager;
import com.gsoft.inventory.service.rfid_stats.GridSpaceItemDecoration;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.const_data.UserRoleConst;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.view.web.WebIndex;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RFID盘点统计页面（一个页面两级复用）
 * <p>
 * 部门层(MODE_DEPT，默认)：
 * 1. 顶部卡片两组统计（每列均可点击下钻盘点明细页）：
 *    左侧个人组 已盘(绿)/未盘(红)/全部(黑)，标题固定"个人"，按管理人(CUSTODIAN)匹配当前登录人；
 *    右侧单位/部门组 已盘(绿)/未盘(红)/全部(黑)，标题按登录人权限动态显示：
 *    最高权限"单位"、部门权限"部门"、个人权限整组隐藏；
 * 2. 网格展示有资产的部门（全部数量为0的部门不展示），每个部门显示 已盘/未盘/全部
 * 3. 点击部门 → 复用本页面进入该部门的存放地点层
 * <p>
 * 存放地点层(MODE_LOCATION)：
 * 1. 顶部卡片单位/部门组展示该部门已盘(绿)/未盘(红)/全部(黑)数量（部门相关口径：管理部门或使用部门为当前部门），
 *    该层单位/部门组标题统一显示"部门"（口径固定为该部门，最高权限也不标"单位"）；
 *    个人三列在本层叠加当前部门条件，与单位/部门组同范围
 * 2. 网格展示该部门涉及的存放地点(按ASSETSLAYADD去重)，每个地点按纯地点口径显示 已盘/未盘/全部
 *    （不加入部门条件，与点击进入后的盘点扫描页统计口径一致）；
 *    首位固定展示"空白地点（临时）"虚拟入口（不显示三列统计），
 *    点击后不限制部门/存放地点扫描，扫到资产后可手动指定实际存放地点并同步使用部门
 * 3. 点击存放地点 → 进入原有RFID盘点扫描页(RfidReadActivity)
 * <p>
 * 下钻：顶部统计点击 → 盘点明细页(InventoryStatsActivity)：
 * 单位/部门组按 部门层全部数据 / 存放地点层当前部门 + 对应状态；
 * 个人三列在此基础上增加"管理人=当前登录人"条件
 * <p>
 * 功能操作卡片（两级统一显示，位于统计卡片下方）：
 * 资产移交/资产处置/盘亏处理/补打条码/上传数据/数字签名六个入口，
 * 其中资产移交、资产处置、数字签名通过WebView(WebIndex)打开，补打条码进入补打条码页(ReprintBarcodeActivity)，
 * 上传数据执行资产同步（与首页"同步上传"一致），盘亏处理暂未开放
 * <p>
 * 其他：onResume自动刷新统计
 * @author 10904
 */
public class RfidStatsActivity extends BaseCompatActivity {

    /** 日志标签（[DIAG]诊断日志用，问题定位完成后可删除） */
    private static final String TAG = "RfidStatsActivity";

    /** 层级：部门统计 */
    public static final int MODE_DEPT = 1;
    /** 层级：存放地点统计 */
    public static final int MODE_LOCATION = 2;
    /** Intent参数：统计层级 */
    public static final String EXTRA_MODE = "extra_mode";
    /** Intent参数：当前部门名称（存放地点层使用） */
    public static final String EXTRA_DEPT_NAME = "extra_dept_name";
    /**
     * 部门统计与存放地点层过滤字段：统一按管理部门(GROUNDMANAGEDEPT)查询，
     * 与RFID扫描页的过滤条件一致，避免多部门同地点时数据串乱；
     * 如需改回使用部门(ASSETSDEPT)，修改此处即可
     */
    private static final String DEPT_FIELD = "GROUNDMANAGEDEPT";
    /** 使用部门字段：部门相关资产查询时与管理部门按"或"匹配 */
    private static final String USE_DEPT_FIELD = "ASSETSDEPT";

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 已盘数量（绿色显示） */
    @BindView(R.id.tvCountedTotal)
    TextView tvCountedTotal;
    /** 未盘数量（红色显示，=全部-已盘） */
    @BindView(R.id.tvUncountedTotal)
    TextView tvUncountedTotal;
    /** 全部数量（黑色显示） */
    @BindView(R.id.tvAllTotal)
    TextView tvAllTotal;
    /** 顶部"单位/部门"分组容器（个人权限时整组隐藏） */
    @BindView(R.id.layoutScopeGroup)
    View layoutScopeGroup;
    /** 顶部"单位/部门"分组标题：最高权限显示单位，部门权限显示部门 */
    @BindView(R.id.tvScopeTitle)
    TextView tvScopeTitle;
    /** 两组统计之间的竖分割线（随"单位/部门"组隐藏） */
    @BindView(R.id.dividerGroup)
    View dividerGroup;
    /** 个人已盘数量（绿色显示） */
    @BindView(R.id.tvPersonalCounted)
    TextView tvPersonalCounted;
    /** 个人未盘数量（红色显示，=个人全部-个人已盘） */
    @BindView(R.id.tvPersonalUncounted)
    TextView tvPersonalUncounted;
    /** 个人全部数量（黑色显示） */
    @BindView(R.id.tvPersonalTotal)
    TextView tvPersonalTotal;
    /** 分组列表标题：部门层显示"部门盘点统计"，存放地点层显示"存放地点盘点统计" */
    @BindView(R.id.tvSectionTitle)
    TextView tvSectionTitle;
    /** 分组两列网格（RecyclerView：行位置按行内最高卡片计算，长名称卡片不再溢出贴压下一行） */
    @BindView(R.id.recyclerDept)
    RecyclerView recyclerDept;
    /** 无数据空提示 */
    @BindView(R.id.tvEmpty)
    TextView tvEmpty;
    /** 统计加载中转圈提示（数据量大时避免页面空置） */
    @BindView(R.id.layoutLoading)
    View layoutLoading;

    /** 分组网格适配器（两级复用） */
    private RfidDeptStatsAdapter statsAdapter;
    /** 当前统计层级：部门层 / 存放地点层 */
    private int mode = MODE_DEPT;
    /** 存放地点层对应的部门名称 */
    private String deptName = "";
    /** 登录人姓名、编号：个人统计按管理人(CUSTODIAN)匹配，兼容数据源存姓名或存编号 */
    private String loginUserName = "";
    private String loginUserNo = "";

    /**
     * 访问服务器地址
     */
    private String serverHost;

    /**
     * 页面创建：加载布局、绑定视图、初始化界面
     *
     * @param savedInstanceState 保存的实例状态（页面重建时恢复用）
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定RFID盘点统计页布局
        setContentView(R.layout.activity_rfid_stats);
        // ButterKnife注入视图控件
        ButterKnife.bind(this);
        initializeView();
    }

    /**
     * 初始化界面：读取层级参数，按层级设置标题与点击行为，并首次加载统计数据
     */
    @Override
    public void initializeView() {
        serverHost = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, SysDefine.SERVERHOST);
        // 读取层级参数：默认部门层；存放地点层附带部门名称
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_DEPT);
        deptName = getIntent().getStringExtra(EXTRA_DEPT_NAME);
        if (StringUtils.isNullOrEmpty(deptName)) {
            deptName = "";
        }

        // 登录人信息：个人统计按管理人(CUSTODIAN)匹配，兼容姓名/编号
        Accounts loginAccount = application.getLoginAccount();
        String userName = loginAccount == null ? "" : StringUtils.trim(loginAccount.getYHXM());
        String userNo = loginAccount == null ? "" : StringUtils.trim(loginAccount.getYHBH());
        loginUserName = userName == null ? "" : userName;
        loginUserNo = userNo == null ? "" : userNo;
        // 顶部"单位/部门"组标题按权限动态显示：单位/部门，个人权限整组隐藏
        applyScopeGroup(loginAccount == null ? "" : loginAccount.getROLE());

        // 标题与分组标题随层级切换
        topBar.setTitle(mode == MODE_LOCATION ? deptName + "盘点统计" : "资产盘点统计");
        tvSectionTitle.setText(mode == MODE_LOCATION ? "存放地点列表" : "部门列表");
        tvEmpty.setText(mode == MODE_LOCATION ? "暂无存放地点数据" : "暂无资产数据");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        // 初始化分组网格适配器并设置卡片点击事件
        // 两列RecyclerView网格：LayoutManager在布局完成后把同一行卡片拉平为该行最高卡片（随最长名称自动等高）
        statsAdapter = new RfidDeptStatsAdapter();
        recyclerDept.setLayoutManager(new EqualRowHeightGridLayoutManager(mContext, 2));
        recyclerDept.addItemDecoration(new GridSpaceItemDecoration(2,
                QMUIDisplayHelper.dp2px(mContext, 10), QMUIDisplayHelper.dp2px(mContext, 10)));
        recyclerDept.setAdapter(statsAdapter);
        statsAdapter.setOnItemClickListener(stats -> {
            if (mode == MODE_DEPT) {
                // 部门层：点击部门 → 复用本页面进入该部门的存放地点统计
                Intent intent = new Intent(mContext, RfidStatsActivity.class);
                intent.putExtra(EXTRA_MODE, MODE_LOCATION);
                intent.putExtra(EXTRA_DEPT_NAME, stats.deptName);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "[DIAG] 跳转统计页失败", e);
                }
            } else if (stats.isBackup) {
                // 空白地点（临时）：扫描不设范围限制，扫到资产后卡片提供"变更地点"手动指定地点
                showScanModeDialog(deptName, Const.BACKUP_LOCATION_NAME, true);
            } else {
                // 存放地点层：点击存放地点 → 弹窗选择扫描方式（RFID扫描/红外线扫描/摄像头扫描），
                // 选择后携带管理部门+存放地点过滤条件进入对应扫描页
                showScanModeDialog(deptName, stats.deptName, false);
            }
        });

        // 顶部三列统计点击：下钻盘点明细页查看对应数据（部门层全量、存放地点层带当前部门条件）
        findViewById(R.id.columnCounted).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_COUNTED));
        findViewById(R.id.columnUncounted).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_UNCOUNTED));
        findViewById(R.id.columnAll).setOnClickListener(v -> openInventoryStatsDetail(InventoryStatsActivity.FILTER_ALL));
        // 个人三列点击：下钻盘点明细页查看个人数据（存放地点层同时带当前部门条件）
        findViewById(R.id.columnPersonalCounted).setOnClickListener(v -> openPersonalStatsDetail(InventoryStatsActivity.FILTER_COUNTED));
        findViewById(R.id.columnPersonalUncounted).setOnClickListener(v -> openPersonalStatsDetail(InventoryStatsActivity.FILTER_UNCOUNTED));
        findViewById(R.id.columnPersonalAll).setOnClickListener(v -> openPersonalStatsDetail(InventoryStatsActivity.FILTER_ALL));

        // 初始化功能操作卡片入口点击
        initToolsPage();

    }

    /**
     * 顶部"单位/部门"组（位于个人组右侧）标题与显隐：
     * 最高权限显示"单位"，部门权限显示"部门"，个人权限整组隐藏（只显示个人三列）；
     * 存放地点层三列口径固定为该部门，标题统一显示"部门"，避免最高权限误标为"单位"
     *
     * @param role 账号ROLE字段
     */
    private void applyScopeGroup(String role) {
        String scopeTitle = UserRoleConst.getScopeTitle(role);
        if (!StringUtils.isNullOrEmpty(scopeTitle) && mode == MODE_LOCATION) {
            scopeTitle = "部门";
        }
        boolean visible = !StringUtils.isNullOrEmpty(scopeTitle);
        layoutScopeGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        dividerGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        tvScopeTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            tvScopeTitle.setText(scopeTitle);
        }
    }

    /**
     * 顶部统计点击下钻：跳转盘点明细页查看对应数据
     * <p>
     * 部门层不带范围条件（查全部数据）；存放地点层带当前部门作为条件，且按"管理部门或使用部门"
     * 匹配（与顶部统计口径一致，保证明细数量与顶部数字相同）；状态由点击的列决定
     */
    private void openInventoryStatsDetail(int filter) {
        Intent intent = new Intent(mContext, InventoryStatsActivity.class);
        intent.putExtra(InventoryStatsActivity.EXTRA_FILTER, filter);
        if (mode == MODE_LOCATION && !StringUtils.isNullOrEmpty(deptName)) {
            intent.putExtra(InventoryStatsActivity.EXTRA_DEPT_NAME, deptName);
            // 存放地点层顶部统计按"管理部门或使用部门"口径，明细页需同口径
            intent.putExtra(InventoryStatsActivity.EXTRA_DEPT_MATCH_USE, true);
        }
        startActivity(intent);
    }

    /**
     * 个人统计点击下钻：明细页在状态条件基础上增加"管理人=当前登录人"条件；
     * 存放地点层与顶部个人口径保持一致，同时带入当前部门（管理部门或使用部门）
     */
    private void openPersonalStatsDetail(int filter) {
        Intent intent = new Intent(mContext, InventoryStatsActivity.class);
        intent.putExtra(InventoryStatsActivity.EXTRA_FILTER, filter);
        intent.putExtra(InventoryStatsActivity.EXTRA_CUSTODIAN_ONLY, true);
        if (mode == MODE_LOCATION && !StringUtils.isNullOrEmpty(deptName)) {
            intent.putExtra(InventoryStatsActivity.EXTRA_DEPT_NAME, deptName);
            intent.putExtra(InventoryStatsActivity.EXTRA_DEPT_MATCH_USE, true);
        }
        startActivity(intent);
    }

    /**
     * 选择扫描方式弹窗（居中弹窗，三个按钮竖排）：RFID扫描 / 红外线扫描 / 摄像头扫描
     * <p>
     * 1. RFID扫描 → RFID盘点扫描页(RfidReadActivity)
     * 2. 红外线扫描 → 红外线盘点扫描页(InfraredReadActivity)，仅新设备可用物理扫描键触发
     * 3. 摄像头扫描 → 摄像头盘点扫描页(CameraReadActivity)，页面内嵌相机预览连续扫码
     * <p>
     * 三个入口均携带当前盘点范围（管理部门+存放地点）过滤条件；
     * 空白地点（临时）模式额外携带备用标记，扫描页不限制部门/存放地点
     */
    private void showScanModeDialog(String deptName, String locationName, boolean backupMode) {
        QMUIDialog.CustomDialogBuilder builder = new QMUIDialog.CustomDialogBuilder(mContext);
        builder.setLayout(R.layout.dialog_scan_mode_choose);
        final QMUIDialog dialog = builder.create();
        dialog.findViewById(R.id.btnScanRfid).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intentRfid = new Intent(mContext, RfidReadActivity.class);
            intentRfid.putExtra(RfidReadActivity.EXTRA_DEPT_NAME, deptName);
            intentRfid.putExtra(RfidReadActivity.EXTRA_LOCATION, locationName);
            if (backupMode) {
                intentRfid.putExtra(InventoryScanBaseActivity.EXTRA_BACKUP_MODE, true);
            }
            startActivity(intentRfid);
        });
        dialog.findViewById(R.id.btnScanInfrared).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intentInfrared = new Intent(mContext, InfraredReadActivity.class);
            intentInfrared.putExtra(InfraredReadActivity.EXTRA_DEPT_NAME, deptName);
            intentInfrared.putExtra(InfraredReadActivity.EXTRA_LOCATION, locationName);
            if (backupMode) {
                intentInfrared.putExtra(InventoryScanBaseActivity.EXTRA_BACKUP_MODE, true);
            }
            startActivity(intentInfrared);
        });
        dialog.findViewById(R.id.btnScanCamera).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intentCamera = new Intent(mContext, CameraReadActivity.class);
            intentCamera.putExtra(CameraReadActivity.EXTRA_DEPT_NAME, deptName);
            intentCamera.putExtra(CameraReadActivity.EXTRA_LOCATION, locationName);
            if (backupMode) {
                intentCamera.putExtra(InventoryScanBaseActivity.EXTRA_BACKUP_MODE, true);
            }
            startActivity(intentCamera);
        });
        dialog.show();
    }

    /**
     * 页面恢复回调：从RFID扫描页返回时自动刷新统计，保证已盘数量实时更新
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    /**
     * 异步加载统计数据
     * <p>
     * 数据库查询放到AsyncManager线程池执行，避免阻塞主线程；
     * 结果通过mainHandler切回主线程更新UI，并做页面销毁防护
     */
    private void loadStats() {
        // 统计耗时随数据量增加，先显示转圈提示，避免大数据量时页面长时间空置
        layoutLoading.setVisibility(View.VISIBLE);
        // 统计期间先隐藏上次的空提示，避免与转圈重叠
        tvEmpty.setVisibility(View.GONE);
        // 提交统计任务到线程池，buildStats在后台线程执行
        AsyncManager.getInstance().submitWithCallback(this::buildStats, result -> {
            // 回调在子线程触发，切回主线程刷新界面
            mainHandler.post(() -> {
                // 统计结束先隐藏转圈提示（失败时同样隐藏，避免一直转圈）
                layoutLoading.setVisibility(View.GONE);
                // 统计失败或页面已销毁时直接返回，避免空指针
                if (!result.isSuccess() || isFinishing() || isDestroyed()) {
                    return;
                }
                DeptStatsData data = result.getData();
                // 刷新顶部统计：已盘数量(绿)/未盘数量(红)/全部数量(黑)，千分位显示并按最大位数统一字号
                int uncountedTotal = data.totalCount - data.countedTotal;
                int maxDigits = CountValueHelper.maxDigitsOf(data.countedTotal, uncountedTotal, data.totalCount);
                CountValueHelper.showTopCardSixCount(tvCountedTotal, data.countedTotal, maxDigits);
                CountValueHelper.showTopCardSixCount(tvUncountedTotal, uncountedTotal, maxDigits);
                CountValueHelper.showTopCardSixCount(tvAllTotal, data.totalCount, maxDigits);
                // 个人三列：管理人=当前登录人，未盘=个人全部-个人已盘
                int personalUncounted = data.personalTotal - data.personalCounted;
                if (StringUtils.isNullOrEmpty(loginUserName) && StringUtils.isNullOrEmpty(loginUserNo)) {
                    // 登录人姓名与编号都缺失时无法匹配管理人，显示"-"避免误读为0
                    tvPersonalCounted.setText("-");
                    tvPersonalUncounted.setText("-");
                    tvPersonalTotal.setText("-");
                } else {
                    int personalMaxDigits = CountValueHelper.maxDigitsOf(data.personalCounted, personalUncounted, data.personalTotal);
                    CountValueHelper.showTopCardSixCount(tvPersonalCounted, data.personalCounted, personalMaxDigits);
                    CountValueHelper.showTopCardSixCount(tvPersonalUncounted, personalUncounted, personalMaxDigits);
                    CountValueHelper.showTopCardSixCount(tvPersonalTotal, data.personalTotal, personalMaxDigits);
                }
                // 刷新分组网格数据
                statsAdapter.setData(data.deptList);
                statsAdapter.notifyDataSetChanged();
                // 无分组数据时展示空提示
                tvEmpty.setVisibility(data.deptList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    /**
     * 统计入口（后台线程执行）：按层级分发到部门统计或存放地点统计
     * <p>
     * 个人三列与单位/部门组同范围，在两次遍历中一并累加，避免额外查库
     *
     * @return 顶部统计与分组明细数据
     */
    private DeptStatsData buildStats() {
        return mode == MODE_LOCATION ? buildLocationStats() : buildDeptStats();
    }

    /**
     * 个人资产判定：管理人(CUSTODIAN)为当前登录人姓名或编号（去首尾空格后比较），
     * 与明细页 TRIM(CUSTODIAN) 条件同一口径
     *
     * @param assets 资产记录
     * @return true=属于当前登录人
     */
    private boolean isPersonalAsset(Assets assets) {
        String custodian = StringUtils.trim(assets.getCUSTODIAN());
        if (StringUtils.isNullOrEmpty(custodian)) {
            return false;
        }
        return (!StringUtils.isNullOrEmpty(loginUserName) && custodian.equals(loginUserName))
                || (!StringUtils.isNullOrEmpty(loginUserNo) && custodian.equals(loginUserNo));
    }

    /**
     * 部门层统计（后台线程执行）
     * <p>
     * 1. 部门以导入资产时的部门字典为准：按当前账套从Accounts表取出部门名称，
     *    用于数据分组与排序；未导入资产的部门（全部数量为0）不展示；
     * 2. 遍历本地全部资产累加统计：已盘数量(ZT=02) / 部门资产总数，
     *    部门按管理部门(GROUNDMANAGEDEPT)分组，与DEPT_FIELD口径一致；
     * 3. GROUNDMANAGEDEPT字段中编号与中文名称并存（新建资产存单位编号DWBH，导入资产存Excel原文），
     *    统计前统一归一化为中文名称，避免编号与中文被拆成两组
     *
     * @return 全局与各部门的盘点统计数据
     */
    private DeptStatsData buildDeptStats() {
        // 部门名称 → [已盘数量, 资产总数]
        Map<String, int[]> deptMap = new HashMap<>();
        // 部门/单位名称 → 编号（部门名称对应BMBH，单位名称对应DWBH），用于分组列表按编号排序
        Map<String, String> deptCodeMap = new HashMap<>();
        // 编号 → 中文名称映射（单位编号DWBH→DWMC、部门编号BMBH→BMMC）
        Map<String, String> codeToNameMap = new HashMap<>();
        // 中文名称集合（部门名称/单位名称），用于判断字段中存的原文是否已是名称
        Set<String> nameSet = new HashSet<>();

        // 从Accounts表读取当前账套的部门/单位字典，同时构建预置分组与编号→名称映射
        String ztbh = "";
        Accounts loginAccount = application.getLoginAccount();
        if (loginAccount != null && !StringUtils.isNullOrEmpty(loginAccount.getZTBH())) {
            ztbh = loginAccount.getZTBH();
        }
        if (!StringUtils.isNullOrEmpty(ztbh)) {
            for (Accounts account : Accounts.find(Accounts.class, "ZTBH = ?", ztbh)) {
                String deptCode = StringUtils.trim(account.getBMBH());
                String deptName = StringUtils.trim(account.getBMMC());
                String unitCode = StringUtils.trim(account.getDWBH());
                String unitName = StringUtils.trim(account.getDWMC());
                // 预置部门名称（与导入资产页同口径），未导入资产的部门显示 0/0
                if (!StringUtils.isNullOrEmpty(deptName) && !deptMap.containsKey(deptName)) {
                    deptMap.put(deptName, new int[2]);
                }
                // 名称集合：识别字段中已存中文名称的数据
                if (!StringUtils.isNullOrEmpty(deptName)) {
                    nameSet.add(deptName);
                }
                if (!StringUtils.isNullOrEmpty(unitName)) {
                    nameSet.add(unitName);
                }
                // 编号→名称映射：把字段中存编号的数据转成中文名称
                if (!StringUtils.isNullOrEmpty(deptCode) && !StringUtils.isNullOrEmpty(deptName)) {
                    codeToNameMap.put(deptCode, deptName);
                }
                if (!StringUtils.isNullOrEmpty(unitCode) && !StringUtils.isNullOrEmpty(unitName)) {
                    codeToNameMap.put(unitCode, unitName);
                }
                // 名称→编号：部门按BMBH、单位按DWBH，供部门层列表按编号排序使用
                if (!StringUtils.isNullOrEmpty(deptName) && !StringUtils.isNullOrEmpty(deptCode)) {
                    deptCodeMap.put(deptName, deptCode);
                }
                if (!StringUtils.isNullOrEmpty(unitName) && !StringUtils.isNullOrEmpty(unitCode)) {
                    deptCodeMap.put(unitName, unitCode);
                }
            }
        }

        // 遍历本地全部资产，将数量累加到对应部门
        List<Assets> allAssets = Assets.listAll(Assets.class);
        // 全局已盘累计数量
        int countedTotal = 0;
        // 个人资产累计：管理人=当前登录人的全部/已盘数量
        int personalTotal = 0;
        int personalCounted = 0;
        for (Assets assets : allAssets) {
            // 管理部门归一化为中文名称（编号转名称、空值归"未分配部门"）
            String dept = normalizeDeptName(StringUtils.trim(assets.getGROUNDMANAGEDEPT()), codeToNameMap, nameSet);
            int[] stat = deptMap.get(dept);
            if (stat == null) {
                // Assets表中存在字典中没有的部门，补录展示
                stat = new int[2];
                deptMap.put(dept, stat);
            }
            // 部门资产总数+1
            stat[1]++;
            // 盘点状态ZT=02表示已盘，已盘数量与全局已盘数同时+1
            if ("02".equals(assets.getZT())) {
                stat[0]++;
                countedTotal++;
            }
            // 个人资产：管理人=当前登录人
            if (isPersonalAsset(assets)) {
                personalTotal++;
                if ("02".equals(assets.getZT())) {
                    personalCounted++;
                }
            }
        }

        // 组装统计结果载体
        DeptStatsData data = new DeptStatsData();
        data.totalCount = allAssets.size();
        data.countedTotal = countedTotal;
        data.personalCounted = personalCounted;
        data.personalTotal = personalTotal;
        data.deptList = buildSortedList(deptMap, deptCodeMap);
        // 全部数量为0的部门不展示：部门字典中存在但本地未导入任何资产的部门
        removeEmptyDeptStats(data.deptList);
        return data;
    }

    /**
     * 过滤全部数量为0的部门：部门字典中有该部门但本地没有资产时不展示
     * <p>
     * 不使用 Collection.removeIf（Android API 24 才提供），改用 Iterator 保证低版本兼容
     *
     * @param statsList 部门统计列表
     */
    private static void removeEmptyDeptStats(List<RfidDeptStatsAdapter.DeptStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            return;
        }
        Iterator<RfidDeptStatsAdapter.DeptStats> iterator = statsList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().total <= 0) {
                iterator.remove();
            }
        }
    }

    /**
     * 管理部门字段归一化：编号转中文名称，空值归入"未分配部门"
     * <p>
     * GROUNDMANAGEDEPT字段历史数据中编号与中文名称并存：
     * 1. 空值 → "未分配部门"
     * 2. 已存中文名称（部门名称/单位名称）→ 直接使用
     * 3. 存编号（单位编号DWBH/部门编号BMBH）→ 按字典转成中文名称
     * 4. 字典中均无法识别 → 保留原文单独成组，避免误归类
     *
     * @param raw           字段原始值（已去除首尾空格）
     * @param codeToNameMap 编号→名称映射
     * @param nameSet       字典中的名称集合
     * @return 归一化后的中文名称
     */
    private static String normalizeDeptName(String raw, Map<String, String> codeToNameMap, Set<String> nameSet) {
        if (StringUtils.isNullOrEmpty(raw)) {
            return "未分配部门";
        }
        // 原文已是字典中的中文名称，直接使用
        if (nameSet.contains(raw)) {
            return raw;
        }
        // 原文是编号，转成对应中文名称
        String name = codeToNameMap.get(raw);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        // 无法识别的值保留原文
        return raw;
    }

    /**
     * 存放地点层统计（后台线程执行）
     * <p>
     * 1. 部门相关资产：管理部门(GROUNDMANAGEDEPT)或使用部门(ASSETSDEPT)为当前部门的资产，
     *    用于顶部三列统计与确定存放地点列表范围；
     * 2. 存放地点列表：按部门相关资产涉及的存放地点去重展示；
     * 3. 地点卡片数字：按纯地点口径统计（不加入部门作为限制），与列表是否属于该部门无关
     *
     * @return 该部门顶部统计与各存放地点明细数据
     */
    private DeptStatsData buildLocationStats() {
        // 管理部门或使用部门为当前部门的资产（名称/部门编号/单位编号历史取值兼容）
        List<Assets> deptAssets = findDeptRelatedAssets(deptName);
        // 部门相关资产已盘累计数量与涉及的存放地点集合
        int countedTotal = 0;
        // 个人资产累计：管理人=当前登录人，范围与第一组一致（管理部门或使用部门为当前部门）
        int personalCounted = 0;
        int personalTotal = 0;
        Set<String> relatedLocations = new HashSet<>();
        for (Assets assets : deptAssets) {
            if ("02".equals(assets.getZT())) {
                countedTotal++;
            }
            // 个人资产：管理人=当前登录人
            if (isPersonalAsset(assets)) {
                personalTotal++;
                if ("02".equals(assets.getZT())) {
                    personalCounted++;
                }
            }
            relatedLocations.add(normalizeLocationName(assets.getASSETSLAYADD()));
        }

        // 存放地点 → [已盘数量, 资产总数]：纯地点统计，不加入部门作为限制
        Map<String, int[]> locationMap = new HashMap<>();
        for (Assets assets : Assets.listAll(Assets.class)) {
            String location = normalizeLocationName(assets.getASSETSLAYADD());
            // 只统计本部门相关资产涉及的存放地点，避免展示与当前部门无关的地点
            if (!relatedLocations.contains(location)) {
                continue;
            }
            int[] stat = locationMap.get(location);
            if (stat == null) {
                stat = new int[2];
                locationMap.put(location, stat);
            }
            // 地点资产总数+1
            stat[1]++;
            // 盘点状态ZT=02表示已盘
            if ("02".equals(assets.getZT())) {
                stat[0]++;
            }
        }

        // 组装统计结果载体
        DeptStatsData data = new DeptStatsData();
        data.totalCount = deptAssets.size();
        data.countedTotal = countedTotal;
        data.personalCounted = personalCounted;
        data.personalTotal = personalTotal;
        data.deptList = buildSortedList(locationMap, null);
        // 空白地点（临时）：每个部门固定展示的虚拟入口，不参与真实地点统计，固定排在第一位
        RfidDeptStatsAdapter.DeptStats backupStats = new RfidDeptStatsAdapter.DeptStats();
        backupStats.deptName = Const.BACKUP_LOCATION_NAME;
        backupStats.isBackup = true;
        data.deptList.add(0, backupStats);
        return data;
    }

    /** 存放地点归一化：去除首尾空格，空值归"未设置存放地点" */
    private static String normalizeLocationName(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.LOCATION_NONE_NAME : value;
    }

    /**
     * 按部门名称查询部门相关资产（后台线程调用）
     * <p>
     * 部门相关口径：管理部门(GROUNDMANAGEDEPT)或使用部门(ASSETSDEPT)为当前部门，满足其一即可；
     * 字段历史数据中编号与中文名称并存（使用部门可能存部门编号BMBH），查询时同时匹配名称与对应编号
     * （部门编号BMBH/单位编号DWBH），避免漏统计；"未分配部门"占位名匹配任一字段为空的数据
     *
     * @param deptName 部门中文名称
     * @return 与部门相关的全部资产
     */
    private List<Assets> findDeptRelatedAssets(String deptName) {
        if (StringUtils.isNullOrEmpty(deptName)) {
            return Assets.listAll(Assets.class);
        }
        // "未分配部门"：管理部门或使用部门为空均视为相关
        if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
            return Assets.find(Assets.class,
                    "(" + DEPT_FIELD + " IS NULL OR TRIM(" + DEPT_FIELD + ") = ''"
                            + " OR " + USE_DEPT_FIELD + " IS NULL OR TRIM(" + USE_DEPT_FIELD + ") = '')");
        }
        // 名称 → 编号转换（部门编号BMBH/单位编号DWBH），与名称一并匹配
        List<String> values = new ArrayList<>();
        values.add(deptName);
        String ztbh = "";
        Accounts loginAccount = application.getLoginAccount();
        if (loginAccount != null && !StringUtils.isNullOrEmpty(loginAccount.getZTBH())) {
            ztbh = loginAccount.getZTBH();
        }
        if (!StringUtils.isNullOrEmpty(ztbh)) {
            String deptCode = AccountHelper.getDepartCode(ztbh, deptName);
            if (!StringUtils.isNullOrEmpty(deptCode) && !values.contains(deptCode)) {
                values.add(deptCode);
            }
        }
        String unitCode = AccountHelper.getDanweiCode(deptName);
        if (!StringUtils.isNullOrEmpty(unitCode) && !values.contains(unitCode)) {
            values.add(unitCode);
        }
        // 管理部门或使用部门命中任一匹配值
        List<String> args = new ArrayList<>();
        List<String> orParts = new ArrayList<>();
        for (String field : new String[]{DEPT_FIELD, USE_DEPT_FIELD}) {
            for (String value : values) {
                orParts.add("TRIM(" + field + ") = ?");
                args.add(value);
            }
        }
        return Assets.find(Assets.class, TextUtils.join(" OR ", orParts), args.toArray(new String[0]));
    }

    /**
     * 将分组Map转换为适配器数据列表
     * <p>
     * 排序规则：
     * 1. 传入编号映射时按编号升序（部门层按BMBH，纯数字编号按数值比较）；
     * 2. 无编号的分组（"未分配部门"、字典外部门等）排在有编号分组之后；
     * 3. 无编号分组之间或编号相同时按中文拼音排序，保证顺序稳定
     *
     * @param groupMap 分组名称 → [已盘数量, 资产总数]
     * @param codeMap  分组名称 → 编号（传null时全部按名称拼音排序，如存放地点层）
     * @return 排序后的分组明细列表
     */
    private static List<RfidDeptStatsAdapter.DeptStats> buildSortedList(Map<String, int[]> groupMap, Map<String, String> codeMap) {
        List<RfidDeptStatsAdapter.DeptStats> statsList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : groupMap.entrySet()) {
            RfidDeptStatsAdapter.DeptStats stats = new RfidDeptStatsAdapter.DeptStats();
            stats.deptName = entry.getKey();
            stats.counted = entry.getValue()[0];
            stats.total = entry.getValue()[1];
            statsList.add(stats);
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(statsList, (o1, o2) -> {
            String code1 = codeMap == null ? null : codeMap.get(o1.deptName);
            String code2 = codeMap == null ? null : codeMap.get(o2.deptName);
            boolean noCode1 = StringUtils.isNullOrEmpty(code1);
            boolean noCode2 = StringUtils.isNullOrEmpty(code2);
            if (noCode1 != noCode2) {
                // 无编号的分组统一排在最后
                return noCode1 ? 1 : -1;
            }
            if (!noCode1) {
                int codeCompare = compareDeptCode(code1, code2);
                if (codeCompare != 0) {
                    return codeCompare;
                }
            }
            // 编号相同或无编号时按中文拼音兜底，保证顺序稳定
            return collator.compare(o1.deptName, o2.deptName);
        });
        return statsList;
    }

    /**
     * 部门编号比较：均为纯数字时按数值比较（避免"10"排在"2"前面），否则按字典序
     */
    private static int compareDeptCode(String code1, String code2) {
        try {
            return Long.compare(Long.parseLong(code1), Long.parseLong(code2));
        } catch (NumberFormatException e) {
            return code1.compareTo(code2);
        }
    }

    /**
     * 功能操作卡片入口点击绑定（位于统计卡片下方，两级页面统一显示）
     * <p>
     * 资产移交 → WebView(/pages/asset_transfer/index)
     * 资产处置 → WebView(/pages/asset_scrap/index)
     * 补打条码 → 补打条码页(ReprintBarcodeActivity)
     * 上传数据 → 资产同步（与首页"同步上传"一致）
     * 数字签名 → 校验未上传数据后，WebView打开签名页
     * 盘亏处理 → 功能暂未开放，点击提示
     */
    private void initToolsPage() {
        findViewById(R.id.btnAssetTransfer).setOnClickListener(v -> openWebPage("/pages/asset_transfer/index?back=false"));
        findViewById(R.id.btnAssetDispose).setOnClickListener(v -> openWebPage("/pages/asset_scrap/index?back=false"));
        findViewById(R.id.btnLossHandle).setOnClickListener(v -> showShortText("功能开发中"));
        findViewById(R.id.btnReprintBarcode).setOnClickListener(v ->
                startActivity(new Intent(mContext, ReprintBarcodeActivity.class)));
        findViewById(R.id.btnUploadData).setOnClickListener(v -> startSyncAssetsTask());
        findViewById(R.id.btnDigitalSign).setOnClickListener(v -> openDigitalSignPage());
    }

    /**
     * 数字签名入口：
     * 1. 先校验本地是否还有未上传数据（口径与上传流程一致：Assets.ISUPLOAD=02），
     *    有则弹窗提示先上传，可直接跳转现有上传流程，避免签名页取到不完整的服务器数据
     * 2. 校验通过后通过项目WebView(WebIndex)打开签名页，phone取当前登录账号手机号
     */
    private void openDigitalSignPage() {
        long waitingSyncCount = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{Const.NOT_UPLOAD});
        if (waitingSyncCount > 0) {
            new QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle("系统提示")
                    .setMessage("当前有 " + waitingSyncCount + " 条数据未上传，请先上传数据后再进行数字签名。")
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction(0, "去上传", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                        dialog.dismiss();
                        startSyncAssetsTask();
                    })
                    .show();
            return;
        }
        String phone = application.getLoginAccount() == null ? "" : application.getLoginAccount().getPHONE();
        if (StringUtils.isNullOrEmpty(phone)) {
            showShortText("当前登录账号缺少手机号，无法进行数字签名");
            return;
        }
        // 签名页部署在独立站点：正式环境域名 / 测试环境IP（与原逻辑一致）
        String linkHost = (serverHost != null && serverHost.contains("www.zrast.com"))
                ? "http://www.zrast.com" : "http://47.110.252.199:8080";
        String signUrl = linkHost + "/signature.html?phone=" + phone;
        // 绝对地址交给WebIndex加载（http/https开头不再拼接服务器前缀）
        Intent intent = new Intent(mContext, WebIndex.class);
        intent.putExtra("webUrl", signUrl);
        startActivity(intent);
    }

    /**
     * 打开WebView页面（相对路径，WebIndex内部自动拼接服务器地址）
     *
     * @param webUrl 页面相对路径，如 /pages/asset_transfer/index
     */
    private void openWebPage(String webUrl) {
        Intent intent = new Intent(mContext, WebIndex.class);
        intent.putExtra("webUrl", webUrl);
        startActivity(intent);
    }

}