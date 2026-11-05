package com.gsoft.inventory;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.gsoft.inventory.adapter.InventoryStatsAdapter;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 盘点统计明细：展示指定条件（范围+状态）下的资产列表
 * <p>
 * 查询条件由跳转页带入且页面内不可修改（顶部一行文字只读展示）：
 * 1. 状态：全部(FILTER_ALL) / 已盘(FILTER_COUNTED, ZT=02) / 未盘(FILTER_UNCOUNTED, ZT为空或!=02)
 * 2. 范围：管理部门(GROUNDMANAGEDEPT，名称/部门编号/单位编号历史取值兼容) + 存放地点(ASSETSLAYADD)
 * <p>
 * 入口：
 * 1. RFID盘点统计页部门层点击顶部数字 → 全部数据+对应状态
 * 2. RFID盘点统计页存放地点层点击顶部数字 → 该部门+对应状态
 * 3. RFID盘点扫描页点击顶部数字 → 该部门+存放地点+对应状态
 * <p>
 * 数据加载：每页PAGE_SIZE条分页查询（后台线程执行），列表滚到底部自动加载下一页，
 * 底部统计条展示真实总数（count查询），列表尾部提示加载状态
 */
public class InventoryStatsActivity extends BaseCompatActivity {

    /** Intent参数：状态筛选，取值 FILTER_ALL / FILTER_COUNTED / FILTER_UNCOUNTED */
    public static final String EXTRA_FILTER = "extra_filter";
    /** Intent参数：管理部门名称（"未分配部门"表示数据库中为空值） */
    public static final String EXTRA_DEPT_NAME = "extra_dept_name";
    /** Intent参数：存放地点名称（"未设置存放地点"表示数据库中为空值） */
    public static final String EXTRA_LOCATION = "extra_location";
    /** Intent参数：部门条件是否同时匹配使用部门（true=管理部门或使用部门命中即可） */
    public static final String EXTRA_DEPT_MATCH_USE = "extra_dept_match_use";

    /** 状态筛选：全部（不限ZT） */
    public static final int FILTER_ALL = 0;
    /** 状态筛选：已盘（ZT=02） */
    public static final int FILTER_COUNTED = 1;
    /** 状态筛选：未盘（ZT为空或!=02） */
    public static final int FILTER_UNCOUNTED = 2;

    /** 分页每页条数 */
    private static final int PAGE_SIZE = 100;

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 条件文字（只读）：范围+状态，如"财务科 / 档案室 未盘"，无范围时只显示状态 */
    @BindView(R.id.tvInventoryFilter)
    TextView tvInventoryFilter;
    @BindView(R.id.listStats)
    ListView listStats;
    /** 底部统计条：满足条件的真实总数 */
    @BindView(R.id.tvTotalCount)
    TextView tvTotalCount;
    /** 加载中转圈提示（首屏/刷新时显示，避免大数据量时页面空置） */
    @BindView(R.id.layoutLoading)
    View layoutLoading;

    private InventoryStatsAdapter statsAdapter;

    /** 状态筛选（跳转带入，页面内不可修改） */
    private int filter = FILTER_ALL;
    /** 管理部门过滤（空表示不过滤；"未分配部门"匹配空值） */
    private String deptName = "";
    /** 部门条件是否同时匹配使用部门（存放地点层顶部统计口径为"管理部门或使用部门"） */
    private boolean deptMatchUse = false;
    /** 存放地点过滤（空表示不过滤；"未设置存放地点"匹配空值） */
    private String locationName = "";
    /** 部门编号BMBH（进入时预计算，查询与名称一并匹配） */
    private String deptCodeValue = "";
    /** 单位编号DWBH（进入时预计算，查询与名称一并匹配） */
    private String unitCodeValue = "";

    /** 已加载数据（分页累加，与适配器共享同一列表实例） */
    private final List<Assets> dataList = new ArrayList<>();
    /** 满足当前条件的真实总数（count查询） */
    private long totalCount = 0;
    /** 是否正在加载（防止滚动重复触发分页） */
    private boolean loading = false;
    /** 加载代数：重新加载时自增，过期回调直接丢弃，避免分页与刷新相互覆盖 */
    private int loadToken = 0;
    /** 列表尾部加载状态提示 */
    private TextView footerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_stats);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 首次进入与从资产详情返回：回到第一页重新加载，保证数据最新（不保留滚动位置）
        reload();
    }

    @Override
    public void initializeView() {
        // 读取跳转带入的固定条件（状态+范围）
        filter = getIntent().getIntExtra(EXTRA_FILTER, FILTER_ALL);
        deptName = getIntent().getStringExtra(EXTRA_DEPT_NAME);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION);
        deptMatchUse = getIntent().getBooleanExtra(EXTRA_DEPT_MATCH_USE, false);

        if (StringUtils.isNullOrEmpty(deptName)) {
            deptName = "";
        }
        if (StringUtils.isNullOrEmpty(locationName)) {
            locationName = "";
        }

        // 预计算部门编号（GROUNDMANAGEDEPT字段中编号与名称并存，查询需两者兼容，避免每次分页都查字典）
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

        topBar.setTitle("盘点统计");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        // 顶部条件行：只读文字展示本次查询条件（范围+状态），不给用户修改入口
        tvInventoryFilter.setText(buildConditionText());

        // 列表尾部加载状态提示（ListView的footer不参与数据项，需在setAdapter之前添加）
        footerView = new TextView(mContext);
        footerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                QMUIDisplayHelper.dp2px(mContext, 40)));
        footerView.setGravity(Gravity.CENTER);
        footerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        footerView.setTextColor(0xFF999999);
        listStats.addFooterView(footerView);

        statsAdapter = new InventoryStatsAdapter(mContext);
        statsAdapter.setData(dataList);
        // 部门字段存编号时按登录账套转名称展示（与盘点扫描卡片同一口径）
        statsAdapter.setAccountZtbh(application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH());
        // 补打标记唯一入口：卡片按钮点击把资产置为待补打
        statsAdapter.setOnMarkReprintClickListener(this::markReprint);
        listStats.setAdapter(statsAdapter);
        listStats.setOnItemClickListener((parent, view, position, id) -> {
            Assets assets = statsAdapter.getItem(position);
            if (assets != null) {
                Intent intentView = new Intent(mContext, AssetsViewActivity.class);
                intentView.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                startActivity(intentView);
            }
        });

        // 上拉分页：滚动到底部自动加载下一页（加载中/已全部加载时由tryLoadNextPage内部拦截）
        listStats.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    tryLoadNextPage();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount >= statsAdapter.getCount()) {
                    tryLoadNextPage();
                }
            }
        });
    }

    /**
     * 重新加载：回到第一页，查询真实总数与第一页数据
     * <p>
     * 数据库查询在后台线程执行（AsyncManager），主线程刷新列表；
     * loadToken自增使在途的翻页/刷新回调作废，避免数据相互覆盖
     */
    private void reload() {
        final int token = ++loadToken;
        loading = true;
        // 首屏/刷新加载中用转圈提示，避免大数据量时页面长时间空置
        layoutLoading.setVisibility(View.VISIBLE);
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(() -> {
            PageData page = new PageData();
            page.totalCount = countTotal();
            page.assets = queryPage(0);
            return page;
        }, result -> mainHandler.post(() -> {
            // 过期回调（已被新一轮加载取代）直接丢弃，避免误隐藏新加载的转圈提示
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            // 本轮加载结束：隐藏转圈提示
            layoutLoading.setVisibility(View.GONE);
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                PageData page = result.getData();
                totalCount = page.totalCount;
                dataList.clear();
                dataList.addAll(page.assets);
                statsAdapter.notifyDataSetChanged();
                updateTotalText();
            }
            updateFooter();
        }));
    }

    /**
     * 上拉加载下一页：滚动到列表底部附近的回调入口，
     * 加载中或已全部加载时不重复触发
     */
    private void tryLoadNextPage() {
        if (loading || dataList.size() >= totalCount) {
            return;
        }
        final int token = loadToken;
        final int offset = dataList.size();
        loading = true;
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(() -> queryPage(offset), result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                dataList.addAll(result.getData());
                statsAdapter.notifyDataSetChanged();
            }
            updateFooter();
        }));
    }

    /**
     * 查询指定偏移的一页数据（后台线程调用），Id升序与统计页查询口径一致
     *
     * @param offset 起始偏移（已加载条数）
     * @return 本页资产列表（最多PAGE_SIZE条）
     */
    private List<Assets> queryPage(int offset) {
        QueryCondition condition = buildCondition();
        return Assets.find(Assets.class, condition.where, condition.args, null, "Id ASC", offset + ", " + PAGE_SIZE);
    }

    /**
     * 查询满足当前条件的真实总数（后台线程调用），底部统计条展示用
     */
    private long countTotal() {
        QueryCondition condition = buildCondition();
        return Assets.count(Assets.class, condition.where, condition.args, null, "Id ASC", "");
    }

    /**
     * 组合当前查询条件（状态+部门+地点），分页查询与总数查询共用
     * <p>
     * 1. 状态：已盘ZT=02 / 未盘ZT为空或!=02（与Assets.ZT字段注释口径一致）/ 全部不限
     * 2. 部门：名称/部门编号/单位编号三种历史取值都兼容，"未分配部门"匹配空值，TRIM去除首尾空格
     * 3. 地点："未设置存放地点"匹配空值，TRIM去除首尾空格
     *
     * @return where语句与参数（顺序一一对应）
     */
    private QueryCondition buildCondition() {
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        // 状态条件
        if (filter == FILTER_COUNTED) {
            whereParts.add("ZT = ?");
            args.add("02");
        } else if (filter == FILTER_UNCOUNTED) {
            whereParts.add("(ZT IS NULL OR ZT != ?)");
            args.add("02");
        }
        // 管理部门条件
        if (!StringUtils.isNullOrEmpty(deptName)) {
            if (RfidTagAdapter.DEPT_NONE_NAME.equals(deptName)) {
                if (deptMatchUse) {
                    // 未分配部门：管理部门或使用部门为空均视为命中
                    whereParts.add("(GROUNDMANAGEDEPT IS NULL OR TRIM(GROUNDMANAGEDEPT) = ''"
                            + " OR ASSETSDEPT IS NULL OR TRIM(ASSETSDEPT) = '')");
                } else {
                    // 未分配部门：匹配数据库空值
                    whereParts.add("(GROUNDMANAGEDEPT IS NULL OR TRIM(GROUNDMANAGEDEPT) = '')");
                }
            } else {
                List<String> deptValues = new ArrayList<>();
                deptValues.add(deptName);
                if (!StringUtils.isNullOrEmpty(deptCodeValue) && !deptCodeValue.equals(deptName)) {
                    deptValues.add(deptCodeValue);
                }
                if (!StringUtils.isNullOrEmpty(unitCodeValue) && !unitCodeValue.equals(deptName)) {
                    deptValues.add(unitCodeValue);
                }
                List<String> orParts = new ArrayList<>();
                String[] deptFields = deptMatchUse
                        ? new String[]{"GROUNDMANAGEDEPT", "ASSETSDEPT"}
                        : new String[]{"GROUNDMANAGEDEPT"};
                for (String field : deptFields) {
                    for (String value : deptValues) {
                        orParts.add("TRIM(" + field + ") = ?");
                        args.add(value);
                    }
                }
                whereParts.add("(" + TextUtils.join(" OR ", orParts) + ")");
            }
        }
        // 存放地点条件
        if (!StringUtils.isNullOrEmpty(locationName)) {
            if (RfidTagAdapter.LOCATION_NONE_NAME.equals(locationName)) {
                // 未设置存放地点：匹配数据库空值
                whereParts.add("(ASSETSLAYADD IS NULL OR TRIM(ASSETSLAYADD) = '')");
            } else {
                whereParts.add("TRIM(ASSETSLAYADD) = ?");
                args.add(locationName);
            }
        }
        // 无任何条件时用恒真条件占位（查全部数据）
        if (whereParts.isEmpty()) {
            whereParts.add("1=1");
        }
        QueryCondition condition = new QueryCondition();
        condition.where = TextUtils.join(" AND ", whereParts);
        condition.args = args.toArray(new String[0]);
        return condition;
    }

    /**
     * 顶部条件文字：范围（部门/地点）+ 状态，如"财务科 / 档案室 未盘"；
     * 无范围时只显示状态（如"未盘"、"全部"）
     */
    private String buildConditionText() {
        StringBuilder builder = new StringBuilder();
        if (!StringUtils.isNullOrEmpty(deptName)) {
            builder.append(deptName);
        }
        if (!StringUtils.isNullOrEmpty(locationName)) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(locationName);
        }
        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(filterName());
        return builder.toString();
    }

    /** 状态显示名 */
    private String filterName() {
        if (filter == FILTER_COUNTED) {
            return "已盘";
        }
        if (filter == FILTER_UNCOUNTED) {
            return "未盘";
        }
        return "全部";
    }

    /** 底部统计条：真实总数 */
    private void updateTotalText() {
        tvTotalCount.setText("共 " + totalCount + " 条数据");
    }

    /**
     * 标记补打（全项目补打标记的唯一入口）：
     * ISPRINTBARCODE置01（待补打）、ISUPLOAD置02（标记待上传，联网后随资产同步回服务器）；
     * 之后由补打条码页(ReprintBarcodeActivity)统一打印，打印完成后由打印流程置02（已打印）
     */
    private void markReprint(Assets assets) {
        if (assets == null) {
            return;
        }
        if (Const.NOT_PRINT.equals(assets.getISPRINTBARCODE())) {
            showShortText("该资产已标记补打");
            return;
        }
        assets.setISPRINTBARCODE(Const.NOT_PRINT);
        assets.setISUPLOAD(Const.NOT_UPLOAD);
        assets.save();
        statsAdapter.notifyDataSetChanged();
        showShortText("已标记补打，可前往补打条码页打印");
    }

    /** 刷新列表尾部加载状态提示 */
    private void updateFooter() {
        if (footerView == null) {
            return;
        }
        if (loading) {
            footerView.setText("正在加载…");
        } else if (dataList.isEmpty()) {
            footerView.setText("");
        } else if (dataList.size() >= totalCount) {
            footerView.setText("已全部加载");
        } else {
            footerView.setText("上拉加载更多");
        }
    }

    /** 查询条件载体：where片段与参数 */
    private static class QueryCondition {
        String where;
        String[] args;
    }

    /** 一次加载的数据载体：真实总数 + 当前页资产 */
    private static class PageData {
        long totalCount;
        List<Assets> assets;
    }
}

