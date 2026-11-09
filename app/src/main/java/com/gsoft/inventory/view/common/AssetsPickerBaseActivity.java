package com.gsoft.inventory.view.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.AssetsPickerAdapter;
import com.gsoft.inventory.data.SelectItem;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.view.common.query.AssetsQueryBuilder;
import com.gsoft.inventory.view.common.query.AssetsQueryCondition;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 公共选择资产页基类。
 * <p>
 * 负责与具体业务无关的全部逻辑：查询面板、分页加载、跨搜索勾选、全选、
 * 确认时按业务范围校验、结果回传。具体入口只需提供 {@link AssetsPickerOptions}，
 * 或在子类里覆写 {@link #appendFixedConditions(AssetsQueryBuilder)} 追加代码级条件。
 * <p>
 * 账套条件由基类强制追加，调用方只描述业务固定条件，避免各入口口径不一致。
 */
public abstract class AssetsPickerBaseActivity extends BaseCompatActivity {

    private static final String TAG = "AssetsPicker";

    /** 分页大小 */
    private static final int PAGE_SIZE = 50;
    /** 管理部门筛选请求码 */
    private static final int REQ_MANAGE_DEPT_FILTER = 2001;
    /** 确认时校验已选 Id 的分片大小，避免 SQLite IN 参数过多 */
    private static final int VALIDATE_CHUNK_SIZE = 400;

    /** 回传的已选资产 Id 数组 */
    public static final String RESULT_SELECTED_IDS = "result_selected_asset_ids";

    @BindView(R.id.lyDrawLayout)
    DrawerLayout lyDrawLayout;
    /** 右侧查询条件面板 */
    @BindView(R.id.lyQuery)
    View lyQuery;
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.querybar)
    QMUITopBar queryBar;
    /**
     * 全选条：只对已加载的数据生效，分页加载更多后继续勾选新加载项
     */
    @BindView(R.id.layoutSelectAll)
    View layoutSelectAll;
    @BindView(R.id.editASSETSNAME)
    EditText editASSETSNAME;
    @BindView(R.id.rowFilterAssetsName)
    View rowFilterAssetsName;
    @BindView(R.id.editBARCODEID)
    EditText editBARCODEID;
    @BindView(R.id.rowFilterBarcode)
    View rowFilterBarcode;
    @BindView(R.id.editASSETSDEPT)
    EditText editASSETSDEPT;
    @BindView(R.id.rowFilterAssetsDept)
    View rowFilterAssetsDept;
    @BindView(R.id.editGROUNDMANAGEDEPT)
    EditText editGROUNDMANAGEDEPT;
    @BindView(R.id.rowFilterManageDept)
    View rowFilterManageDept;
    @BindView(R.id.btnPickManageDept)
    TextView btnPickManageDept;
    @BindView(R.id.editASSETSUSER)
    EditText editASSETSUSER;
    @BindView(R.id.rowFilterAssetsUser)
    View rowFilterAssetsUser;
    @BindView(R.id.editASSETSLAYADD)
    EditText editASSETSLAYADD;
    @BindView(R.id.rowFilterLocation)
    View rowFilterLocation;
    @BindView(R.id.btnReset)
    QMUIRoundButton btnReset;
    @BindView(R.id.btnSearch)
    QMUIRoundButton btnSearch;
    @BindView(R.id.cbSelectAll)
    CheckBox cbSelectAll;
    @BindView(R.id.tvSelectedCount)
    TextView tvSelectedCount;
    @BindView(R.id.listSource)
    ListView listSource;
    @BindView(R.id.layoutEmpty)
    View layoutEmpty;
    @BindView(R.id.tvEmpty)
    TextView tvEmpty;
    @BindView(R.id.layoutLoading)
    View layoutLoading;

    private TextView tvSearch;
    private TextView tvConfirm;
    private AssetsPickerAdapter adapter;
    private final List<Assets> dataList = new ArrayList<>();
    private View footerView;

    private AssetsPickerOptions options;
    private String ztbH;
    /** 管理部门筛选条件：部门编号 BMBH */
    private String groundManageDeptCode = "";
    /** 管理部门筛选条件：部门名称，用于兼容历史存名称的数据 */
    private String groundManageDeptName = "";

    private String whereClause = " Id > 0 ";
    private String[] whereArgs = new String[0];
    /** 当前最多允许加载的条数（查询上限） */
    private long totalCount;
    /** 当前条件下的真实总数，用于区分“查询上限截断”和“全部加载完” */
    private long realTotalCount;
    private int loadToken;
    private boolean loading;
    private boolean confirming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        options = createOptions();
        if (options == null) {
            options = AssetsPickerOptions.builder().build();
        }
        setContentView(R.layout.activity_assets_picker);
        ButterKnife.bind(this);
        initializeView();
    }

    /**
     * 由具体入口提供配置：常规入口从 Intent 读取，特殊入口可在子类里直接构造
     */
    protected abstract AssetsPickerOptions createOptions();

    @Override
    public void initializeView() {
        ztbH = application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH();

        topBar.setTitle(options.getTitle());
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        tvSearch = topBar.addRightTextButton("搜索", R.id.topbar_right_about_button);
        tvSearch.setOnClickListener(v -> openSearchPanel());
        tvConfirm = topBar.addRightTextButton("确定", R.id.topbar_right_query_button);
        tvConfirm.setOnClickListener(v -> confirmSelection());

        // 查询条件面板：从右侧滑出，左侧返回键/取消仅收起面板，不退出页面
        queryBar.setTitle("查询条件");
        queryBar.addLeftBackImageButton().setOnClickListener(v -> closeSearchPanel());

        footerView = new TextView(mContext);
        footerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                QMUIDisplayHelper.dp2px(mContext, 40)));
        ((TextView) footerView).setGravity(Gravity.CENTER);
        ((TextView) footerView).setTextSize(13);
        ((TextView) footerView).setTextColor(0xFF999999);
        listSource.addFooterView(footerView);

        adapter = new AssetsPickerAdapter(mContext);
        adapter.setData(dataList);
        adapter.setMaxSelectCount(options.getEffectiveMaxSelectCount());
        adapter.setSingleSelect(options.isSingleSelect());
        adapter.setSelectionListener(new AssetsPickerAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectedCount();
            }

            @Override
            public void onSelectLimitReached(int maxCount) {
                showShortText("单次最多选择 " + maxCount + " 条");
            }
        });
        listSource.setAdapter(adapter);

        listSource.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    tryLoadNextPage();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount >= adapter.getCount()) {
                    tryLoadNextPage();
                }
            }
        });

        layoutSelectAll.setVisibility(options.isSelectAllEnabled() ? View.VISIBLE : View.GONE);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });

        btnSearch.setOnClickListener(v -> {
            closeSearchPanel();
            reload();
        });
        btnReset.setOnClickListener(v -> {
            clearSearchFields();
            closeSearchPanel();
            reload();
        });

        initGroundManageDeptPicker();
        applyFilterVisibility();
        tvEmpty.setText(options.getEmptyText());
        initSelectedFromOptions();
        buildQuery();
        reload();
    }

    /**
     * 管理部门筛选：跳公共选择页（部门字典），回传部门编号 BMBH
     */
    private void initGroundManageDeptPicker() {
        editGROUNDMANAGEDEPT.setFocusable(false);
        editGROUNDMANAGEDEPT.setFocusableInTouchMode(false);
        editGROUNDMANAGEDEPT.setClickable(true);
        View.OnClickListener picker = v -> CommonSelectActivity.pickManageDept(
                this, REQ_MANAGE_DEPT_FILTER, editGROUNDMANAGEDEPT.getText().toString().trim());
        editGROUNDMANAGEDEPT.setOnClickListener(picker);
        btnPickManageDept.setOnClickListener(picker);
    }

    /**
     * 按配置显示/隐藏查询字段；没有可见字段时不显示顶部“搜索”按钮
     */
    private void applyFilterVisibility() {
        int mask = options.getVisibleFilters();
        rowFilterAssetsName.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_NAME) ? View.VISIBLE : View.GONE);
        rowFilterBarcode.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_BARCODE) ? View.VISIBLE : View.GONE);
        rowFilterAssetsDept.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_DEPT) ? View.VISIBLE : View.GONE);
        rowFilterManageDept.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_MANAGE_DEPT) ? View.VISIBLE : View.GONE);
        rowFilterAssetsUser.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_USER) ? View.VISIBLE : View.GONE);
        rowFilterLocation.setVisibility(isFilterVisible(AssetsPickerOptions.FILTER_LOCATION) ? View.VISIBLE : View.GONE);
        tvSearch.setVisibility(mask == 0 ? View.GONE : View.VISIBLE);
    }

    private boolean isFilterVisible(int filter) {
        return (options.getVisibleFilters() & filter) != 0;
    }

    @Override
    public void onBackPressed() {
        // 面板打开时返回键先收起面板，避免误退出整个选择页
        if (lyDrawLayout.isDrawerOpen(lyQuery)) {
            closeSearchPanel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQ_MANAGE_DEPT_FILTER) {
            return;
        }
        SelectItem item = CommonSelectActivity.readResult(data);
        if (item == null) {
            return;
        }
        editGROUNDMANAGEDEPT.setText(item.getName());
        groundManageDeptCode = item.getCode();
        groundManageDeptName = item.getName();
        // 只回填条件，不立即查询：面板保持打开，用户可继续设置其它条件后点"搜索"
    }

    private void initSelectedFromOptions() {
        long[] ids = options.getInitialSelectedIds();
        List<Long> idList = new ArrayList<>();
        for (long id : ids) {
            if (id > 0) {
                idList.add(id);
            }
        }
        adapter.setSelectedIds(idList);
        updateSelectedCount();
    }

    /** 从右侧滑出查询条件面板 */
    private void openSearchPanel() {
        lyDrawLayout.openDrawer(lyQuery);
    }

    /** 收起查询条件面板 */
    private void closeSearchPanel() {
        if (lyDrawLayout.isDrawerOpen(lyQuery)) {
            lyDrawLayout.closeDrawer(lyQuery);
        }
    }

    /**
     * 清空查询条件。管理部门除输入框外还要清掉编码，
     * 否则空名称配上旧编码仍会参与 groundManageDeptCode 的匹配。
     */
    private void clearSearchFields() {
        editASSETSNAME.setText("");
        editBARCODEID.setText("");
        editASSETSDEPT.setText("");
        editGROUNDMANAGEDEPT.setText("");
        groundManageDeptCode = "";
        groundManageDeptName = "";
        editASSETSUSER.setText("");
        editASSETSLAYADD.setText("");
    }

    // ==================== 查询 ====================

    private void buildQuery() {
        AssetsQueryBuilder builder = newBaseQuery();
        appendPanelFilters(builder);
        whereClause = builder.getWhere();
        whereArgs = builder.getArgs();
    }

    /**
     * 业务范围条件：账套 + 各入口固定条件。
     * 子类如需追加代码级条件，可覆写 {@link #appendFixedConditions(AssetsQueryBuilder)}。
     */
    protected AssetsQueryBuilder newBaseQuery() {
        AssetsQueryBuilder builder = new AssetsQueryBuilder();
        if (StringUtils.isNullOrEmpty(ztbH)) {
            // 账套异常时不允许退化成“查询全部账套”
            builder.andFalse();
        } else {
            builder.andEquals("ACCTSUITEID", ztbH);
        }
        appendFixedConditions(builder);
        return builder;
    }

    /**
     * 追加业务固定条件，默认读取 {@link AssetsPickerOptions#getFixedConditions()}
     */
    protected void appendFixedConditions(AssetsQueryBuilder builder) {
        List<AssetsQueryCondition> conditions = options.getFixedConditions();
        if (conditions == null) {
            return;
        }
        for (AssetsQueryCondition condition : conditions) {
            builder.andCondition(condition);
        }
    }

    /**
     * 追加查询面板条件；隐藏的字段即使有残留值也不参与查询
     */
    private void appendPanelFilters(AssetsQueryBuilder builder) {
        if (isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_NAME) && !isEmpty(editASSETSNAME)) {
            builder.andLike("ASSETSNAME", editASSETSNAME.getText().toString());
        }
        if (isFilterVisible(AssetsPickerOptions.FILTER_BARCODE) && !isEmpty(editBARCODEID)) {
            builder.andLike("BARCODEID", editBARCODEID.getText().toString());
        }
        if (isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_DEPT) && !isEmpty(editASSETSDEPT)) {
            builder.andLike("ASSETSDEPT", editASSETSDEPT.getText().toString());
        }
        if (isFilterVisible(AssetsPickerOptions.FILTER_MANAGE_DEPT)
                && !isEmpty(editGROUNDMANAGEDEPT)
                && !StringUtils.isNullOrEmpty(groundManageDeptCode)) {
            // 管理部门历史数据存在三种取值：部门编号 BMBH、单位编号 DWBH、中文名称，三者一并匹配
            String danweiCode = AccountHelper.getDanweiCode(groundManageDeptName);
            builder.andAnyOf("GROUNDMANAGEDEPT",
                    Arrays.asList(groundManageDeptCode, groundManageDeptName, danweiCode));
        }
        if (isFilterVisible(AssetsPickerOptions.FILTER_ASSETS_USER) && !isEmpty(editASSETSUSER)) {
            builder.andLike("ASSETSUSER", editASSETSUSER.getText().toString());
        }
        if (isFilterVisible(AssetsPickerOptions.FILTER_LOCATION) && !isEmpty(editASSETSLAYADD)) {
            builder.andLike("ASSETSLAYADD", editASSETSLAYADD.getText().toString());
        }
    }

    private static boolean isEmpty(EditText editText) {
        return StringUtils.isNullOrEmpty(editText.getText().toString().trim());
    }

    private void reload() {
        buildQuery();
        final int token = ++loadToken;
        loading = true;
        layoutLoading.setVisibility(View.VISIBLE);
        updateFooter();
        final String where = whereClause;
        final String[] args = whereArgs;
        AsyncManager.getInstance().submitWithCallback(() -> {
            PageData page = new PageData();
            page.total = Assets.count(Assets.class, where, args, null, "Id DESC", "");
            page.list = Assets.find(Assets.class, where, args, null, "Id DESC", "0, " + PAGE_SIZE);
            return page;
        }, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            layoutLoading.setVisibility(View.GONE);
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                PageData page = result.getData();
                realTotalCount = page.total;
                totalCount = Math.min(page.total, (long) options.getQueryLimit());
                dataList.clear();
                appendPage(page.list, totalCount);
                adapter.notifyDataSetChanged();
                updateEmpty();
                updateSelectAllState();
            } else {
                Log.e(TAG, "查询资产失败：where=" + where, result.getError());
                showShortText("查询失败，请重试");
            }
            updateFooter();
        }));
    }

    private void tryLoadNextPage() {
        if (loading || confirming || dataList.size() >= totalCount) {
            return;
        }
        final int token = loadToken;
        final int offset = dataList.size();
        final int pageSize = (int) Math.min(PAGE_SIZE, totalCount - offset);
        if (pageSize <= 0) {
            return;
        }
        loading = true;
        updateFooter();
        final String where = whereClause;
        final String[] args = whereArgs;
        AsyncManager.getInstance().submitWithCallback(
                () -> Assets.find(Assets.class, where, args, null, "Id DESC", offset + ", " + pageSize),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || token != loadToken) {
                        return;
                    }
                    loading = false;
                    if (result.isSuccess() && result.getData() != null) {
                        appendPage(result.getData(), totalCount);
                        adapter.notifyDataSetChanged();
                        updateSelectAllState();
                    } else if (!result.isSuccess()) {
                        Log.e(TAG, "加载下一页失败：offset=" + offset, result.getError());
                    }
                    updateFooter();
                }));
    }

    /**
     * 追加一页数据，并保证不超过查询上限
     */
    private void appendPage(List<Assets> page, long maxCount) {
        if (page == null) {
            return;
        }
        for (Assets assets : page) {
            if (dataList.size() >= maxCount) {
                break;
            }
            dataList.add(assets);
        }
    }

    private void updateFooter() {
        if (footerView == null) {
            return;
        }
        TextView footer = (TextView) footerView;
        if (loading) {
            footer.setText("正在加载…");
        } else if (dataList.isEmpty()) {
            footer.setText("");
        } else if (dataList.size() >= totalCount) {
            if (realTotalCount > totalCount) {
                footer.setText("已显示前 " + totalCount + " 条，请缩小查询范围");
            } else {
                footer.setText("已加载全部 " + totalCount + " 条");
            }
        } else {
            footer.setText("上拉加载更多");
        }
    }

    private void updateEmpty() {
        boolean empty = dataList.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listSource.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateSelectedCount() {
        int count = adapter.getSelectedCount();
        tvSelectedCount.setText(count > 0 ? "已选 " + count + " 条" : "");
        tvConfirm.setText(count > 0 ? "确定(" + count + ")" : "确定");
        updateSelectAllState();
    }

    private void updateSelectAllState() {
        boolean allSelected = adapter.isAllSelected();
        if (cbSelectAll.isChecked() == allSelected) {
            return;
        }
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(allSelected);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
    }

    // ==================== 确认 ====================

    private void confirmSelection() {
        if (confirming) {
            return;
        }
        final List<Long> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty() || !options.isValidateSelectionOnConfirm()) {
            deliverSelection(selectedIds);
            return;
        }
        confirming = true;
        showProgressDialog("请稍候", "正在确认所选资产…");
        AsyncManager.getInstance().submitWithCallback(() -> queryValidSelectedIds(selectedIds),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    confirming = false;
                    hideProgressDialog();
                    if (!result.isSuccess()) {
                        showShortText("确认失败，请重试");
                        return;
                    }
                    List<Long> validIds = result.getData();
                    if (validIds == null) {
                        validIds = new ArrayList<>();
                    }
                    if (validIds.size() < selectedIds.size()) {
                        showShortText("部分已选资产不在当前可选范围内，已自动移除");
                    }
                    deliverSelection(validIds);
                }));
    }

    /**
     * 按“账套 + 固定条件”校验已选 Id，不限定当前查询面板条件。
     * 这样跨搜索勾选的资产只在业务范围外才被剔除。
     */
    private List<Long> queryValidSelectedIds(List<Long> selectedIds) {
        List<Long> validIds = new ArrayList<>();
        if (selectedIds == null || selectedIds.isEmpty()) {
            return validIds;
        }
        for (int start = 0; start < selectedIds.size(); start += VALIDATE_CHUNK_SIZE) {
            int end = Math.min(start + VALIDATE_CHUNK_SIZE, selectedIds.size());
            List<Long> chunk = selectedIds.subList(start, end);
            AssetsQueryBuilder builder = newBaseQuery();
            List<String> idValues = new ArrayList<>(chunk.size());
            for (Long id : chunk) {
                idValues.add(String.valueOf(id));
            }
            builder.andIn("Id", idValues);
            List<Assets> found = Assets.find(Assets.class, builder.getWhere(), builder.getArgs());
            Set<Long> foundIds = new LinkedHashSet<>();
            if (found != null) {
                for (Assets assets : found) {
                    if (assets.getId() != null) {
                        foundIds.add(assets.getId());
                    }
                }
            }
            for (Long id : chunk) {
                if (foundIds.contains(id)) {
                    validIds.add(id);
                }
            }
        }
        return validIds;
    }

    /**
     * 回传结果；特殊入口可覆写以追加自己的数据
     */
    protected void deliverSelection(List<Long> selectedIds) {
        int size = selectedIds == null ? 0 : selectedIds.size();
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = selectedIds.get(i);
        }
        Intent data = new Intent();
        data.putExtra(RESULT_SELECTED_IDS, result);
        setResult(RESULT_OK, data);
        finish();
    }

    /** 分页查询结果 */
    private static class PageData {
        long total;
        List<Assets> list;
    }
}
