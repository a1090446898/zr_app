package com.gsoft.inventory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gsoft.inventory.adapter.RfidDeptStatsAdapter;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Accounts;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.CountValueHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * 1. 顶部卡片展示全局已盘(绿)/未盘(红)/全部(黑)数量
 * 2. 网格展示全部部门（部门字典来自Accounts表，与导入资产页一致），每个部门显示 已盘/未盘/全部
 * 3. 点击部门 → 复用本页面进入该部门的存放地点层
 * <p>
 * 存放地点层(MODE_LOCATION)：
 * 1. 顶部卡片展示该部门已盘(绿)/未盘(红)/全部(黑)数量
 * 2. 网格展示该部门下的存放地点(ASSETSLAYADD分组)，每个地点显示 已盘/未盘/全部
 * 3. 点击存放地点 → 进入原有RFID盘点扫描页(RfidReadActivity)
 * <p>
 * 其他：底部"开始RFID盘点"按钮两级均跳转原有RFID扫描页；onResume自动刷新统计
 */
public class RfidStatsActivity extends BaseCompatActivity {

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

    // ==================== 【临时测试】模拟数字开关：验证已盘/未盘/全部显示（测试完成后删除本区域） ====================
    /** 【临时测试】true=顶部与分组卡片渲染模拟数字，false=恢复正常统计 */
    private static final boolean TEST_COUNT_DISPLAY = false;
    /** 【临时测试】模拟已盘数量（超过十） */
    private static final int TEST_COUNTED = 3676845;
    /** 【临时测试】模拟全部数量（未盘=全部-已盘，自动为42） */
    private static final int TEST_TOTAL = 9999999;

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
    /** 分组列表标题：部门层显示"部门盘点统计"，存放地点层显示"存放地点盘点统计" */
    @BindView(R.id.tvSectionTitle)
    TextView tvSectionTitle;
    /** 分组两列网格（RecyclerView：行位置按行内最高卡片计算，长名称卡片不再溢出贴压下一行） */
    @BindView(R.id.recyclerDept)
    RecyclerView recyclerDept;
    /** 无数据空提示 */
    @BindView(R.id.tvEmpty)
    TextView tvEmpty;


    /** 分组网格适配器（两级复用） */
    private RfidDeptStatsAdapter statsAdapter;
    /** 当前统计层级：部门层 / 存放地点层 */
    private int mode = MODE_DEPT;
    /** 存放地点层对应的部门名称 */
    private String deptName = "";

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
        // 读取层级参数：默认部门层；存放地点层附带部门名称
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_DEPT);
        deptName = getIntent().getStringExtra(EXTRA_DEPT_NAME);
        if (StringUtils.isNullOrEmpty(deptName)) {
            deptName = "";
        }

        // 标题与分组标题随层级切换
        topBar.setTitle(mode == MODE_LOCATION ? deptName + "盘点统计" : "RFID盘点统计");
        tvSectionTitle.setText(mode == MODE_LOCATION ? "存放地点盘点统计" : "部门盘点统计");
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
                startActivity(intent);
            } else {
                // 存放地点层：点击存放地点 → 进入RFID盘点扫描页，携带管理部门+存放地点过滤条件
                Intent intentRfid = new Intent(mContext, RfidReadActivity.class);
                intentRfid.putExtra(RfidReadActivity.EXTRA_DEPT_NAME, deptName);
                intentRfid.putExtra(RfidReadActivity.EXTRA_LOCATION, stats.deptName);
                startActivity(intentRfid);
            }
        });

        // 底部按钮两级均跳转原有RFID盘点扫描页（保留原盘点流程）
//        buttonStartRfid.setOnClickListener(v ->
//                startActivity(new Intent(mContext, RfidReadActivity.class)));
//
        // 首次加载统计数据
        loadStats();
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
        // 提交统计任务到线程池，buildStats在后台线程执行
        AsyncManager.getInstance().submitWithCallback(this::buildStats, result -> {
            // 回调在子线程触发，切回主线程刷新界面
            mainHandler.post(() -> {
                // 统计失败或页面已销毁时直接返回，避免空指针
                if (!result.isSuccess() || isFinishing() || isDestroyed()) {
                    return;
                }
                DeptStatsData data = result.getData();
                // 刷新顶部统计：已盘数量(绿)/未盘数量(红)/全部数量(黑)，千分位显示并按最大位数统一字号
                int uncountedTotal = data.totalCount - data.countedTotal;
                int maxDigits = CountValueHelper.maxDigitsOf(data.countedTotal, uncountedTotal, data.totalCount);
                CountValueHelper.showTopCardCount(tvCountedTotal, data.countedTotal, maxDigits);
                CountValueHelper.showTopCardCount(tvUncountedTotal, uncountedTotal, maxDigits);
                CountValueHelper.showTopCardCount(tvAllTotal, data.totalCount, maxDigits);
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
     *
     * @return 顶部统计与分组明细数据
     */
    private DeptStatsData buildStats() {
        // 【临时测试】开关开启时直接返回模拟数据渲染，不查库（测试完成后删除本if块与buildTestStats方法）
        if (TEST_COUNT_DISPLAY) {
            return buildTestStats();
        }
        if (mode == MODE_LOCATION) {
            return buildLocationStats();
        }
        return buildDeptStats();
    }

    /**
     * 【临时测试】构造模拟统计数据：顶部三列与分组卡片均使用模拟数字（均超过十）
     * <p>
     * 分组卡片预置3条：两位数、两位数、四位数(含千分位)，分别验证分组卡片字号档位与千分位显示；
     * 测试完成后删除本方法
     */
    private DeptStatsData buildTestStats() {
        DeptStatsData data = new DeptStatsData();
        data.countedTotal = TEST_COUNTED;
        data.totalCount = TEST_TOTAL;
        List<RfidDeptStatsAdapter.DeptStats> list = new ArrayList<>();
        String[] names = {"测试部门一", "测试部门二", "测试部门三"};
        int[][] values = {{TEST_COUNTED, TEST_TOTAL}, {23, 57}, {1234, 1567}};
        for (int i = 0; i < names.length; i++) {
            RfidDeptStatsAdapter.DeptStats stats = new RfidDeptStatsAdapter.DeptStats();
            stats.deptName = names[i];
            stats.counted = values[i][0];
            stats.total = values[i][1];
            list.add(stats);
        }
        data.deptList = list;
        return data;
    }

    /**
     * 部门层统计（后台线程执行）
     * <p>
     * 1. 部门以导入资产时的部门字典为准：按当前账套从Accounts表取出全部部门（与导入资产页一致），
     *    即使该部门尚未导入资产也展示为 0/0；
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
            }
        }

        // 遍历本地全部资产，将数量累加到对应部门
        List<Assets> allAssets = Assets.listAll(Assets.class);
        // 全局已盘累计数量
        int countedTotal = 0;
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
        }

        // 组装统计结果载体
        DeptStatsData data = new DeptStatsData();
        data.totalCount = allAssets.size();
        data.countedTotal = countedTotal;
        data.deptList = buildSortedList(deptMap);
        return data;
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
     * 先按管理部门过滤出该部门的全部资产（DEPT_FIELD，默认GROUNDMANAGEDEPT），
     * 再按存放地点(ASSETSLAYADD)分组统计每个地点的已盘数量(ZT=02)与资产总数，
     * 地点为空的归入"未设置存放地点"
     *
     * @return 该部门顶部统计与各存放地点明细数据
     */
    private DeptStatsData buildLocationStats() {
        // 存放地点 → [已盘数量, 资产总数]
        Map<String, int[]> locationMap = new HashMap<>();
        // 按部门查询该部门的全部资产（字段中名称与编号两种历史取值都兼容，避免漏统计）
        List<Assets> deptAssets = findDeptAssets(deptName);
        // 该部门已盘累计数量
        int countedTotal = 0;
        for (Assets assets : deptAssets) {
            // 按存放地点分组，去除首尾空格
            String location = StringUtils.trim(assets.getASSETSLAYADD());
            // 地点为空时归入"未设置存放地点"
            if (StringUtils.isNullOrEmpty(location)) {
                location = "未设置存放地点";
            }
            int[] stat = locationMap.get(location);
            if (stat == null) {
                stat = new int[2];
                locationMap.put(location, stat);
            }
            // 地点资产总数+1
            stat[1]++;
            // 盘点状态ZT=02表示已盘，地点已盘数与部门已盘数同时+1
            if ("02".equals(assets.getZT())) {
                stat[0]++;
                countedTotal++;
            }
        }

        // 组装统计结果载体
        DeptStatsData data = new DeptStatsData();
        data.totalCount = deptAssets.size();
        data.countedTotal = countedTotal;
        data.deptList = buildSortedList(locationMap);
        return data;
    }

    /**
     * 按部门名称查询该部门的全部资产（后台线程调用）
     * <p>
     * GROUNDMANAGEDEPT字段历史数据中编号与中文名称并存，
     * 查询时同时匹配名称与对应编号（部门编号BMBH/单位编号DWBH），避免漏统计；
     * "未分配部门"占位名匹配数据库空值
     *
     * @param deptName 部门中文名称
     * @return 该部门的全部资产
     */
    private List<Assets> findDeptAssets(String deptName) {
        if (StringUtils.isNullOrEmpty(deptName)) {
            return Assets.listAll(Assets.class);
        }
        // "未分配部门"：匹配字段为空的数据
        if ("未分配部门".equals(deptName)) {
            return Assets.find(Assets.class, "(" + DEPT_FIELD + " IS NULL OR " + DEPT_FIELD + " = '')");
        }
        // 名称 → 编号转换（部门编号BMBH/单位编号DWBH），与名称一并匹配
        String ztbh = "";
        Accounts loginAccount = application.getLoginAccount();
        if (loginAccount != null && !StringUtils.isNullOrEmpty(loginAccount.getZTBH())) {
            ztbh = loginAccount.getZTBH();
        }
        List<String> args = new ArrayList<>();
        args.add(deptName);
        if (!StringUtils.isNullOrEmpty(ztbh)) {
            String deptCode = AccountHelper.getDepartCode(ztbh, deptName);
            if (!StringUtils.isNullOrEmpty(deptCode) && !deptCode.equals(deptName)) {
                args.add(deptCode);
            }
        }
        String unitCode = AccountHelper.getDanweiCode(deptName);
        if (!StringUtils.isNullOrEmpty(unitCode) && !unitCode.equals(deptName)) {
            args.add(unitCode);
        }
        // GROUNDMANAGEDEPT = 名称 OR = 编号...（两种历史取值都覆盖）
        StringBuilder where = new StringBuilder(DEPT_FIELD + " = ?");
        for (int i = 1; i < args.size(); i++) {
            where.append(" OR ").append(DEPT_FIELD).append(" = ?");
        }
        return Assets.find(Assets.class, where.toString(), args.toArray(new String[0]));
    }

    /**
     * 将分组Map转换为适配器数据列表，并按中文拼音排序分组名称
     *
     * @param groupMap 分组名称 → [已盘数量, 资产总数]
     * @return 排序后的分组明细列表
     */
    private static List<RfidDeptStatsAdapter.DeptStats> buildSortedList(Map<String, int[]> groupMap) {
        List<RfidDeptStatsAdapter.DeptStats> statsList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : groupMap.entrySet()) {
            RfidDeptStatsAdapter.DeptStats stats = new RfidDeptStatsAdapter.DeptStats();
            stats.deptName = entry.getKey();
            stats.counted = entry.getValue()[0];
            stats.total = entry.getValue()[1];
            statsList.add(stats);
        }
        // 按中文拼音排序，保证展示顺序稳定美观
        Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(statsList, (o1, o2) -> collator.compare(o1.deptName, o2.deptName));
        return statsList;
    }

    /**
     * 盘点统计数据载体
     * <p>
     * 承载一次统计的全部结果：总数、已盘数、分组明细列表
     */
    static class DeptStatsData {
        /** 全部资产总数 */
        int totalCount;
        /** 已盘数量 */
        int countedTotal;
        /** 分组明细列表（部门层为部门列表，存放地点层为地点列表） */
        List<RfidDeptStatsAdapter.DeptStats> deptList;
    }

    /**
     * 两列等高网格LayoutManager：
     * 每次布局完成后把同一行卡片的最小高度拉平为该行最高卡片，
     * 实现"同一行卡片随最长名称自动等高"（RecyclerView本身不会同行等高）
     */
    private static class EqualRowHeightGridLayoutManager extends GridLayoutManager {

        EqualRowHeightGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            equalizeRowHeights();
        }

        /** 同行卡片按行内最大高度统一最小高度，保证卡片底部对齐；高度一致后不再触发重排 */
        private void equalizeRowHeights() {
            int childCount = getChildCount();
            if (childCount == 0) {
                return;
            }
            // 行号 → 行内最大卡片高度
            Map<Integer, Integer> rowMaxHeights = new HashMap<>();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                int position = getPosition(child);
                if (position == RecyclerView.NO_POSITION) {
                    continue;
                }
                int row = position / getSpanCount();
                Integer max = rowMaxHeights.get(row);
                if (max == null || child.getHeight() > max) {
                    rowMaxHeights.put(row, child.getHeight());
                }
            }
            // 低于行最大高度的卡片拉平（setMinimumHeight值不变时不会再触发布局，不会循环）
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                int position = getPosition(child);
                if (position == RecyclerView.NO_POSITION) {
                    continue;
                }
                Integer max = rowMaxHeights.get(position / getSpanCount());
                if (max != null && child.getHeight() < max && child.getMinimumHeight() != max) {
                    child.setMinimumHeight(max);
                }
            }
        }
    }

    /**
     * 网格间距装饰：列间距左右各半保证两列卡片间距均匀，行间距加在非首行卡片顶部（首行与标题自然衔接），
     * 左右/底部边距由RecyclerView的padding提供（与原GridView参数一致：边距12dp、间距10dp）
     */
    private static class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {

        /** 列数（与LayoutManager的spanCount一致） */
        private final int spanCount;
        /** 列间距(px) */
        private final int columnSpace;
        /** 行间距(px) */
        private final int rowSpace;

        GridSpaceItemDecoration(int spanCount, int columnSpace, int rowSpace) {
            this.spanCount = spanCount;
            this.columnSpace = columnSpace;
            this.rowSpace = rowSpace;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            int column = position % spanCount;
            int row = position / spanCount;
            outRect.left = column == 0 ? 0 : columnSpace / 2;
            outRect.right = column == 0 ? columnSpace / 2 : 0;
            outRect.top = row == 0 ? 0 : rowSpace;
        }
    }
}